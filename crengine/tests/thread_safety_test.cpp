#include "lvthread.h"
#include "crconcurrent.h"
#include "crrecursionguard.h"
#include "lvdocview.h"
#include "lvstreamutils.h"
#include "wordfmt.h"

#include <atomic>
#include <condition_variable>
#include <cstdio>
#include <memory>
#include <mutex>
#include <sched.h>
#include <string>
#include <thread>
#include <type_traits>
#include <vector>

static const char INTERNED_TEXT_8[] = "thread-safe-interning-8";
static const char INTERNED_TEXT_32_NARROW[] = "thread-safe-interning-32-narrow";
static const lChar32 INTERNED_TEXT_32_WIDE[] = U"thread-safe-interning-32-wide";

static std::atomic<int> concurrencyMutexCreated(0);
static std::atomic<int> concurrencyMutexDestroyed(0);
static std::atomic<int> concurrencyMutexAcquired(0);
static std::atomic<int> concurrencyMutexReleased(0);

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

class QueueTrackedValue {
private:
    std::atomic<int> &_destroyed;

public:
    int id;

    QueueTrackedValue(int value, std::atomic<int> &destroyed)
        : _destroyed(destroyed)
        , id(value)
    {
    }

    ~QueueTrackedValue()
    {
        _destroyed.fetch_add(1, std::memory_order_relaxed);
    }
};

static int testQueueOwnership()
{
    static_assert(!std::is_copy_constructible<LVQueue<int> >::value,
            "LVQueue must not duplicate node ownership");

    LVQueue<int> values;
    values.pushBack(1);
    values.pushBack(2);
    values.pushFront(0);
    if (values.length() != 3)
        return fail("LVQueue did not retain all inserted values");
    {
        LVQueue<int>::Iterator iterator = values.iterator();
        if (!iterator.next() || iterator.get() != 0
                || !iterator.next() || iterator.get() != 1)
            return fail("LVQueue iterator order is inconsistent");
        iterator.moveToHead();
    }
    if (values.popFront() != 1)
        return fail("LVQueue move-to-head did not preserve its value");
    {
        LVQueue<int>::Iterator iterator = values.iterator();
        if (!iterator.next() || iterator.remove() != 0
                || iterator.get() != 2)
            return fail("LVQueue iterator removal lost its successor");
    }
    if (values.popBack() != 2 || values.length() != 0)
        return fail("LVQueue pop operations retained stale nodes");

    std::atomic<int> destroyed(0);
    {
        LVQueue<std::unique_ptr<QueueTrackedValue> > owned;
        owned.pushBack(std::unique_ptr<QueueTrackedValue>(
                new QueueTrackedValue(7, destroyed)));
        owned.pushFront(std::unique_ptr<QueueTrackedValue>(
                new QueueTrackedValue(3, destroyed)));
        std::unique_ptr<QueueTrackedValue> first = owned.popFront();
        if (!first || first->id != 3 || owned.length() != 1)
            return fail("LVQueue did not move its owned front value");
        first.reset();
        if (destroyed.load(std::memory_order_relaxed) != 1)
            return fail("LVQueue moved value did not keep singular ownership");
        owned.clear();
        if (destroyed.load(std::memory_order_relaxed) != 2)
            return fail("LVQueue clear did not release owned values");
    }
    if (destroyed.load(std::memory_order_relaxed) != 2)
        return fail("LVQueue destructor duplicated owned-value teardown");
    return 0;
}

class CountingConcurrencyMutex : public CRMutex {
private:
    std::recursive_mutex m_mutex;

public:
    CountingConcurrencyMutex()
    {
        concurrencyMutexCreated.fetch_add(1, std::memory_order_relaxed);
    }

    ~CountingConcurrencyMutex() override
    {
        concurrencyMutexDestroyed.fetch_add(1, std::memory_order_relaxed);
    }

    void acquire() override
    {
        m_mutex.lock();
        concurrencyMutexAcquired.fetch_add(1, std::memory_order_relaxed);
    }

    void release() override
    {
        concurrencyMutexReleased.fetch_add(1, std::memory_order_relaxed);
        m_mutex.unlock();
    }
};

class CountingConcurrencyProvider : public CRConcurrencyProvider {
private:
    int m_createCalls;
    int m_failAt;

public:
    explicit CountingConcurrencyProvider(int failAt = -1)
        : m_createCalls(0)
        , m_failAt(failAt)
    {
    }

    CRMutex *createMutex() override
    {
        int call = m_createCalls++;
        return call == m_failAt ? NULL : new CountingConcurrencyMutex();
    }

    CRMonitor *createMonitor() override
    {
        return NULL;
    }

    CRThread *createThread(CRRunnable *) override
    {
        return NULL;
    }

    void executeGui(CRRunnable *task) override
    {
        delete task;
    }

    void executeGui(CRRunnable *task, int) override
    {
        delete task;
    }

    void sleepMs(int) override
    {
    }
};

class ExecutorTestMutex : public CRMutex {
public:
    void acquire() override
    {
    }

    void release() override
    {
    }
};

class ExecutorTestMonitor : public CRMonitor {
private:
    std::atomic<int> &_destroyed;
    std::recursive_mutex _mutex;
    std::condition_variable_any _condition;

public:
    explicit ExecutorTestMonitor(std::atomic<int> &destroyed)
        : _destroyed(destroyed)
    {
    }

    ~ExecutorTestMonitor() override
    {
        _destroyed.fetch_add(1, std::memory_order_relaxed);
    }

    void acquire() override
    {
        _mutex.lock();
    }

    void release() override
    {
        _mutex.unlock();
    }

    void wait() override
    {
        std::unique_lock<std::recursive_mutex> lock(
                _mutex, std::adopt_lock);
        _condition.wait(lock);
        lock.release();
    }

    void notify() override
    {
        _condition.notify_one();
    }

    void notifyAll() override
    {
        _condition.notify_all();
    }
};

class ExecutorTestThread : public CRThread {
private:
    std::atomic<int> &_destroyed;
    CRRunnable *_task;
    std::thread _worker;

public:
    ExecutorTestThread(CRRunnable *task, std::atomic<int> &destroyed)
        : _destroyed(destroyed)
        , _task(task)
    {
    }

    ~ExecutorTestThread() override
    {
        if (_worker.joinable())
            _worker.join();
        _destroyed.fetch_add(1, std::memory_order_relaxed);
    }

    void start() override
    {
        _worker = std::thread([this]() {
            _task->run();
        });
    }

    void join() override
    {
        if (_worker.joinable())
            _worker.join();
    }
};

class ExecutorTestProvider : public CRConcurrencyProvider {
private:
    std::atomic<int> &_monitorDestroyed;
    std::atomic<int> &_threadDestroyed;

public:
    ExecutorTestProvider(std::atomic<int> &monitorDestroyed,
                         std::atomic<int> &threadDestroyed)
        : _monitorDestroyed(monitorDestroyed)
        , _threadDestroyed(threadDestroyed)
    {
    }

    CRMutex *createMutex() override
    {
        return new ExecutorTestMutex();
    }

    CRMonitor *createMonitor() override
    {
        return new ExecutorTestMonitor(_monitorDestroyed);
    }

    CRThread *createThread(CRRunnable *task) override
    {
        return new ExecutorTestThread(task, _threadDestroyed);
    }

    void executeGui(CRRunnable *task) override
    {
        delete task;
    }

    void executeGui(CRRunnable *task, int) override
    {
        delete task;
    }

    void sleepMs(int) override
    {
    }
};

class ExecutorTestTask : public CRRunnable {
private:
    std::atomic<int> &_ran;
    std::atomic<int> &_destroyed;

public:
    ExecutorTestTask(std::atomic<int> &ran,
                     std::atomic<int> &destroyed)
        : _ran(ran)
        , _destroyed(destroyed)
    {
    }

    ~ExecutorTestTask() override
    {
        _destroyed.fetch_add(1, std::memory_order_relaxed);
    }

    void run() override
    {
        _ran.fetch_add(1, std::memory_order_relaxed);
    }
};

class ExecutorBlockingTask : public CRRunnable {
private:
    std::atomic<bool> &_started;
    std::atomic<bool> &_release;
    std::atomic<int> &_destroyed;

public:
    ExecutorBlockingTask(std::atomic<bool> &started,
                         std::atomic<bool> &release,
                         std::atomic<int> &destroyed)
        : _started(started)
        , _release(release)
        , _destroyed(destroyed)
    {
    }

    ~ExecutorBlockingTask() override
    {
        _destroyed.fetch_add(1, std::memory_order_relaxed);
    }

    void run() override
    {
        _started.store(true, std::memory_order_release);
        while (!_release.load(std::memory_order_acquire))
            std::this_thread::yield();
    }
};

static int testThreadExecutorOwnership()
{
    std::atomic<int> monitorDestroyed(0);
    std::atomic<int> threadDestroyed(0);
    std::atomic<bool> blockingStarted(false);
    std::atomic<bool> releaseBlocking(false);
    std::atomic<int> blockingDestroyed(0);
    std::atomic<int> queuedRan(0);
    std::atomic<int> queuedDestroyed(0);
    ExecutorTestProvider provider(monitorDestroyed, threadDestroyed);
    CRConcurrencyProvider *savedProvider = concurrencyProvider;
    concurrencyProvider = &provider;

    const char *error = NULL;
    {
        CRThreadExecutor executor;
        executor.execute(new ExecutorBlockingTask(
                blockingStarted, releaseBlocking, blockingDestroyed));
        for (int spin = 0; spin < 100000
                && !blockingStarted.load(std::memory_order_acquire); ++spin)
            std::this_thread::yield();
        if (!blockingStarted.load(std::memory_order_acquire)) {
            error = "thread executor did not start its accepted task";
            releaseBlocking.store(true, std::memory_order_release);
            executor.stop();
        } else {
            executor.execute(new ExecutorTestTask(
                    queuedRan, queuedDestroyed));
            std::thread stopper([&executor]() {
                executor.stop();
            });
            for (int spin = 0; spin < 100000
                    && queuedDestroyed.load(std::memory_order_acquire) == 0;
                    ++spin)
                std::this_thread::yield();
            if (queuedDestroyed.load(std::memory_order_acquire) != 1)
                error = "thread executor stop retained its queued task";
            releaseBlocking.store(true, std::memory_order_release);
            stopper.join();
            executor.stop();
            executor.execute(new ExecutorTestTask(
                    queuedRan, queuedDestroyed));
            if (blockingDestroyed.load(std::memory_order_relaxed) != 1
                    || queuedRan.load(std::memory_order_relaxed) != 0
                    || queuedDestroyed.load(std::memory_order_relaxed) != 2)
                error = "thread executor task ownership is inconsistent";
        }
    }
    concurrencyProvider = savedProvider;
    if (error)
        return fail(error);
    if (monitorDestroyed.load(std::memory_order_relaxed) != 1
            || threadDestroyed.load(std::memory_order_relaxed) != 1)
        return fail("thread executor did not release monitor/thread owners");
    return 0;
}

static void resetConcurrencyMutexCounters()
{
    concurrencyMutexCreated.store(0, std::memory_order_relaxed);
    concurrencyMutexDestroyed.store(0, std::memory_order_relaxed);
    concurrencyMutexAcquired.store(0, std::memory_order_relaxed);
    concurrencyMutexReleased.store(0, std::memory_order_relaxed);
}

static bool engineMutexViewsAreNull()
{
    return _refMutex == NULL
            && _fontMutex == NULL
            && _fontManMutex == NULL
            && _fontGlyphCacheMutex == NULL
            && _fontLocalGlyphCacheMutex == NULL
            && _crengineMutex == NULL;
}

static bool engineMutexViewsAreReady()
{
    return _refMutex != NULL
            && _fontMutex != NULL
            && _fontManMutex != NULL
            && _fontGlyphCacheMutex != NULL
            && _fontLocalGlyphCacheMutex != NULL
            && _crengineMutex != NULL;
}

static bool engineRefMutexExcludesThreads()
{
    std::atomic<int> active(0);
    std::atomic<bool> overlapped(false);
    std::vector<std::thread> workers;
    for (int workerIndex = 0; workerIndex < 4; ++workerIndex) {
        workers.emplace_back([&active, &overlapped]() {
            for (int iteration = 0; iteration < 1000; ++iteration) {
                CRGuard guard(_refMutex);
                CR_UNUSED(guard);
                if (active.fetch_add(
                            1, std::memory_order_relaxed) != 0) {
                    overlapped.store(true, std::memory_order_relaxed);
                }
                std::this_thread::yield();
                active.fetch_sub(1, std::memory_order_relaxed);
            }
        });
    }
    for (std::thread &worker : workers)
        worker.join();
    return !overlapped.load(std::memory_order_relaxed);
}

static int testEngineConcurrencyLifecycle()
{
    if (!engineMutexViewsAreReady() || !engineRefMutexExcludesThreads())
        return fail("built-in engine mutex fallback is not active");
    CRShutdownEngineConcurrency();
    concurrencyProvider = NULL;
    resetConcurrencyMutexCounters();

    CountingConcurrencyProvider provider;
    concurrencyProvider = &provider;
    CRSetupEngineConcurrency();
    CRSetupEngineConcurrency();
    CRMutex *mutexes[] = {
        _refMutex,
        _fontMutex,
        _fontManMutex,
        _fontGlyphCacheMutex,
        _fontLocalGlyphCacheMutex,
        _crengineMutex
    };
    for (CRMutex *mutex : mutexes) {
        if (mutex == NULL) {
            CRShutdownEngineConcurrency();
            concurrencyProvider = NULL;
            return fail("engine mutex setup published a null slot");
        }
        CRGuard guard(mutex);
        CR_UNUSED(guard);
    }
    if (concurrencyMutexCreated.load(std::memory_order_relaxed) != 6
            || concurrencyMutexAcquired.load(std::memory_order_relaxed) != 6
            || concurrencyMutexReleased.load(std::memory_order_relaxed) != 6) {
        CRShutdownEngineConcurrency();
        concurrencyProvider = NULL;
        return fail("engine mutex setup was not idempotent");
    }

    CRShutdownEngineConcurrency();
    CRShutdownEngineConcurrency();
    concurrencyProvider = NULL;
    if (!engineMutexViewsAreNull()
            || concurrencyMutexDestroyed.load(
                    std::memory_order_relaxed) != 6) {
        return fail("engine mutex shutdown did not clear owned slots");
    }

    resetConcurrencyMutexCounters();
    CountingConcurrencyProvider failingProvider(2);
    concurrencyProvider = &failingProvider;
    CRSetupEngineConcurrency();
    concurrencyProvider = NULL;
    if (!engineMutexViewsAreNull()
            || concurrencyMutexCreated.load(
                    std::memory_order_relaxed) != 5
            || concurrencyMutexDestroyed.load(
                    std::memory_order_relaxed) != 5) {
        CRShutdownEngineConcurrency();
        return fail("partial engine mutex setup did not roll back");
    }
    CRShutdownEngineConcurrency();
    concurrencyProvider = NULL;
    CRSetupEngineConcurrency();
    if (!engineMutexViewsAreReady())
        return fail("engine mutex fallback did not restart after shutdown");
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

static int testStringLiteralInterningAcrossThreads() {
    class InterningThread : public LVThread {
    private:
        std::atomic<bool> &_start;
        std::atomic<bool> &_valid;

    protected:
        void run() override {
            while (!_start.load(std::memory_order_acquire))
                sched_yield();
            for (int i = 0; i < 10000; ++i) {
                const lString8 &value8 = cs8(INTERNED_TEXT_8);
                const lString32 &value32Narrow = cs32(INTERNED_TEXT_32_NARROW);
                const lString32 &value32Wide = cs32(INTERNED_TEXT_32_WIDE);
                if (value8 != INTERNED_TEXT_8
                        || value32Narrow != U"thread-safe-interning-32-narrow"
                        || value32Wide != INTERNED_TEXT_32_WIDE
                        || &value8 != &cs8(INTERNED_TEXT_8)
                        || &value32Narrow != &cs32(INTERNED_TEXT_32_NARROW)
                        || &value32Wide != &cs32(INTERNED_TEXT_32_WIDE)) {
                    _valid.store(false, std::memory_order_release);
                    return;
                }
            }
        }

    public:
        InterningThread(std::atomic<bool> &start, std::atomic<bool> &valid)
            : _start(start), _valid(valid) {
        }
    };

    std::atomic<bool> start(false);
    std::atomic<bool> valid(true);
    std::vector<std::unique_ptr<InterningThread> > workers;
    for (int i = 0; i < 8; ++i) {
        workers.emplace_back(new InterningThread(start, valid));
        workers.back()->start();
    }
    start.store(true, std::memory_order_release);
    for (size_t i = 0; i < workers.size(); ++i) {
        workers[i]->join();
    }
    if (!valid.load(std::memory_order_acquire))
        return fail("string literal interning was inconsistent across threads");
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

static bool importWordFixture(const char *fixturePath) {
    LVStreamRef detectionStream = LVOpenFileStream(fixturePath, LVOM_READ);
    if (detectionStream.isNull() || !DetectWordFormat(detectionStream))
        return false;

    LVStreamRef importStream = LVOpenFileStream(fixturePath, LVOM_READ);
    if (importStream.isNull())
        return false;
    std::unique_ptr<ldomDocument> document(new ldomDocument());
    if (!ImportWordDocument(
                importStream, document.get(), NULL, NULL))
        return false;
    ldomNode *root = document->getRootNode();
    return root != NULL && root->getChildCount() > 0;
}

static int testConcurrentWordImport() {
#if ENABLE_ANTIWORD==1
    const std::string fixturePath =
            std::string(COOLREADER_SOURCE_DIR)
            + "/thirdparty_unman/antiword/Docs/testdoc.doc";
    if (!importWordFixture(fixturePath.c_str()))
        return fail("baseline Antiword fixture import failed");

    static const int workerCount = 4;
    static const int iterations = 3;
    std::atomic<int> ready(0);
    std::atomic<bool> start(false);
    std::atomic<bool> valid(true);
    std::vector<std::thread> workers;
    workers.reserve(workerCount);
    for (int workerIndex = 0; workerIndex < workerCount; ++workerIndex) {
        workers.emplace_back([&fixturePath, &ready, &start, &valid]() {
            ready.fetch_add(1, std::memory_order_release);
            while (!start.load(std::memory_order_acquire))
                std::this_thread::yield();
            for (int iteration = 0; iteration < iterations; ++iteration) {
                if (!importWordFixture(fixturePath.c_str())) {
                    valid.store(false, std::memory_order_release);
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
    if (!valid.load(std::memory_order_acquire))
        return fail("concurrent Antiword imports shared mutable context");
#endif
    return 0;
}

int main() {
    if (testThreadCompletion() != 0)
        return 1;
    if (testMutexAcrossThreads() != 0)
        return 1;
    if (testQueueOwnership() != 0)
        return 1;
    if (testThreadExecutorOwnership() != 0)
        return 1;
    if (testEngineConcurrencyLifecycle() != 0)
        return 1;
    if (testThreadLocalRecursionLimit() != 0)
        return 1;
    if (testStringLiteralInterningAcrossThreads() != 0)
        return 1;
    if (testPageImageCacheMissReleasesMutex() != 0)
        return 1;
    return testConcurrentWordImport();
}
