#include "lvfntman.h"
#include "lvstreamutils.h"
#include "../src/lvfont/lvfreetypeface.h"

#include <ft2build.h>
#include FT_FREETYPE_H
#include FT_MULTIPLE_MASTERS_H

#include <cstdio>
#include <cstring>
#include <memory>

static int fail(const char *message)
{
    std::fprintf(stderr, "%s\n", message);
    return 1;
}

static const char *fontFamily(const LVFontRef &font)
{
    if (font.isNull())
        return nullptr;
    FT_Face face = static_cast<FT_Face>(font->GetHandle());
    return face ? face->family_name : nullptr;
}

static int testVariableAndScopedFonts()
{
    const lString8 fixtureRoot(
            COOLREADER_SOURCE_DIR
            "/thirdparty/harfbuzz-14.2.1/test/subset/data/fonts");
    const lString8 globalRoboto(
            COOLREADER_SOURCE_DIR
            "/thirdparty/harfbuzz-14.2.1/test/subset/data/expected/"
            "retain-num-glyphs/Roboto-Regular.retain-num-glyphs.all.ttf");
    const lString8 globalCjk(
            COOLREADER_SOURCE_DIR
            "/thirdparty/harfbuzz-14.2.1/test/subset/data/fonts/"
            "MPLUS1-Variable.ttf");

    if (!fontMan->RegisterFont(globalRoboto)
            || !fontMan->RegisterFont(globalCjk))
        return fail("vendored global font fixtures did not register");

    static const int externalDocumentId = 38;
    const lString8 duplicateFamily("RAII Duplicate");
    if (!fontMan->RegisterExternalFont(
                externalDocumentId,
                Utf8ToUnicode(globalRoboto),
                duplicateFamily,
                false,
                false))
        return fail("first external font candidate did not register");
    if (fontMan->RegisterExternalFont(
                externalDocumentId,
                Utf8ToUnicode(globalCjk),
                duplicateFamily,
                false,
                false))
        return fail("duplicate external font candidate was published");
    fontMan->UnregisterDocumentFonts(externalDocumentId);

    LVFontRef systemFont = fontMan->GetFont(
            24, 400, false, css_ff_sans_serif,
            lString8("Roboto"), 0, -1, false);
    if (systemFont.isNull()
            || !fontFamily(systemFont)
            || std::strcmp(fontFamily(systemFont), "Roboto") != 0)
        return fail("global Roboto selection was not deterministic");

    LVContainerRef container = LVOpenDirectory(fixtureRoot);
    if (container.isNull())
        return fail("embedded font fixture container did not open");
    static const int documentId = 37;
    if (!fontMan->RegisterDocumentFont(
                documentId,
                container,
                U"SourceSerifVariable-Roman.ttf",
                lString8("Roboto"),
                false,
                false))
        return fail("embedded variable font fixture did not register");

    LVFontRef embeddedFont = fontMan->GetFont(
            24, 400, false, css_ff_serif,
            lString8("Roboto"), 0, documentId, false);
    FT_Face embeddedFace = embeddedFont.isNull()
            ? nullptr
            : static_cast<FT_Face>(embeddedFont->GetHandle());
    if (!embeddedFace || !embeddedFace->family_name
            || std::strcmp(
                    embeddedFace->family_name,
                    "Source Serif Variable") != 0)
        return fail("document-scoped font did not override the global family");
    if (!FT_HAS_MULTIPLE_MASTERS(embeddedFace))
        return fail("variable font fixture lost its variation axes");

    LVFont::glyph_info_t glyph = {};
    if (!embeddedFont->getGlyphInfo(U'A', &glyph)
            || embeddedFont->getCharWidth(U'A') <= 0)
        return fail("variable font fixture did not expose usable glyphs");
    const lChar32 sample[] = {U'A', U'B', U'C'};
    lUInt16 widths[3] = {};
    lUInt8 flags[3] = {};
    if (embeddedFont->measureText(
                sample, 3, widths, flags, 1000, U'?') != 3
            || widths[2] <= widths[0])
        return fail("variable font fixture did not shape deterministic text");

    embeddedFont.Clear();
    fontMan->UnregisterDocumentFonts(documentId);
    LVFontRef restoredGlobal = fontMan->GetFont(
            24, 400, false, css_ff_sans_serif,
            lString8("Roboto"), 0, documentId, false);
    if (restoredGlobal.isNull()
            || !fontFamily(restoredGlobal)
            || std::strcmp(fontFamily(restoredGlobal), "Roboto") != 0)
        return fail("global font was not restored after document teardown");
    return 0;
}

static int testPersistentFaceReloadOwnership()
{
    const char *fontPath =
            COOLREADER_SOURCE_DIR
            "/thirdparty/harfbuzz-14.2.1/test/subset/data/expected/"
            "retain-num-glyphs/Roboto-Regular.retain-num-glyphs.all.ttf";
    FT_Library rawLibrary = nullptr;
    if (FT_Init_FreeType(&rawLibrary) != FT_Err_Ok)
        return fail("persistent face fixture FreeType did not initialize");
    using FreeTypeLibraryOwner =
            std::unique_ptr<FT_LibraryRec_, decltype(&FT_Done_FreeType)>;
    FreeTypeLibraryOwner library(rawLibrary, &FT_Done_FreeType);
    LVMutex mutex;
    LVFontGlobalGlyphCache glyphCache(0x20000);
    LVFreeTypeFace face(mutex, library.get(), &glyphCache);

    if (!face.loadFromFile(
                fontPath, 0, 24, css_ff_sans_serif, false, false)
            || face.IsNull()
            || face.GetHandle() == nullptr)
        return fail("persistent FreeType face candidate did not load");
    if (face.loadFromFile(
                "/definitely/missing/coolreader-font.ttf",
                0, 24, css_ff_sans_serif, false, false))
        return fail("missing persistent FreeType face unexpectedly loaded");
    if (!face.IsNull() || face.GetHandle() != nullptr)
        return fail("failed persistent FreeType reload retained stale state");
    if (!face.loadFromFile(
                fontPath, 0, 24, css_ff_sans_serif, false, false)
            || face.IsNull())
        return fail("persistent FreeType face did not recover after failure");
    face.Clear();
    if (!face.IsNull() || face.GetHandle() != nullptr)
        return fail("persistent FreeType face clear retained its handle");
    return 0;
}

static int testOrderedFallback()
{
    if (!fontMan->SetFallbackFontFaces(
                lString8("Missing Fixture; M PLUS 1; Roboto; M PLUS 1")))
        return fail("valid fallback font list was rejected");
    if (fontMan->GetFallbackFontCount() != 2
            || fontMan->GetFallbackFontFace(0) != "M PLUS 1"
            || fontMan->GetFallbackFontFace(1) != "Roboto"
            || fontMan->GetFallbackFontFaces()
                    != "M PLUS 1; Roboto") {
        std::fprintf(stderr,
                "fallbacks: count=%d first='%s' second='%s' all='%s'\n",
                fontMan->GetFallbackFontCount(),
                fontMan->GetFallbackFontFace(0).c_str(),
                fontMan->GetFallbackFontFace(1).c_str(),
                fontMan->GetFallbackFontFaces().c_str());
        return fail("fallback font list was not filtered and ordered");
    }

    LVFontRef cjkFallback =
            fontMan->GetFallbackFont(24, 400, false, 0);
    LVFontRef latinFallback =
            fontMan->GetFallbackFont(24, 400, false, 1);
    if (cjkFallback.isNull() || latinFallback.isNull()
            || cjkFallback->getTypeFace() != "M PLUS 1"
            || latinFallback->getTypeFace() != "Roboto"
            || cjkFallback->getFallbackMask() != 1
            || latinFallback->getFallbackMask() != 2
            || !fontMan->GetFallbackFont(
                    24, 400, false, 2).isNull())
        return fail("fallback font instances did not preserve list order");

    LVFontRef primary = fontMan->GetFont(
            24, 400, false, css_ff_sans_serif,
            lString8("Roboto"), 0, -1, false);
    LVFont::glyph_info_t glyph = {};
    if (primary.isNull() || !primary->getGlyphInfo(0x4E2D, &glyph))
        return fail("primary font did not resolve a CJK fallback glyph");

    if (fontMan->SetFallbackFontFaces(lString8("Missing Fixture"))
            || fontMan->GetFallbackFontCount() != 0
            || !fontMan->GetFallbackFontFaces().empty())
        return fail("missing fallback font was retained");
    return 0;
}

int main()
{
    if (!InitFontManager(lString8::empty_str) || !fontMan)
        return fail("typography fixture font manager did not initialize");
    int result = testVariableAndScopedFonts();
    if (result == 0)
        result = testPersistentFaceReloadOwnership();
    if (result == 0)
        result = testOrderedFallback();
    if (!ShutdownFontManager() && result == 0)
        return fail("typography fixture font manager did not shut down");
    return result;
}
