#include "lvthread.h"
#include "crrecursionguard.h"

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

int main() {
    if (testThreadCompletion() != 0)
        return 1;
    if (testMutexAcrossThreads() != 0)
        return 1;
    return testThreadLocalRecursionLimit();
}
