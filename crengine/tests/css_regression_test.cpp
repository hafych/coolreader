#include "lvdocview.h"
#include "lvfntman.h"
#include "lvstreamutils.h"
#include "lvstsheet.h"
#include "lvtinydom.h"

#include <cstdio>
#include <memory>
#include <string>

static int fail(const char *message)
{
    std::fprintf(stderr, "%s\n", message);
    return 1;
}

static int testDeclarationUnitsAndPageBreaks()
{
    static const char declarationText[] =
            "{"
            " width: 12px;"
            " height: 2.5em;"
            " min-width: 25%;"
            " max-width: 3rem;"
            " margin-left: 4pt;"
            " page-break-before: always;"
            " break-after: page;"
            " break-inside: avoid;"
            "}";
    const char *declarationCursor = declarationText;
    LVCssDeclaration declaration;
    if (!declaration.parse(declarationCursor, 20200824))
        return fail("CSS declaration fixture did not parse");

    css_style_rec_t style;
    declaration.apply(&style);
    if (style.width.type != css_val_px
            || style.width.value != 12 * 256
            || style.height.type != css_val_em
            || style.height.value != 640
            || style.min_width.type != css_val_percent
            || style.min_width.value != 25 * 256
            || style.max_width.type != css_val_rem
            || style.max_width.value != 3 * 256
            || style.margin[0].type != css_val_pt
            || style.margin[0].value != 4 * 256)
        return fail("CSS length units changed during declaration parsing");
    if (style.page_break_before != css_pb_always
            || style.page_break_after != css_pb_page
            || style.page_break_inside != css_pb_avoid)
        return fail("CSS page-break aliases changed during parsing");
    return 0;
}

static int testStylesheetOwnershipAndRollback()
{
    LVCssDeclaration declaration;
    const char *valid =
            "{ color: #112233; font-size: 18px; }";
    if (!declaration.parse(valid, 20200824))
        return fail("CSS ownership declaration did not parse");
    const lUInt32 validHash = declaration.getHash();
    css_style_rec_t validStyle;
    declaration.apply(&validStyle);
    if (validHash == 0
            || validStyle.color.type != css_val_color
            || validStyle.color.value != 0x112233)
        return fail("CSS ownership declaration changed values");

    const char *truncated =
            "{ color: #445566; font-size: 24px;";
    if (declaration.parse(truncated, 20200824)
            || declaration.getHash() != validHash)
        return fail("truncated CSS declaration replaced committed data");
    css_style_rec_t rollbackStyle;
    declaration.apply(&rollbackStyle);
    if (rollbackStyle.color.type != css_val_color
            || rollbackStyle.color.value != 0x112233)
        return fail("truncated CSS declaration changed applied data");

    const char *empty = "{ }";
    if (!declaration.parse(empty, 20200824)
            || !declaration.empty()
            || declaration.getHash() != 0)
        return fail("empty CSS declaration did not release old data");

    ldomDocument document;
    LVStyleSheet stylesheet(&document);
    if (!stylesheet.parse(
                ".base, p > span:first-child "
                "{ color: #123456; }"))
        return fail("CSS ownership stylesheet did not parse");
    const lUInt32 baseHash = stylesheet.getHash();
    LVStyleSheet copied(stylesheet);
    if (baseHash == 0 || copied.getHash() != baseHash)
        return fail("CSS selector deep copy changed its hash");
    stylesheet.push();
    if (!stylesheet.parse(
                ".extra { font-weight: bold; }")
            || stylesheet.getHash() == baseHash)
        return fail("CSS snapshot mutation did not publish");
    if (!stylesheet.pop()
            || stylesheet.getHash() != baseHash
            || stylesheet.pop())
        return fail("CSS snapshot restore changed selector ownership");
    stylesheet.clear();
    if (stylesheet.getHash() != 0
            || copied.getHash() != baseHash)
        return fail("CSS copy shared selector-chain ownership");

    std::string longCss;
    const int selectorCount = 4096;
    longCss.reserve(
            static_cast<size_t>(selectorCount) * 3 + 32);
    for (int i = 0; i < selectorCount; ++i) {
        if (i)
            longCss += ',';
        longCss += ".x";
    }
    longCss += "{ display: block; }";
    LVStyleSheet longSheet(&document);
    if (!longSheet.parse(longCss.c_str()))
        return fail("long CSS selector chain did not parse");
    const lUInt32 longHash = longSheet.getHash();
    LVStyleSheet longCopy(longSheet);
    longSheet.push();
    if (!longSheet.pop()
            || longHash == 0
            || longSheet.getHash() != longHash
            || longCopy.getHash() != longHash)
        return fail("long CSS selector chain did not copy or restore");
    return 0;
}

static std::unique_ptr<LVDocView> loadStyledFixture()
{
    static const char fixture[] =
            "<!DOCTYPE html>"
            "<html><head><meta charset=\"utf-8\"><style>"
            "body { color: #102030; text-align: center;"
            " font-size: 20px; }"
            "p { color: #202020; }"
            ".cascade { color: #303030; }"
            "p.cascade { color: #405060; }"
            "#cascade { color: #506070; }"
            "p#cascade { color: #607080; font-size: 150%;"
            " page-break-before: always; }"
            "#cascade { color: #708090; }"
            "#parent { color: #123456; text-align: right;"
            " font-size: 24px; }"
            "#child { font-size: 0.5em; }"
            "</style></head><body>"
            "<p id=\"cascade\" class=\"cascade\">Cascade</p>"
            "<div id=\"parent\"><span id=\"child\">Inherited</span></div>"
            "</body></html>";
    LVStreamRef stream = LVCreateMemoryStream(
            const_cast<char *>(fixture),
            static_cast<int>(sizeof(fixture) - 1),
            true,
            LVOM_READ);
    if (stream.isNull())
        return std::unique_ptr<LVDocView>();

    std::unique_ptr<LVDocView> view(new LVDocView(16, true));
    view->setShowCover(false);
    view->setPageHeaderInfo(0);
    view->setDefaultFontFace(lString8("Roboto"));
    view->setStatusFontFace(lString8("Roboto"));
    view->setFontSize(20);
    view->Resize(320, 240);
    if (!view->LoadDocument(stream, U"css-regression.html", false))
        return std::unique_ptr<LVDocView>();
    view->Render(320, 240);
    return view;
}

static int testCascadeSpecificityAndInheritance()
{
    std::unique_ptr<LVDocView> view = loadStyledFixture();
    if (!view)
        return fail("styled HTML fixture did not load");
    ldomDocument *document = view->getDocument();
    ldomNode *cascade = document->getElementById(U"cascade");
    ldomNode *parent = document->getElementById(U"parent");
    ldomNode *child = document->getElementById(U"child");
    if (!cascade || !parent || !child)
        return fail("styled HTML fixture IDs did not resolve");

    css_style_ref_t cascadeStyle = cascade->getStyle();
    css_style_ref_t parentStyle = parent->getStyle();
    css_style_ref_t childStyle = child->getStyle();
    if (cascadeStyle.isNull()
            || parentStyle.isNull()
            || childStyle.isNull())
        return fail("styled HTML fixture did not produce computed styles");

    if (cascadeStyle->color.type != css_val_color
            || cascadeStyle->color.value != 0x607080)
        return fail("CSS specificity or source-order cascade changed");
    if (cascadeStyle->font_size.type != css_val_px
            || cascadeStyle->font_size.value != 30 * 256)
        return fail("CSS percentage font-size did not resolve against parent");
    if (cascadeStyle->page_break_before != css_pb_always)
        return fail("CSS page-break-before was not retained in computed style");

    if (parentStyle->color.type != css_val_color
            || parentStyle->color.value != 0x123456
            || childStyle->color.type != parentStyle->color.type
            || childStyle->color.value != parentStyle->color.value
            || childStyle->text_align != css_ta_right)
        return fail("CSS inherited properties changed");
    if (childStyle->font_size.type != css_val_px
            || childStyle->font_size.value != 12 * 256)
        return fail("CSS em font-size did not resolve against parent");
    return 0;
}

int main()
{
    int result = testDeclarationUnitsAndPageBreaks();
    if (result != 0)
        return result;
    result = testStylesheetOwnershipAndRollback();
    if (result != 0)
        return result;

    if (!InitFontManager(lString8::empty_str) || !fontMan)
        return fail("CSS fixture font manager did not initialize");
    const lString8 fontPath(
            COOLREADER_SOURCE_DIR
            "/thirdparty/harfbuzz-14.2.1/test/subset/data/expected/"
            "retain-num-glyphs/Roboto-Regular.retain-num-glyphs.all.ttf");
    if (!fontMan->RegisterFont(fontPath))
        result = fail("CSS fixture font did not register");
    if (result == 0)
        result = testCascadeSpecificityAndInheritance();
    if (!ShutdownFontManager() && result == 0)
        result = fail("CSS fixture font manager did not shut down");
    return result;
}
