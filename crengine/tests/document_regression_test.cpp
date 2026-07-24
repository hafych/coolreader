#include "fb2def.h"
#include "lvtinydom.h"
#include "lvstreamutils.h"

#include <atomic>
#include <chrono>
#include <cstdio>
#include <filesystem>
#include <memory>
#include <thread>
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
    if (testConcurrentDocumentRegistry() != 0)
        return 1;
    return testBoundedObservableDocumentCache();
}
