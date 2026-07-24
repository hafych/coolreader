#include "cssdef.h"
#include "lvdocview.h"
#include "lvfntman.h"
#include "lvstreamutils.h"
#include "lvxmlutils.h"

#include <cstdint>
#include <cstdio>
#include <memory>
#include <string>
#include <vector>

static int fail(const char *message)
{
    std::fprintf(stderr, "%s\n", message);
    return 1;
}

static void append16(std::vector<lUInt8> &output, lUInt16 value)
{
    output.push_back(static_cast<lUInt8>(value));
    output.push_back(static_cast<lUInt8>(value >> 8));
}

static void append32(std::vector<lUInt8> &output, lUInt32 value)
{
    append16(output, static_cast<lUInt16>(value));
    append16(output, static_cast<lUInt16>(value >> 16));
}

static lUInt32 crc32(const std::string &data)
{
    lUInt32 crc = 0xFFFFFFFFU;
    for (unsigned char byte : data) {
        crc ^= byte;
        for (int bit = 0; bit < 8; bit++)
            crc = (crc >> 1) ^ (0xEDB88320U & (0U - (crc & 1U)));
    }
    return ~crc;
}

struct ZipEntry {
    std::string name;
    std::string data;
    lUInt32 checksum = 0;
    lUInt32 localOffset = 0;
};

static void appendBytes(
        std::vector<lUInt8> &output, const std::string &bytes)
{
    output.insert(output.end(), bytes.begin(), bytes.end());
}

static std::vector<lUInt8> makeStoredZip(
        std::vector<ZipEntry> entries)
{
    std::vector<lUInt8> archive;
    for (ZipEntry &entry : entries) {
        entry.checksum = crc32(entry.data);
        entry.localOffset = static_cast<lUInt32>(archive.size());
        append32(archive, 0x04034B50U);
        append16(archive, 20);
        append16(archive, 0);
        append16(archive, 0);
        append16(archive, 0);
        append16(archive, 0);
        append32(archive, entry.checksum);
        append32(archive, static_cast<lUInt32>(entry.data.size()));
        append32(archive, static_cast<lUInt32>(entry.data.size()));
        append16(archive, static_cast<lUInt16>(entry.name.size()));
        append16(archive, 0);
        appendBytes(archive, entry.name);
        appendBytes(archive, entry.data);
    }

    const lUInt32 centralOffset =
            static_cast<lUInt32>(archive.size());
    for (const ZipEntry &entry : entries) {
        append32(archive, 0x02014B50U);
        append16(archive, 20);
        append16(archive, 20);
        append16(archive, 0);
        append16(archive, 0);
        append16(archive, 0);
        append16(archive, 0);
        append32(archive, entry.checksum);
        append32(archive, static_cast<lUInt32>(entry.data.size()));
        append32(archive, static_cast<lUInt32>(entry.data.size()));
        append16(archive, static_cast<lUInt16>(entry.name.size()));
        append16(archive, 0);
        append16(archive, 0);
        append16(archive, 0);
        append16(archive, 0);
        append32(archive, 0);
        append32(archive, entry.localOffset);
        appendBytes(archive, entry.name);
    }
    const lUInt32 centralSize =
            static_cast<lUInt32>(archive.size()) - centralOffset;
    append32(archive, 0x06054B50U);
    append16(archive, 0);
    append16(archive, 0);
    append16(archive, static_cast<lUInt16>(entries.size()));
    append16(archive, static_cast<lUInt16>(entries.size()));
    append32(archive, centralSize);
    append32(archive, centralOffset);
    append16(archive, 0);
    return archive;
}

static std::string makeNav(bool includeToc)
{
    std::string nav =
            "<?xml version=\"1.0\" encoding=\"UTF-8\"?>"
            "<html xmlns=\"http://www.w3.org/1999/xhtml\""
            " xmlns:epub=\"http://www.idpf.org/2007/ops\"><body>";
    if (includeToc) {
        nav +=
            "<nav epub:type=\"toc\"><h1>Contents</h1><ol>"
            "<li><span>Part One</span><ol><li>"
            "<a href=\"chapter.xhtml#start\">Chapter One</a>"
            "</li></ol></li></ol></nav>";
    }
    nav +=
            "<nav epub:type=\"landmarks\"><ol><li>"
            "<a epub:type=\"bodymatter\" href=\"chapter.xhtml#start\">"
            "Start Reading</a></li></ol></nav>"
            "<nav epub:type=\"page-list\"><ol><li>"
            "<a href=\"chapter.xhtml#page-one\">1</a>"
            "</li></ol></nav></body></html>";
    return nav;
}

static std::vector<lUInt8> makeEpub(bool includeToc)
{
    const std::string container =
            "<?xml version=\"1.0\"?>"
            "<container version=\"1.0\""
            " xmlns=\"urn:oasis:names:tc:opendocument:xmlns:container\">"
            "<rootfiles><rootfile full-path=\"OEBPS/package.opf\""
            " media-type=\"application/oebps-package+xml\"/>"
            "</rootfiles></container>";
    const std::string package =
            "<?xml version=\"1.0\" encoding=\"UTF-8\"?>"
            "<package xmlns=\"http://www.idpf.org/2007/opf\" version=\"3.0\""
            " unique-identifier=\"book-id\""
            " prefix=\"media: http://www.idpf.org/epub/vocab/overlays/#\">"
            "<metadata xmlns:dc=\"http://purl.org/dc/elements/1.1/\">"
            "<dc:identifier id=\"book-id\">urn:uuid:epub3-regression</dc:identifier>"
            "<dc:title>EPUB3 Regression</dc:title>"
            "<dc:creator>Author One</dc:creator>"
            "<dc:creator>Author Two</dc:creator>"
            "<dc:language>en</dc:language>"
            "<dc:description>Static text policy</dc:description>"
            "<dc:subject>Navigation</dc:subject>"
            "<dc:subject>Accessibility</dc:subject>"
            "<meta property=\"belongs-to-collection\" id=\"series\">"
            "Reader Corpus</meta>"
            "<meta property=\"collection-type\" refines=\"#series\">series</meta>"
            "<meta property=\"group-position\" refines=\"#series\">2</meta>"
            "<meta property=\"media:duration\" refines=\"#overlay\">0:00:04</meta>"
            "<meta property=\"dcterms:modified\">2026-07-24T00:00:00Z</meta>"
            "</metadata><manifest>"
            "<item id=\"nav\" href=\"nav.xhtml\""
            " media-type=\"application/xhtml+xml\" properties=\"nav\"/>"
            "<item id=\"chapter\" href=\"chapter.xhtml\""
            " media-type=\"application/xhtml+xml\" media-overlay=\"overlay\"/>"
            "<item id=\"overlay\" href=\"overlay.smil\""
            " media-type=\"application/smil+xml\"/>"
            "</manifest><spine><itemref idref=\"chapter\"/></spine></package>";
    const std::string chapter =
            "<?xml version=\"1.0\" encoding=\"UTF-8\"?>"
            "<html xmlns=\"http://www.w3.org/1999/xhtml\""
            " xmlns:epub=\"http://www.idpf.org/2007/ops\"><body>"
            "<section id=\"start\"><h1>Chapter One</h1>"
            "<span id=\"page-one\" epub:type=\"pagebreak\" title=\"1\"/>"
            "<p>Static chapter text"
            "<a id=\"note-ref\" epub:type=\"noteref\" href=\"#note-body\">1</a>"
            "</p><aside id=\"note-body\" epub:type=\"footnote\">"
            "<p>Footnote text</p></aside>"
            "<aside id=\"endnote-body\" epub:type=\"endnote\">"
            "<p>Endnote text</p></aside></section></body></html>";
    const std::string overlay =
            "<?xml version=\"1.0\" encoding=\"UTF-8\"?>"
            "<smil xmlns=\"http://www.w3.org/ns/SMIL\" version=\"3.0\">"
            "<body><seq id=\"overlay-marker\"><par>"
            "<text src=\"chapter.xhtml#start\"/>"
            "<audio src=\"audio.mp3\" clipEnd=\"0:00:04\"/>"
            "</par></seq></body></smil>";
    return makeStoredZip({
        {"mimetype", "application/epub+zip"},
        {"META-INF/container.xml", container},
        {"OEBPS/package.opf", package},
        {"OEBPS/nav.xhtml", makeNav(includeToc)},
        {"OEBPS/chapter.xhtml", chapter},
        {"OEBPS/overlay.smil", overlay},
    });
}

static std::unique_ptr<LVDocView> loadEpub(
        bool includeToc, const lChar32 *fileName)
{
    std::vector<lUInt8> archive = makeEpub(includeToc);
    LVStreamRef stream = LVCreateMemoryStream(
            archive.data(),
            static_cast<int>(archive.size()),
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
    view->setStyleSheet(UnicodeToUtf8(LVReadTextFile(lString32(
            COOLREADER_SOURCE_DIR "/cr3gui/data/epub.css"))));
    view->Resize(320, 240);
    if (!view->LoadDocument(stream, fileName, false))
        return std::unique_ptr<LVDocView>();
    view->Render(320, 240);
    return view;
}

static int testFullNavigationAndMetadata()
{
    std::unique_ptr<LVDocView> view =
            loadEpub(true, U"epub3-full-regression.epub");
    if (!view)
        return fail("EPUB3 full navigation fixture did not load");
    if (view->getTitle() != U"EPUB3 Regression"
            || view->getAuthors() != U"Author One\nAuthor Two"
            || view->getLanguage() != U"en"
            || view->getDescription() != U"Static text policy"
            || view->getKeywords() != U"Navigation\nAccessibility"
            || view->getSeriesName() != U"Reader Corpus"
            || view->getSeriesNumber() != 2)
        return fail("EPUB3 metadata contract changed");

    ldomDocument *document = view->getDocument();
    LVTocItem *toc = document->getToc();
    if (toc->getChildCount() != 1
            || toc->getChild(0)->getName() != U"Part One"
            || toc->getChild(0)->getChildCount() != 1
            || toc->getChild(0)->getChild(0)->getName() != U"Chapter One"
            || toc->getChild(0)->getChild(0)->getXPointer().isNull())
        return fail("EPUB3 nested nav TOC changed");
    LVPageMap *pageMap = document->getPageMap();
    if (pageMap->getChildCount() != 1
            || pageMap->getChild(0)->getLabel() != U"1"
            || pageMap->getChild(0)->getXPointer().isNull())
        return fail("EPUB3 page-list navigation changed");

    ldomNode *noteRef =
            document->getElementById(U"_doc_fragment_0_ note-ref");
    ldomNode *footnote =
            document->getElementById(U"_doc_fragment_0_ note-body");
    ldomNode *endnote =
            document->getElementById(U"_doc_fragment_0_ endnote-body");
    if (!noteRef || !footnote || !endnote)
        return fail("EPUB3 semantic note nodes did not survive import");
    const lUInt32 noteRefHints = noteRef->getStyle()->cr_hint.value;
    const lUInt32 footnoteHints = footnote->getStyle()->cr_hint.value;
    const lUInt32 endnoteHints = endnote->getStyle()->cr_hint.value;
    if ((noteRefHints & CSS_CR_HINT_NOTEREF) == 0
            || (footnoteHints & CSS_CR_HINT_FOOTNOTE) == 0
            || (footnoteHints & CSS_CR_HINT_FOOTNOTE_INPAGE) == 0
            || (endnoteHints & CSS_CR_HINT_FOOTNOTE) == 0
            || (endnoteHints & CSS_CR_HINT_FOOTNOTE_INPAGE) != 0)
        return fail("EPUB3 footnote policy hints changed");

    if (document->getElementById(U"overlay-marker") != NULL
            || document->getElementById(
                    U"_doc_fragment_0_ start") == NULL)
        return fail("EPUB3 media-overlay static-text policy changed");
    return 0;
}

static int testLandmarksFallback()
{
    std::unique_ptr<LVDocView> view =
            loadEpub(false, U"epub3-landmarks-regression.epub");
    if (!view)
        return fail("EPUB3 landmarks fixture did not load");
    LVTocItem *toc = view->getDocument()->getToc();
    if (toc->getChildCount() != 1
            || toc->getChild(0)->getName() != U"Start Reading"
            || toc->getChild(0)->getXPointer().isNull())
        return fail("EPUB3 landmarks fallback changed");
    return 0;
}

int main()
{
    if (!InitFontManager(lString8::empty_str) || !fontMan)
        return fail("EPUB3 fixture font manager did not initialize");
    const lString8 fontPath(
            COOLREADER_SOURCE_DIR
            "/thirdparty/harfbuzz-14.2.1/test/subset/data/expected/"
            "retain-num-glyphs/Roboto-Regular.retain-num-glyphs.all.ttf");
    int result = 0;
    if (!fontMan->RegisterFont(fontPath))
        result = fail("EPUB3 fixture font did not register");
    if (result == 0)
        result = testFullNavigationAndMetadata();
    if (result == 0)
        result = testLandmarksFallback();
    if (!ShutdownFontManager() && result == 0)
        result = fail("EPUB3 fixture font manager did not shut down");
    return result;
}
