#include "fb2def.h"
#include "lvdocview.h"
#include "lvfntman.h"
#include "lvtinydom.h"
#include "lvstreamutils.h"

#include <algorithm>
#include <atomic>
#include <chrono>
#include <cstdio>
#include <filesystem>
#include <memory>
#include <string>
#include <thread>
#include <type_traits>
#include <vector>

#define XS_IMPLEMENT_SCHEME 1
#include "fb2def.h"

static int fail(const char *message) {
    std::fprintf(stderr, "%s\n", message);
    return 1;
}

static std::unique_ptr<ldomDocument> parseFixture() {
    static const char fixture[] =
            "<?xml version=\"1.0\" encoding=\"UTF-8\"?>"
            "<FictionBook>"
            "<body><section><title><p>Regression chapter</p></title>"
            "<p>Alpha Needle middle needle omega.</p>"
            "</section></body>"
            "</FictionBook>";
    LVStreamRef stream = LVCreateMemoryStream(
            const_cast<char *>(fixture),
            static_cast<int>(sizeof(fixture) - 1),
            true,
            LVOM_READ);
    return std::unique_ptr<ldomDocument>(LVParseXMLStream(
            stream, fb2_elem_table, fb2_attr_table, fb2_ns_table));
}

struct DocumentSnapshot {
    lString32 firstPosition;
    lString32 secondPosition;
    lString32 selectionText;
};

static int snapshotDocument(
        ldomDocument *document, DocumentSnapshot &snapshot) {
    if (document == NULL)
        return fail("document fixture did not parse");

    ldomXPointer textPointer = document->createXPointer(
            U"/FictionBook/body[1]/section[1]/p[1]/text()[1]");
    if (textPointer.isNull() || !textPointer.isText())
        return fail("fixture text XPointer did not resolve");

    lString32 text = textPointer.getNode()->getText();
    ldomXRange textRange(
            ldomXPointer(textPointer.getNode(), 0),
            ldomXPointer(textPointer.getNode(), text.length()));

    LVArray<ldomWord> forward;
    if (!textRange.findText(
                U"needle", true, false, forward, 10, 0)
            || forward.length() != 2) {
        return fail("case-insensitive forward search changed");
    }
    if (forward[0].getText() != U"Needle"
            || forward[1].getText() != U"needle") {
        return fail("forward search result order changed");
    }

    ldomXRange reverseRange(
            ldomXPointer(textPointer.getNode(), 0),
            ldomXPointer(textPointer.getNode(), text.length()));
    LVArray<ldomWord> reverse;
    if (!reverseRange.findText(
                U"needle", true, true, reverse, 10, 0)
            || reverse.length() != 2
            || reverse[0].getText() != U"needle"
            || reverse[1].getText() != U"Needle") {
        return fail("reverse search result order changed");
    }

    ldomXRange caseSensitiveRange(
            ldomXPointer(textPointer.getNode(), 0),
            ldomXPointer(textPointer.getNode(), text.length()));
    LVArray<ldomWord> caseSensitive;
    if (!caseSensitiveRange.findText(
                U"needle", false, false, caseSensitive, 10, 0)
            || caseSensitive.length() != 1
            || caseSensitive[0].getText() != U"needle") {
        return fail("case-sensitive search changed");
    }

    ldomXRange missingRange(
            ldomXPointer(textPointer.getNode(), 0),
            ldomXPointer(textPointer.getNode(), text.length()));
    LVArray<ldomWord> missing;
    if (missingRange.findText(
                U"absent", true, false, missing, 10, 0)
            || !missing.empty()) {
        return fail("missing search term produced a result");
    }

    snapshot.firstPosition =
            forward[0].getStartXPointer().toString();
    snapshot.secondPosition =
            forward[1].getStartXPointer().toString();
    ldomXPointer restored =
            document->createXPointer(snapshot.firstPosition);
    if (restored.isNull()
            || restored != forward[0].getStartXPointer()) {
        return fail("serialized reading position did not round-trip");
    }

    ldomXRange selection(
            forward[0].getStartXPointer(),
            forward[1].getEndXPointer());
    snapshot.selectionText = selection.getRangeText(' ', 1000);
    if (snapshot.selectionText != U"Needle middle needle")
        return fail("selection range text changed");
    return 0;
}

static std::string createRenderedFixture() {
    std::string fixture =
            "<?xml version=\"1.0\" encoding=\"UTF-8\"?>"
            "<FictionBook><body><section>"
            "<title><p>Rendered regression chapter</p></title>"
            "<table id=\"render-state-table\" "
            "style=\"float: left; width: 42%;\">"
            "<tr><td>Scoped render state in a floating single-column "
            "table must survive nested layout and pagination. "
            "<math xmlns=\"http://www.w3.org/1998/Math/MathML\">"
            "<msubsup id=\"render-state-math\"><mi>x</mi><mi>i</mi>"
            "<mn>2</mn></msubsup></math></td></tr>"
            "</table>";
    for (int index = 0; index < 80; index++) {
        fixture += "<p>Paragraph ";
        fixture += std::to_string(index);
        fixture += " carries deterministic alpha beta gamma delta epsilon "
                "text across several wrapped lines for pagination.";
        if (index == 30) {
            fixture += " Selection anchor bridge spans a rendered range "
                    "whose rectangles must remain stable.";
        }
        fixture += "</p>";
    }
    fixture += "</section></body></FictionBook>";
    return fixture;
}

struct RenderedDocumentSnapshot {
    std::vector<int> pageStarts;
    std::vector<int> pageHeights;
    std::vector<lString32> pageBookmarks;
    LVArray<lvRect> selectionRects;
    lString32 restoredBookmark;
    int restoredPage = -1;
};

class DocumentRegressionView : public LVDocView {
public:
    DocumentRegressionView() : LVDocView(16, true) {
    }

    int bookmarkRangeCount() const {
        return m_bmkRanges.length();
    }
};

static int snapshotRenderedDocument(
        RenderedDocumentSnapshot &snapshot) {
    const std::string fixture = createRenderedFixture();
    LVStreamRef stream = LVCreateMemoryStream(
            const_cast<char *>(fixture.data()),
            static_cast<int>(fixture.size()),
            true,
            LVOM_READ);
    if (stream.isNull())
        return fail("rendered document fixture stream was not created");

    DocumentRegressionView view;
    view.setShowCover(false);
    view.setPageHeaderInfo(0);
    view.setPageMargins(lvRect(8, 8, 8, 8));
    view.setViewMode(DVM_PAGES, 1);
    view.overrideVisiblePageCount(1);
    view.setDefaultFontFace(lString8("Roboto"));
    view.setStatusFontFace(lString8("Roboto"));
    view.setFontSize(20);
    view.Resize(320, 240);
    if (!view.LoadDocument(stream, U"rendered-regression.fb2", false))
        return fail("rendered document fixture did not load");
    view.Render(320, 240);

    ldomDocument *document = view.getDocument();
    ldomNode *renderStateTable =
            document->getElementById(U"render-state-table");
    if (!renderStateTable
            || renderStateTable->getRendMethod() != erm_table
            || !renderStateTable->getParentNode()
            || renderStateTable->getParentNode()->getNodeId()
                    != el_floatBox)
        return fail("render-state table did not enter the floated table path");
    ldomNode *renderStateMath =
            document->getElementById(U"render-state-math");
    if (!renderStateMath
            || renderStateMath->getRendMethod() != erm_table
            || !renderStateMath->getParentNode()
            || renderStateMath->getParentNode()->getNodeId()
                    != el_inlineBox)
        return fail("render-state MathML did not enter the table graph path");

    const int pageCount = view.getPageCount();
    if (pageCount < 8)
        return fail("rendered fixture did not produce enough pages");
    snapshot.pageStarts.reserve(pageCount);
    snapshot.pageHeights.reserve(pageCount);
    snapshot.pageBookmarks.reserve(pageCount);
    for (int page = 0; page < pageCount; page++) {
        const int start = view.getPageStartY(page);
        const int height = view.getPageHeight(page);
        if (start < 0 || height <= 0
                || (page > 0
                    && start <= snapshot.pageStarts.back()))
            return fail("rendered page sequence is not strictly ordered");
        if (!view.goToPage(page))
            return fail("rendered page navigation failed");
        ldomXPointer bookmark = view.getBookmark();
        if (bookmark.isNull()
                || view.getBookmarkPage(bookmark) != page)
            return fail("rendered page bookmark did not map to its page");
        snapshot.pageStarts.push_back(start);
        snapshot.pageHeights.push_back(height);
        snapshot.pageBookmarks.push_back(bookmark.toString());
    }

    const int restorePage = pageCount / 2;
    if (!view.goToPage(restorePage))
        return fail("position restore target page was not reachable");
    const lString32 savedBookmark = view.getBookmark().toString();
    if (savedBookmark.empty())
        return fail("position restore target bookmark was empty");
    view.savePosition();
    if (!view.goToPage(0))
        return fail("position restore fixture could not leave target page");
    view.restorePosition();
    snapshot.restoredPage = view.getCurPage();
    snapshot.restoredBookmark = view.getBookmark().toString();
    if (snapshot.restoredPage != restorePage
            || snapshot.restoredBookmark != savedBookmark)
        return fail("saved rendered position did not restore exactly");

    ldomXPointer selectionPointer = document->createXPointer(
            U"/FictionBook/body[1]/section[1]/p[31]/text()[1]");
    if (selectionPointer.isNull() || !selectionPointer.isText())
        return fail("rendered selection XPointer did not resolve");
    lString32 selectionText = selectionPointer.getNode()->getText();
    ldomXRange selection(
            ldomXPointer(selectionPointer.getNode(), 0),
            ldomXPointer(
                    selectionPointer.getNode(), selectionText.length()));
    selection.setFlags(0x11);

    static const int overlapStarts[] = {0, 5, 12};
    static const int overlapEnds[] = {10, 15, 20};
    static const lUInt32 overlapFlags[] = {0x11, 0x12, 0x14};
    ldomXRangeList overlappingRanges;
    overlappingRanges.reserve(3);
    for (int index = 0; index < 3; ++index) {
        std::unique_ptr<ldomXRange> range =
                std::make_unique<ldomXRange>(
                        ldomXPointer(
                                selectionPointer.getNode(),
                                overlapStarts[index]),
                        ldomXPointer(
                                selectionPointer.getNode(),
                                overlapEnds[index]));
        range->setFlags(overlapFlags[index]);
        overlappingRanges.add(range.release());
    }
    ldomXRangeList splitRanges(overlappingRanges, true);
    static const int expectedStarts[] = {0, 5, 10, 12, 15};
    static const int expectedEnds[] = {5, 10, 12, 15, 20};
    static const lUInt32 expectedFlags[] = {
        0x11, 0x13, 0x12, 0x16, 0x14
    };
    if (splitRanges.length() != 5)
        return fail("overlapping range owners did not split completely");
    for (int index = 0; index < splitRanges.length(); ++index) {
        if (splitRanges[index]->getStart().getOffset()
                    != expectedStarts[index]
                || splitRanges[index]->getEnd().getOffset()
                    != expectedEnds[index]
                || splitRanges[index]->getFlags()
                    != expectedFlags[index])
            return fail(
                    "overlapping range replacement changed its segments");
    }
    ldomMarkedRangeList splitDrawRanges;
    splitRanges.getRanges(splitDrawRanges);
    if (splitDrawRanges.length() < splitRanges.length())
        return fail("split range draw owners were not published");
    ldomMarkedRange *firstDrawOwner = splitDrawRanges[0];
    const int firstDrawCount = splitDrawRanges.length();
    splitRanges.getRanges(splitDrawRanges);
    if (splitDrawRanges.length() != firstDrawCount
            || splitDrawRanges[0] == firstDrawOwner)
        return fail("split range draw owner replacement was not isolated");

    ldomMarkedTextList splitTextRanges;
    overlappingRanges.splitText(
            splitTextRanges, selectionPointer.getNode());
    if (splitTextRanges.length() != 6)
        return fail("split text range owners did not cover the source");
    static const lUInt32 expectedTextFlags[] = {
        0x11, 0x13, 0x13, 0x17, 0x15
    };
    for (int index = 0; index < 5; ++index) {
        if (splitTextRanges[index]->offset != expectedStarts[index]
                || splitTextRanges[index]->text
                    != selectionText.substr(
                            expectedStarts[index],
                            expectedEnds[index]
                                    - expectedStarts[index])
                || splitTextRanges[index]->flags
                    != expectedTextFlags[index])
            return fail("split text owner changed its range payload");
    }
    if (splitTextRanges[5]->offset != 20
            || splitTextRanges[5]->text
                != selectionText.substr(20)
            || splitTextRanges[5]->flags != 0x01)
        return fail("split text owner lost the unmarked suffix");

    selection.getSegmentRects(snapshot.selectionRects);
    if (snapshot.selectionRects.length() < 2)
        return fail("rendered selection did not span line segments");
    for (int index = 0;
            index < snapshot.selectionRects.length(); index++) {
        const lvRect &rect = snapshot.selectionRects[index];
        if (rect.width() <= 0 || rect.height() <= 0
                || (index > 0
                    && rect.top
                            < snapshot.selectionRects[index - 1].top))
            return fail("rendered selection geometry is invalid");
    }
    view.selectRange(selection);
    if (document->getSelections().length() != 1
            || document->getSelections()[0]->getRangeText(' ', 1000)
                    != selectionText)
        return fail("rendered selection was not retained by the document");

    CRBookmark *rangeBookmark = view.saveRangeBookmark(
            selection, bmkt_comment, U"Range note");
    CRBookmark *pageBookmark =
            view.saveCurrentPageBookmark(U"Page note");
    CRFileHistRecord *record = view.getCurrentFileHistRecord();
    if (!rangeBookmark || !pageBookmark || !record
            || record->getBookmarks().length() != 2
            || view.bookmarkRangeCount() != 1)
        return fail("bookmark candidates did not publish complete ranges");

    LVColorDrawBuf highlightedPage(320, 240, 32);
    view.Draw(highlightedPage, false);

    CRBookmark *firstShortcut =
            view.saveCurrentPageShortcutBookmark(7);
    const int shortcutCount = record->getBookmarks().length();
    if (!view.goToPage(std::min(
                view.getCurPage() + 1, view.getPageCount() - 1)))
        return fail("shortcut replacement page was not reachable");
    CRBookmark *replacedShortcut =
            view.saveCurrentPageShortcutBookmark(7);
    if (!firstShortcut || !replacedShortcut
            || firstShortcut == replacedShortcut
            || record->getBookmarks().length() != shortcutCount
            || record->getShortcutBookmark(7) != replacedShortcut)
        return fail("shortcut replacement leaked or duplicated its owner");

    LVPtrVector<CRBookmark> replacements;
    replacements.reserve(2);
    std::unique_ptr<CRBookmark> rangeReplacement(
            new CRBookmark(*rangeBookmark));
    replacements.add(rangeReplacement.release());
    std::unique_ptr<CRBookmark> pageReplacement(
            new CRBookmark(*pageBookmark));
    replacements.add(pageReplacement.release());
    view.setBookmarkList(replacements);
    if (record->getBookmarks().length() != 2
            || record->getBookmarks()[0] == replacements[0]
            || record->getBookmarks()[1] == replacements[1]
            || record->getBookmarks()[0]->getCommentText()
                    != U"Range note"
            || record->getBookmarks()[1]->getCommentText()
                    != U"Page note"
            || view.bookmarkRangeCount() != 1)
        return fail("bookmark list replacement shared or lost ownership");

    CRBookmark *removed = record->getBookmarks()[0];
    CRBookmark missing;
    if (!view.removeBookmark(removed)
            || view.removeBookmark(&missing)
            || record->getBookmarks().length() != 1
            || replacements.length() != 2
            || replacements[0]->getCommentText() != U"Range note"
            || view.bookmarkRangeCount() != 0)
        return fail("bookmark removal corrupted independent owners");

    view.clearSelection();
    if (!document->getSelections().empty())
        return fail("rendered selection did not clear");
    return 0;
}

static bool equalRenderedSnapshots(
        const RenderedDocumentSnapshot &first,
        const RenderedDocumentSnapshot &second) {
    if (first.pageStarts != second.pageStarts
            || first.pageHeights != second.pageHeights
            || first.pageBookmarks != second.pageBookmarks
            || first.restoredPage != second.restoredPage
            || first.restoredBookmark != second.restoredBookmark
            || first.selectionRects.length()
                    != second.selectionRects.length())
        return false;
    for (int index = 0; index < first.selectionRects.length(); index++) {
        const lvRect &left = first.selectionRects[index];
        const lvRect &right = second.selectionRects[index];
        if (left.left != right.left || left.top != right.top
                || left.right != right.right
                || left.bottom != right.bottom)
            return false;
    }
    return true;
}

static int testDocViewDocumentOwnership() {
    static_assert(
            !std::is_copy_constructible<LVDocView>::value,
            "LVDocView must not duplicate document ownership");
    static_assert(
            !std::is_copy_assignable<LVDocView>::value,
            "LVDocView must not assign document ownership");

    LVDocView view(16, true);
    view.createHtmlDocument(
            U"<p id=\"first-owner\">first document</p>");
    ldomDocument *document = view.getDocument();
    if (!document
            || !document->getElementById(U"first-owner"))
        return fail("LVDocView did not publish its first owned document");

    view.createHtmlDocument(
            U"<p id=\"replacement-owner\">replacement document</p>");
    document = view.getDocument();
    if (!document
            || document->getElementById(U"first-owner")
            || !document->getElementById(U"replacement-owner"))
        return fail(
                "LVDocView replacement retained nodes from its released document");

    view.Clear();
    view.Clear();
    view.createDefaultDocument(
            U"Recreated owner", U"document after repeated Clear");
    document = view.getDocument();
    if (!document
            || view.getTitle() != U"Recreated owner"
            || document->getRootNode() == NULL)
        return fail(
                "LVDocView repeated Clear lost document-owner idempotence");
    return 0;
}

static int testRenderedDocumentBehavior() {
    if (!InitFontManager(lString8::empty_str) || !fontMan)
        return fail("rendered fixture font manager did not initialize");
    const lString8 fontPath(
            COOLREADER_SOURCE_DIR
            "/thirdparty/harfbuzz-14.2.1/test/subset/data/expected/"
            "retain-num-glyphs/Roboto-Regular.retain-num-glyphs.all.ttf");
    if (!fontMan->RegisterFont(fontPath)) {
        ShutdownFontManager();
        return fail("rendered fixture font did not register");
    }

    int result = 0;
    {
        RenderedDocumentSnapshot first;
        RenderedDocumentSnapshot second;
        if (testDocViewDocumentOwnership() != 0)
            result = 1;
        else if (snapshotRenderedDocument(first) != 0
                || snapshotRenderedDocument(second) != 0)
            result = 1;
        else if (!equalRenderedSnapshots(first, second))
            result = fail("equivalent renders produced different snapshots");
        else {
            const font_lang_compat firstCompatibility =
                    fontMan->checkFontLangCompat(
                            lString8("Roboto"), lString8("en"));
            const font_lang_compat cachedCompatibility =
                    fontMan->checkFontLangCompat(
                            lString8("Roboto"), lString8("en"));
            if (firstCompatibility == font_lang_compat_invalid_tag
                    || cachedCompatibility != firstCompatibility)
                result = fail(
                        "font language compatibility cache lost its owner");
        }
    }
    if (!ShutdownFontManager() && result == 0)
        return fail("rendered fixture font manager did not shut down");
    return result;
}

static int testConcurrentDocumentRegistry() {
    static const int workerCount = 8;
    static const int iterations = 200;
    std::atomic<int> ready(0);
    std::atomic<bool> start(false);
    std::atomic<bool> failed(false);
    std::vector<std::thread> workers;
    workers.reserve(workerCount);

    for (int workerIndex = 0;
            workerIndex < workerCount; workerIndex++) {
        workers.emplace_back([&ready, &start, &failed]() {
            ready.fetch_add(1, std::memory_order_release);
            while (!start.load(std::memory_order_acquire))
                std::this_thread::yield();
            for (int iteration = 0;
                    iteration < iterations; iteration++) {
                std::unique_ptr<ldomDocument> document(
                        new ldomDocument());
                ldomNode *root = document->getRootNode();
                if (root == NULL
                        || root->getDocument() != document.get()) {
                    failed.store(true, std::memory_order_release);
                    return;
                }
            }
        });
    }
    while (ready.load(std::memory_order_acquire) < workerCount)
        std::this_thread::yield();
    start.store(true, std::memory_order_release);
    for (std::thread &worker : workers)
        worker.join();
    if (failed.load(std::memory_order_acquire))
        return fail("concurrent documents shared a registry slot");
    return 0;
}

static bool writeCacheBytes(LVStreamRef &stream, int size) {
    std::vector<lUInt8> bytes(size, 0x5A);
    lvsize_t written = 0;
    return !stream.isNull()
            && stream->Write(bytes.data(), bytes.size(), &written) == LVERR_OK
            && written == bytes.size();
}

static int testBoundedObservableDocumentCache() {
    std::filesystem::path directoryPath =
            std::filesystem::temp_directory_path()
            / ("coolreader-doc-cache-"
                    + std::to_string(
                            std::chrono::steady_clock::now()
                                    .time_since_epoch().count()));
    std::error_code directoryError;
    if (!std::filesystem::create_directory(
                directoryPath, directoryError))
        return fail("document cache fixture directory could not be created");
    lString32 directory = Utf8ToUnicode(directoryPath.string().c_str());

    class CacheFixtureCleanup {
        lString32 _directory;
    public:
        explicit CacheFixtureCleanup(lString32 directory)
            : _directory(directory) {
        }
        ~CacheFixtureCleanup() {
            if (ldomDocCache::enabled()) {
                ldomDocCache::clear();
                ldomDocCache::close();
            }
            lString32 indexPath = _directory;
            LVAppendPathDelimiter(indexPath);
            LVDeleteFile(indexPath + U"cr3cache.inx");
            LVDeleteDirectory(_directory);
        }
    } cleanup(directory);

    if (!ldomDocCache::init(directory, 100))
        return fail("document cache fixture did not initialize");
    ldomDocCache::resetStats();

    lString32 firstPath;
    LVStreamRef first = ldomDocCache::createNew(
            U"first.fb2", 1, 0, 600, firstPath);
    if (!writeCacheBytes(first, 60))
        return fail("first document cache fixture could not be written");
    first.Clear();

    LVCacheStats stats = ldomDocCache::getStats();
    if (stats.capacity != 100 || stats.size != 60
            || stats.itemCount != 1)
        return fail("document cache did not record the finalized file size");

    lString32 secondPath;
    LVStreamRef second = ldomDocCache::createNew(
            U"second.fb2", 2, 0, 600, secondPath);
    if (!writeCacheBytes(second, 60))
        return fail("second document cache fixture could not be written");
    second.Clear();

    stats = ldomDocCache::getStats();
    if (stats.size != 60 || stats.itemCount != 1
            || stats.evictions != 1)
        return fail("document cache did not enforce its byte bound");
    if (LVFileExists(firstPath) || !LVFileExists(secondPath))
        return fail("document cache evicted the wrong file");

    lString32 cachePath;
    if (!ldomDocCache::openExisting(
                U"first.fb2", 1, 0, cachePath).isNull())
        return fail("evicted document cache file was reopened");
    LVStreamRef hit = ldomDocCache::openExisting(
            U"second.fb2", 2, 0, cachePath);
    if (hit.isNull())
        return fail("retained document cache file was not reopened");
    hit.Clear();

    lString32 oversizedPath;
    LVStreamRef oversized = ldomDocCache::createNew(
            U"oversized.fb2", 3, 0, 1200, oversizedPath);
    if (!writeCacheBytes(oversized, 120))
        return fail("oversized document cache fixture could not be written");
    oversized.Clear();
    stats = ldomDocCache::getStats();
    if (stats.size != 60 || stats.itemCount != 1
            || LVFileExists(oversizedPath))
        return fail("oversized document cache file was retained");
    if (stats.hits != 1 || stats.misses != 1
            || stats.evictions != 1)
        return fail("document cache counters are incorrect");

    lString32 pendingPath;
    LVStreamRef pending = ldomDocCache::createNew(
            U"pending.fb2", 4, 0, 20, pendingPath);
    if (!writeCacheBytes(pending, 20))
        return fail("pending document cache fixture could not be written");
    if (!ldomDocCache::clear() || LVFileExists(secondPath))
        return fail("document cache clear did not remove cached files");
    pending.Clear();
    if (LVFileExists(pendingPath))
        return fail("document cache clear admitted an in-progress file");
    stats = ldomDocCache::getStats();
    if (stats.size != 0 || stats.itemCount != 0)
        return fail("document cache retained accounting after clear");
    ldomDocCache::resetStats();
    stats = ldomDocCache::getStats();
    if (stats.hits != 0 || stats.misses != 0 || stats.evictions != 0)
        return fail("document cache counters did not reset");
    if (!ldomDocCache::close())
        return fail("document cache fixture did not close");
    return 0;
}

int main() {
    std::unique_ptr<ldomDocument> first = parseFixture();
    DocumentSnapshot firstSnapshot;
    if (snapshotDocument(first.get(), firstSnapshot) != 0)
        return 1;

    std::unique_ptr<ldomDocument> second = parseFixture();
    DocumentSnapshot secondSnapshot;
    if (snapshotDocument(second.get(), secondSnapshot) != 0)
        return 1;

    if (firstSnapshot.firstPosition != secondSnapshot.firstPosition
            || firstSnapshot.secondPosition
                    != secondSnapshot.secondPosition
            || firstSnapshot.selectionText
                    != secondSnapshot.selectionText) {
        return fail("document results changed between equivalent parses");
    }
    first.reset();
    second.reset();
    if (testRenderedDocumentBehavior() != 0)
        return 1;
    if (testConcurrentDocumentRegistry() != 0)
        return 1;
    return testBoundedObservableDocumentCache();
}
