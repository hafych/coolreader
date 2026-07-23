#include "lvstreamutils.h"
#include "lvthread.h"
#include "lvxmlparser.h"
#include "lvxmlparsercallback.h"
#include "parsebudget.h"

#include <cerrno>
#include <cstdlib>
#include <cstdio>
#include <cstring>
#include <cstdint>
#include <fcntl.h>
#include <string>
#include <unistd.h>
#include <vector>

static int fail(const char *message) {
    std::fprintf(stderr, "%s\n", message);
    return 1;
}

static int testMutex() {
    LVMutex mutex;
    if (!mutex.trylock())
        return fail("LVMutex::trylock failed after initialization");
    if (!mutex.trylock())
        return fail("LVMutex is not recursive");
    mutex.unlock();
    mutex.unlock();

    {
        LVLock lock(mutex);
        if (!mutex.trylock())
            return fail("LVLock did not leave the mutex usable");
        mutex.unlock();
    }
    return 0;
}

static int testOwnedDescriptor() {
    char path[] = "/tmp/coolreader-fd-test-XXXXXX";
    int fd = mkstemp(path);
    if (fd < 0)
        return fail("mkstemp failed");

    const char payload[] = "coolreader";
    if (write(fd, payload, sizeof(payload)) != static_cast<ssize_t>(sizeof(payload))) {
        close(fd);
        unlink(path);
        return fail("temporary-file write failed");
    }

    LVStreamRef stream = LVOpenFileDescriptorStream(
            fd, lString32(U"owned"), LVOM_READ, true);
    if (stream.isNull()) {
        close(fd);
        unlink(path);
        return fail("owned descriptor stream creation failed");
    }
    if (lString32(stream->GetName()) != lString32(U"owned")) {
        close(fd);
        stream.Clear();
        unlink(path);
        return fail("logical descriptor stream name was not preserved");
    }
    close(fd);

    char buffer[sizeof(payload)] = {};
    lvsize_t bytesRead = 0;
    if (stream->Read(buffer, sizeof(buffer), &bytesRead) != LVERR_OK
            || bytesRead != sizeof(payload)
            || std::memcmp(buffer, payload, sizeof(payload)) != 0) {
        stream.Clear();
        unlink(path);
        return fail("descriptor duplicate did not remain readable");
    }
    stream.Clear();
    unlink(path);
    return 0;
}

static int testBorrowedDescriptor() {
    char path[] = "/tmp/coolreader-borrowed-fd-test-XXXXXX";
    int fd = mkstemp(path);
    if (fd < 0)
        return fail("mkstemp failed");

    {
        LVStreamRef stream = LVOpenFileDescriptorStream(
                fd, lString32(U"borrowed"), LVOM_READ, false);
        if (stream.isNull()) {
            close(fd);
            unlink(path);
            return fail("borrowed descriptor stream creation failed");
        }
    }

    if (fcntl(fd, F_GETFD) == -1 && errno == EBADF) {
        unlink(path);
        return fail("borrowed descriptor was closed by LVFileStream");
    }
    close(fd);
    unlink(path);
    return 0;
}

struct ZipEntrySpec {
    std::string name;
    std::uint32_t unpackedSize;
};

class NoOpXmlCallback : public LVXMLParserCallback {
public:
    virtual void OnStop() {}
    virtual ldomNode *OnTagOpen(const lChar32 *, const lChar32 *)
    {
        return NULL;
    }
    virtual void OnTagBody() {}
    virtual void OnTagClose(const lChar32 *, const lChar32 *, bool) {}
    virtual void OnAttribute(const lChar32 *, const lChar32 *,
                             const lChar32 *) {}
    virtual void OnText(const lChar32 *, int, lUInt32) {}
    virtual bool OnBlob(lString32, const lUInt8 *, int)
    {
        return true;
    }
};

class AutoClosingDepthXmlCallback : public NoOpXmlCallback {
private:
    int m_depth;

public:
    AutoClosingDepthXmlCallback() : m_depth(0) {}

    virtual void OnStart(LVFileFormatParser *parser)
    {
        LVXMLParserCallback::OnStart(parser);
        m_depth = 0;
    }

    virtual int GetCurrentElementDepth() const
    {
        return m_depth;
    }

    virtual ldomNode *OnTagOpen(const lChar32 *, const lChar32 *tagname)
    {
        // Model HTML's implicit close of a previous paragraph.
        if (lString32(tagname) == U"p")
            m_depth = 1;
        else
            ++m_depth;
        return NULL;
    }

    virtual void OnTagClose(const lChar32 *, const lChar32 *, bool)
    {
        if (m_depth > 0)
            --m_depth;
    }
};

static void appendLe16(std::vector<unsigned char> &bytes, std::uint16_t value) {
    bytes.push_back(static_cast<unsigned char>(value));
    bytes.push_back(static_cast<unsigned char>(value >> 8));
}

static void appendLe32(std::vector<unsigned char> &bytes, std::uint32_t value) {
    bytes.push_back(static_cast<unsigned char>(value));
    bytes.push_back(static_cast<unsigned char>(value >> 8));
    bytes.push_back(static_cast<unsigned char>(value >> 16));
    bytes.push_back(static_cast<unsigned char>(value >> 24));
}

static std::vector<unsigned char> buildHeaderOnlyZip(
        const std::vector<ZipEntrySpec> &entries) {
    std::vector<unsigned char> bytes;
    std::vector<std::uint32_t> offsets;
    for (const ZipEntrySpec &entry : entries) {
        offsets.push_back(static_cast<std::uint32_t>(bytes.size()));
        appendLe32(bytes, 0x04034b50);
        appendLe16(bytes, 20);
        appendLe16(bytes, 0);
        appendLe16(bytes, 8);
        appendLe16(bytes, 0);
        appendLe16(bytes, 0);
        appendLe32(bytes, 0);
        appendLe32(bytes, 0);
        appendLe32(bytes, entry.unpackedSize);
        appendLe16(bytes, static_cast<std::uint16_t>(entry.name.size()));
        appendLe16(bytes, 0);
        bytes.insert(bytes.end(), entry.name.begin(), entry.name.end());
    }

    const std::uint32_t centralOffset = static_cast<std::uint32_t>(bytes.size());
    for (std::size_t i = 0; i < entries.size(); i++) {
        const ZipEntrySpec &entry = entries[i];
        appendLe32(bytes, 0x02014b50);
        appendLe16(bytes, 20);
        appendLe16(bytes, 20);
        appendLe16(bytes, 0);
        appendLe16(bytes, 8);
        appendLe16(bytes, 0);
        appendLe16(bytes, 0);
        appendLe32(bytes, 0);
        appendLe32(bytes, 0);
        appendLe32(bytes, entry.unpackedSize);
        appendLe16(bytes, static_cast<std::uint16_t>(entry.name.size()));
        appendLe16(bytes, 0);
        appendLe16(bytes, 0);
        appendLe16(bytes, 0);
        appendLe16(bytes, 0);
        appendLe32(bytes, 0);
        appendLe32(bytes, offsets[i]);
        bytes.insert(bytes.end(), entry.name.begin(), entry.name.end());
    }

    const std::uint32_t centralSize =
            static_cast<std::uint32_t>(bytes.size()) - centralOffset;
    appendLe32(bytes, 0x06054b50);
    appendLe16(bytes, 0);
    appendLe16(bytes, 0);
    appendLe16(bytes, static_cast<std::uint16_t>(entries.size()));
    appendLe16(bytes, static_cast<std::uint16_t>(entries.size()));
    appendLe32(bytes, centralSize);
    appendLe32(bytes, centralOffset);
    appendLe16(bytes, 0);
    return bytes;
}

static std::vector<unsigned char> buildStoredZip(
        const std::string &name, const std::vector<unsigned char> &payload) {
    std::vector<unsigned char> bytes;
    appendLe32(bytes, 0x04034b50);
    appendLe16(bytes, 20);
    appendLe16(bytes, 0);
    appendLe16(bytes, 0);
    appendLe16(bytes, 0);
    appendLe16(bytes, 0);
    appendLe32(bytes, 0);
    appendLe32(bytes, static_cast<std::uint32_t>(payload.size()));
    appendLe32(bytes, static_cast<std::uint32_t>(payload.size()));
    appendLe16(bytes, static_cast<std::uint16_t>(name.size()));
    appendLe16(bytes, 0);
    bytes.insert(bytes.end(), name.begin(), name.end());
    bytes.insert(bytes.end(), payload.begin(), payload.end());

    const std::uint32_t centralOffset = static_cast<std::uint32_t>(bytes.size());
    appendLe32(bytes, 0x02014b50);
    appendLe16(bytes, 20);
    appendLe16(bytes, 20);
    appendLe16(bytes, 0);
    appendLe16(bytes, 0);
    appendLe16(bytes, 0);
    appendLe16(bytes, 0);
    appendLe32(bytes, 0);
    appendLe32(bytes, static_cast<std::uint32_t>(payload.size()));
    appendLe32(bytes, static_cast<std::uint32_t>(payload.size()));
    appendLe16(bytes, static_cast<std::uint16_t>(name.size()));
    appendLe16(bytes, 0);
    appendLe16(bytes, 0);
    appendLe16(bytes, 0);
    appendLe16(bytes, 0);
    appendLe32(bytes, 0);
    appendLe32(bytes, 0);
    bytes.insert(bytes.end(), name.begin(), name.end());

    const std::uint32_t centralSize =
            static_cast<std::uint32_t>(bytes.size()) - centralOffset;
    appendLe32(bytes, 0x06054b50);
    appendLe16(bytes, 0);
    appendLe16(bytes, 0);
    appendLe16(bytes, 1);
    appendLe16(bytes, 1);
    appendLe32(bytes, centralSize);
    appendLe32(bytes, centralOffset);
    appendLe16(bytes, 0);
    return bytes;
}

static bool zipOpens(const std::vector<ZipEntrySpec> &entries) {
    std::vector<unsigned char> bytes = buildHeaderOnlyZip(entries);
    LVStreamRef stream = LVCreateMemoryStream(
            bytes.data(), static_cast<int>(bytes.size()), true, LVOM_READ);
    LVContainerRef archive = LVOpenArchieve(stream);
    return !archive.isNull();
}

static int testZipArchiveBudgets() {
    if (!zipOpens({{"OPS/content.opf", 0}}))
        return fail("safe ZIP archive was rejected");
    if (zipOpens({{"../secret", 0}}))
        return fail("ZIP traversal entry was accepted");
    if (zipOpens({{"/absolute/path", 0}}))
        return fail("absolute ZIP entry was accepted");
    if (zipOpens({{std::string("safe\0hidden", 11), 0}}))
        return fail("ZIP entry with embedded NUL was accepted");
    if (zipOpens({{"OPS/content.opf", 0}, {"OPS/content.opf", 0}}))
        return fail("duplicate ZIP entry was accepted");
    if (zipOpens({{"part-1", 600U * 1024U * 1024U},
                  {"part-2", 600U * 1024U * 1024U}}))
        return fail("ZIP total uncompressed size limit was not enforced");
    if (zipOpens({{"single-entry", 257U * 1024U * 1024U}}))
        return fail("ZIP single entry size limit was not enforced");

    std::vector<ZipEntrySpec> tooManyEntries;
    tooManyEntries.reserve(10001);
    for (int i = 0; i < 10001; i++)
        tooManyEntries.push_back({"entry-" + std::to_string(i), 0});
    if (zipOpens(tooManyEntries))
        return fail("ZIP entry count limit was not enforced");

    std::string tooDeepPath;
    for (int i = 0; i < 65; i++)
        tooDeepPath += (i == 0 ? "" : "/") + std::string("level");
    if (zipOpens({{tooDeepPath, 0}}))
        return fail("ZIP path depth limit was not enforced");
    return 0;
}

static int testParseBudgetCodes() {
    ParseBudgetLimits limits = ParseBudgetLimits::defaults();

    limits.maxInputBytes = 10;
    ParseBudget inputBudget(limits);
    if (inputBudget.checkInputBytes(11)
            || inputBudget.error() != PARSE_BUDGET_INPUT_BYTES)
        return fail("ParseBudget input byte code mismatch");

    limits = ParseBudgetLimits::defaults();
    limits.maxDecodedTextCharacters = 10;
    ParseBudget textBudget(limits);
    if (!textBudget.consumeTextCharacters(6)
            || textBudget.consumeTextCharacters(5)
            || textBudget.error() != PARSE_BUDGET_TEXT_CHARACTERS)
        return fail("ParseBudget text character code mismatch");

    limits = ParseBudgetLimits::defaults();
    limits.maxXmlDepth = 2;
    ParseBudget xmlBudget(limits);
    if (!xmlBudget.enterXmlElement() || !xmlBudget.enterXmlElement()
            || xmlBudget.enterXmlElement()
            || xmlBudget.error() != PARSE_BUDGET_XML_DEPTH)
        return fail("ParseBudget XML depth code mismatch");

    limits = ParseBudgetLimits::defaults();
    limits.maxArchiveEntries = 1;
    ParseBudget countBudget(limits);
    if (!countBudget.consumeArchiveEntry(0)
            || countBudget.consumeArchiveEntry(0)
            || countBudget.error() != PARSE_BUDGET_ARCHIVE_ENTRY_COUNT)
        return fail("ParseBudget archive count code mismatch");

    limits = ParseBudgetLimits::defaults();
    limits.maxArchiveEntryBytes = 5;
    ParseBudget entryBudget(limits);
    if (entryBudget.consumeArchiveEntry(6)
            || entryBudget.error() != PARSE_BUDGET_ARCHIVE_ENTRY_BYTES)
        return fail("ParseBudget archive entry byte code mismatch");

    limits = ParseBudgetLimits::defaults();
    limits.maxArchiveEntryBytes = 10;
    limits.maxArchiveTotalBytes = 10;
    ParseBudget totalBudget(limits);
    if (!totalBudget.consumeArchiveEntry(6)
            || totalBudget.consumeArchiveEntry(5)
            || totalBudget.error() != PARSE_BUDGET_ARCHIVE_TOTAL_BYTES)
        return fail("ParseBudget archive total byte code mismatch");

    limits = ParseBudgetLimits::defaults();
    limits.archiveRatioMinimumBytes = 10;
    limits.maxArchiveCompressionRatio = 2;
    ParseBudget ratioBudget(limits);
    if (ratioBudget.checkArchiveCompression(5, 11)
            || ratioBudget.error()
                    != PARSE_BUDGET_ARCHIVE_COMPRESSION_RATIO)
        return fail("ParseBudget compression ratio code mismatch");
    limits.maxArchiveCompressionRatio = 0;
    ParseBudget zeroRatioBudget(limits);
    if (zeroRatioBudget.checkArchiveCompression(5, 11)
            || zeroRatioBudget.error()
                    != PARSE_BUDGET_ARCHIVE_COMPRESSION_RATIO)
        return fail("ParseBudget zero compression ratio was not safe");

    limits = ParseBudgetLimits::defaults();
    limits.maxArchivePathDepth = 2;
    ParseBudget pathBudget(limits);
    if (pathBudget.checkArchivePathDepth(3)
            || pathBudget.error() != PARSE_BUDGET_ARCHIVE_PATH_DEPTH)
        return fail("ParseBudget archive path depth code mismatch");

    limits = ParseBudgetLimits::defaults();
    limits.maxContainerDepth = 2;
    ParseBudget containerBudget(limits);
    if (containerBudget.checkContainerDepth(3)
            || containerBudget.error() != PARSE_BUDGET_CONTAINER_DEPTH)
        return fail("ParseBudget container depth code mismatch");

    limits = ParseBudgetLimits::defaults();
    limits.maxImageDimension = 10;
    limits.maxImagePixels = 50;
    ParseBudget imageBudget(limits);
    if (imageBudget.checkImageDimensions(8, 8)
            || imageBudget.error() != PARSE_BUDGET_IMAGE_DIMENSIONS)
        return fail("ParseBudget image dimension code mismatch");

    if (std::strcmp(parseBudgetErrorName(PARSE_BUDGET_XML_DEPTH),
                    "xml-depth") != 0)
        return fail("ParseBudget stable error name mismatch");
    return 0;
}

static bool parseXml(const std::string &xml, const ParseBudgetLimits &limits,
                     ParseBudgetErrorCode &error) {
    LVStreamRef stream = LVCreateMemoryStream(
            const_cast<char *>(xml.data()), static_cast<int>(xml.size()),
            true, LVOM_READ);
    NoOpXmlCallback callback;
    LVXMLParser parser(stream, &callback);
    parser.SetCharset(U"utf-8");
    parser.SetParseBudgetLimits(limits);
    const bool result = parser.Parse();
    error = parser.GetParseBudgetError();
    return result;
}

static int testXmlParseBudgetIntegration() {
    ParseBudgetErrorCode error = PARSE_BUDGET_OK;
    if (!parseXml("<root><child>safe</child></root>",
                  ParseBudgetLimits::defaults(), error)
            || error != PARSE_BUDGET_OK)
        return fail("safe XML was rejected by ParseBudget");

    std::string deepXml;
    for (unsigned i = 0;
            i < ParseBudgetLimits::defaults().maxXmlDepth + 1; ++i)
        deepXml += "<n>";
    for (unsigned i = 0;
            i < ParseBudgetLimits::defaults().maxXmlDepth + 1; ++i)
        deepXml += "</n>";
    if (parseXml(deepXml, ParseBudgetLimits::defaults(), error)
            || error != PARSE_BUDGET_XML_DEPTH)
        return fail("deep XML did not fail with XML depth code");

    ParseBudgetLimits textLimits = ParseBudgetLimits::defaults();
    textLimits.maxDecodedTextCharacters = 4;
    if (parseXml("<root>hello</root>", textLimits, error)
            || error != PARSE_BUDGET_TEXT_CHARACTERS)
        return fail("large decoded XML text did not fail with text code");

    ParseBudgetLimits inputLimits = ParseBudgetLimits::defaults();
    inputLimits.maxInputBytes = 4;
    if (parseXml("<root/>", inputLimits, error)
            || error != PARSE_BUDGET_INPUT_BYTES)
        return fail("large XML input did not fail with input code");

    std::string autoClosingHtml;
    for (int i = 0; i < 300; ++i)
        autoClosingHtml += "<p>paragraph";
    LVStreamRef htmlStream = LVCreateMemoryStream(
            const_cast<char *>(autoClosingHtml.data()),
            static_cast<int>(autoClosingHtml.size()), true, LVOM_READ);
    AutoClosingDepthXmlCallback htmlCallback;
    LVXMLParser htmlParser(htmlStream, &htmlCallback);
    htmlParser.SetCharset(U"utf-8");
    ParseBudgetLimits htmlLimits = ParseBudgetLimits::defaults();
    htmlLimits.maxXmlDepth = 2;
    htmlParser.SetParseBudgetLimits(htmlLimits);
    if (!htmlParser.Parse()
            || htmlParser.GetParseBudgetError() != PARSE_BUDGET_OK)
        return fail("HTML callback depth/autoclose was ignored");
    return 0;
}

static int testRecursiveContainerBudget() {
    std::vector<unsigned char> nested =
            buildStoredZip("leaf.txt", std::vector<unsigned char>{'x'});
    for (unsigned depth = 1;
            depth <= ParseBudgetLimits::defaults().maxContainerDepth; ++depth)
        nested = buildStoredZip("nested.zip", nested);

    LVStreamRef stream = LVCreateMemoryStream(
            nested.data(), static_cast<int>(nested.size()), true, LVOM_READ);
    for (unsigned depth = 1;
            depth <= ParseBudgetLimits::defaults().maxContainerDepth; ++depth) {
        LVContainerRef archive = LVOpenArchieve(stream);
        if (archive.isNull()) {
            std::fprintf(stderr,
                         "archive at recursive depth %u (stream depth %u) was rejected\n",
                         depth, stream->GetContainerDepth());
            return 1;
        }
        stream = archive->OpenStream(U"nested.zip", LVOM_READ);
        if (stream.isNull())
            return fail("nested archive entry could not be opened");
        if (depth == 1) {
            LVStreamRef memoryCopy = LVCreateMemoryStream(stream);
            LVStreamRef buffered = LVCreateBufferedStream(stream, 4096);
            if (memoryCopy.isNull() || buffered.isNull()
                    || memoryCopy->GetContainerDepth() != depth
                    || buffered->GetContainerDepth() != depth)
                return fail("stream wrapper lost recursive container depth");
        }
    }
    if (!LVOpenArchieve(stream).isNull())
        return fail("recursive container depth limit was not enforced");
    return 0;
}

int main() {
    if (testMutex() != 0)
        return 1;
    if (testOwnedDescriptor() != 0)
        return 1;
    if (testBorrowedDescriptor() != 0)
        return 1;
    if (testZipArchiveBudgets() != 0)
        return 1;
    if (testParseBudgetCodes() != 0)
        return 1;
    if (testXmlParseBudgetIntegration() != 0)
        return 1;
    return testRecursiveContainerBudget();
}
