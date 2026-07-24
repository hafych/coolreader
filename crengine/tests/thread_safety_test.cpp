#include "lvthread.h"
#include "crrecursionguard.h"
#include "lvdocview.h"

#include <atomic>
#include <cstdio>
#include <sched.h>

static int fail(const char *message) {
    std::fprintf(stderr, "%s\n", message);
    return 1;
}

class WorkerThread : public LVThread {
private:
    std::atomic<int> &_result;

protected:
    void run() override {
        _result.store(42, std::memory_order_release);
    }

public:
    explicit WorkerThread(std::atomic<int> &result)
        : _result(result) {
    }
};

static int testThreadCompletion() {
    std::atomic<int> result(0);
    WorkerThread worker(result);
    worker.start();
    while (!worker.stopped())
        sched_yield();
    worker.join();
    if (result.load(std::memory_order_acquire) != 42)
        return fail("LVThread did not publish its result");
    return 0;
}

static int testMutexAcrossThreads() {
    class LockingThread : public LVThread {
    private:
        LVMutex &_mutex;
        std::atomic<bool> &_entered;

    protected:
        void run() override {
            LVLock lock(_mutex);
            _entered.store(true, std::memory_order_release);
        }

    public:
        LockingThread(LVMutex &mutex, std::atomic<bool> &entered)
            : _mutex(mutex), _entered(entered) {
        }
    };

    LVMutex mutex;
    std::atomic<bool> entered(false);
    if (!mutex.lock())
        return fail("LVMutex could not be locked");
    LockingThread worker(mutex, entered);
    worker.start();
    sched_yield();
    if (entered.load(std::memory_order_acquire)) {
        mutex.unlock();
        worker.join();
        return fail("LVMutex did not exclude a second thread");
    }
    mutex.unlock();
    worker.join();
    if (!entered.load(std::memory_order_acquire))
        return fail("LVMutex did not release the waiting thread");
    return 0;
}

static int testThreadLocalRecursionLimit() {
    CRThreadLocalRecursionLimit outer;
    if (!outer.test(2))
        return fail("recursion limit rejected the first level");

    CRThreadLocalRecursionLimit inner;
    if (inner.test(2))
        return fail("recursion limit accepted its configured boundary");

    class RecursionThread : public LVThread {
    private:
        std::atomic<bool> &_isolated;

    protected:
        void run() override {
            CRThreadLocalRecursionLimit local;
            _isolated.store(local.test(2), std::memory_order_release);
        }

    public:
        explicit RecursionThread(std::atomic<bool> &isolated)
            : _isolated(isolated) {
        }
    };

    std::atomic<bool> isolated(false);
    RecursionThread worker(isolated);
    worker.start();
    worker.join();
    if (!isolated.load(std::memory_order_acquire))
        return fail("recursion depth leaked between threads");
    return 0;
}

static int testPageImageCacheMissReleasesMutex() {
#if CR_ENABLE_PAGE_IMAGE_CACHE==1
    class LockProbeThread : public LVThread {
    private:
        LVMutex &_mutex;
        std::atomic<bool> &_acquired;

    protected:
        void run() override {
            bool acquired = _mutex.trylock();
            _acquired.store(acquired, std::memory_order_release);
            if (acquired)
                _mutex.unlock();
        }

    public:
        LockProbeThread(LVMutex &mutex, std::atomic<bool> &acquired)
            : _mutex(mutex), _acquired(acquired) {
        }
    };

    LVDocViewImageCache cache;
    if (!cache.get(-1, 1).isNull())
        return fail("empty page image cache unexpectedly returned an item");
    std::atomic<bool> acquiredAfterGet(false);
    LockProbeThread afterGet(cache.getMutex(), acquiredAfterGet);
    afterGet.start();
    afterGet.join();
    if (!acquiredAfterGet.load(std::memory_order_acquire))
        return fail("page image cache get miss retained its mutex");

    if (cache.has(-1, 1))
        return fail("empty page image cache unexpectedly reported an item");
    std::atomic<bool> acquiredAfterHas(false);
    LockProbeThread afterHas(cache.getMutex(), acquiredAfterHas);
    afterHas.start();
    afterHas.join();
    if (!acquiredAfterHas.load(std::memory_order_acquire))
        return fail("page image cache has lookup retained its mutex");
#endif
    return 0;
}

int main() {
    if (testThreadCompletion() != 0)
        return 1;
    if (testMutexAcrossThreads() != 0)
        return 1;
    if (testThreadLocalRecursionLimit() != 0)
        return 1;
    return testPageImageCacheMissReleasesMutex();
}
