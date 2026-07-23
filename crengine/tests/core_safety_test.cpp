#include "lvstreamutils.h"
#include "lvthread.h"

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

int main() {
    if (testMutex() != 0)
        return 1;
    if (testOwnedDescriptor() != 0)
        return 1;
    if (testBorrowedDescriptor() != 0)
        return 1;
    return testZipArchiveBudgets();
}
