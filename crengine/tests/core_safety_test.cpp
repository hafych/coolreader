#include "lvstreamutils.h"
#include "lvthread.h"

#include <cerrno>
#include <cstdlib>
#include <cstdio>
#include <cstring>
#include <fcntl.h>
#include <unistd.h>

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

int main() {
    if (testMutex() != 0)
        return 1;
    if (testOwnedDescriptor() != 0)
        return 1;
    return testBorrowedDescriptor();
}
