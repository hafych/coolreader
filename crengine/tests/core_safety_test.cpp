#include "chmfmt.h"
#include "crtxtenc.h"
#include "lvstreamutils.h"
#include "lvstreamfragment.h"
#include "lvarray.h"
#include "lvhashtable.h"
#include "lvmemman.h"
#include "lvpagesplitter.h"
#include "lvptrvec.h"
#include "lvrefcache.h"
#include "lvstring8collection.h"
#include "lvstring32hashedcollection.h"
#include "lvthread.h"
#include "lvcontaineriteminfo.h"
#include "lvdocview.h"
#include "hyphman.h"
#include "lvcolordrawbuf.h"
#include "lvfntman.h"
#include "lvfnt.h"
#include "lvgraydrawbuf.h"
#include "lvimg.h"
#include "lvimagedecodercallback.h"
#include "lvrend.h"
#include "lvstreambuffer.h"
#include "lvstyles.h"
#include "crgui.h"
#include "crtest.h"
#include "crskin.h"
#include "hist.h"
#include "lvhtmlparser.h"
#include "lvtextbookmarkparser.h"
#include "lvtextparser.h"
#include "lvxmlparser.h"
#include "lvxmlparsercallback.h"
#include "cri18n.h"
#include "dtddef.h"
#include "fb3fmt.h"
#include "lstridmap.h"
#include "lvembeddedfont.h"
#include "logredactor.h"
#include "odtfmt.h"
#include "parsebudget.h"
#include "pdbfmt.h"
#include "props.h"
#include "rtfimp.h"
#include "serialbuf.h"
#include "textlang.h"
#include "../src/chmfmt_internal.h"
#include "../src/crtxtenc_internal.h"
#include "../src/lvdrawbuf/lvimagescaleddrawcallback.h"
#include "../src/lvfont/lvfontcache.h"
#include "../src/lvfont/lvfontglyphcache.h"
#include "../src/lvfont/lvwin32font.h"
#if (USE_FREETYPE==1)
#include "../src/lvfont/lvfreetypeface.h"
#endif
#include "../src/lvstream/lvfilestream.h"
#include "../src/lvstream/lvfilemappedstream.h"
#include "../src/lvstream/lvmemorystream.h"
#include "../src/lvtextfm_internal.h"
#include "../src/lvtinydom_internal.h"
#include "../src/lvxml/lvtextlinequeue.h"
#include "../src/pdbfmt_internal.h"
#include "../src/wolutil_internal.h"
#include "../../cr3gui/src/t9encoding.h"
#if (USE_GIF==1)
#include "../src/lvimg/clzwdecoder.h"
#include "../src/lvimg/lvgifimagesource.h"
#endif
#if (USE_LIBPNG==1)
#include "../src/lvimg/lvpngimagesource.h"
#endif
#if (USE_LIBJPEG==1)
#include "../src/lvimg/lvjpegimagesource.h"
#endif

#include <algorithm>
#include <atomic>
#include <cerrno>
#include <cstdlib>
#include <cstdio>
#include <cstring>
#include <cstdint>
#include <fcntl.h>
#include <limits>
#include <memory>
#include <stdexcept>
#include <string>
#include <sys/stat.h>
#include <thread>
#include <unistd.h>
#include <vector>
#include <zlib.h>

static int fail(const char *message) {
    std::fprintf(stderr, "%s\n", message);
    return 1;
}

static int testRectangleClipValue() {
    const lvRect source(10, 20, 110, 220);
    std::optional<lvRect> trim =
            source.clipBy(lvRect(20, 30, 100, 200));
    if (!trim.has_value()
            || trim->left != 10 || trim->top != 10
            || trim->right != 10 || trim->bottom != 20)
        return fail("rectangle clipping did not return value-owned trims");

    if (source.clipBy(lvRect(0, 0, 200, 300)).has_value())
        return fail("rectangle clipping reported a containing clip");
    if (source.clipBy(lvRect(200, 300, 400, 500)).has_value())
        return fail("rectangle clipping reported a disjoint clip");
    return 0;
}

static int testT9EncodingCurrentWidth() {
    T9ClassicEncoding classic;
    if (classic.length() != 10
            || classic.encode_string(lString32(U"Quick")) != "78425")
        return fail("classic T9 mapping changed during UTF-32 migration");

    const lChar32 *unicodeDefinitions[] = {
        U"", U"абв", U"где", NULL
    };
    TEncoding unicodeEncoding(unicodeDefinitions);
    if (unicodeEncoding.encode_string(lString32(U"БЕД")) != "122")
        return fail("T9 Unicode lowercase mapping was not preserved");

    lString32Collection replacement;
    replacement.add(U"ab");
    replacement.add(U"cd");
    unicodeEncoding.set(replacement);
    if (unicodeEncoding.length() != 2
            || unicodeEncoding.encode_string(lString32(U"Dab")) != "100")
        return fail("T9 runtime layout replacement changed");

    const lChar32 *supplementaryDefinitions[] = {
        U"", U"\U0001F600", NULL
    };
    TEncoding supplementaryEncoding(supplementaryDefinitions);
    if (supplementaryEncoding.encode_string(
                lString32(U"\U0001F600")) != "1")
        return fail("UTF-32 T9 input split a supplementary character");
    return 0;
}

#ifdef _DEBUG
static int testCompareStreamScratchOwnership() {
    static lUInt8 equalBytes[] = {1, 2, 3, 4, 5, 6};
    LVStreamRef equalLeft = LVCreateMemoryStream(
            equalBytes, sizeof(equalBytes), true, LVOM_READ);
    LVStreamRef equalRight = LVCreateMemoryStream(
            equalBytes, sizeof(equalBytes), true, LVOM_READ);
    LVStreamRef equal =
            LVCreateCompareTestStream(equalLeft, equalRight);
    lUInt8 output[sizeof(equalBytes)] = {};
    lvsize_t bytesRead = 0;
    if (equal.isNull()
            || equal->Read(output, sizeof(output), &bytesRead) != LVERR_OK
            || bytesRead != sizeof(output)
            || std::memcmp(output, equalBytes, sizeof(output)) != 0)
        return fail("compare stream changed matching read output");

    static lUInt8 differentBytes[] = {1, 2, 9, 4, 5, 6};
    LVStreamRef mismatchLeft = LVCreateMemoryStream(
            equalBytes, sizeof(equalBytes), true, LVOM_READ);
    LVStreamRef mismatchRight = LVCreateMemoryStream(
            differentBytes, sizeof(differentBytes), true, LVOM_READ);
    LVStreamRef mismatch =
            LVCreateCompareTestStream(mismatchLeft, mismatchRight);
    bool rejected = false;
    try {
        mismatch->Read(output, sizeof(output), &bytesRead);
    } catch (const std::runtime_error &) {
        rejected = true;
    }
    if (!rejected)
        return fail("compare stream mismatch did not unwind scratch ownership");
    return 0;
}
#endif

class CountingHyphDataLoader : public HyphDataLoader {
private:
    std::atomic<int> &_destroyed;

public:
    explicit CountingHyphDataLoader(std::atomic<int> &destroyed)
        : _destroyed(destroyed) {
    }

    ~CountingHyphDataLoader() override {
        _destroyed.fetch_add(1, std::memory_order_relaxed);
    }

    LVStreamRef loadData(lString32) override {
        return LVStreamRef();
    }
};

class FixtureHyphDataLoader : public HyphDataLoader {
private:
    std::atomic<int> &_loads;

public:
    explicit FixtureHyphDataLoader(std::atomic<int> &loads)
        : _loads(loads) {
    }

    LVStreamRef loadData(lString32) override {
        static const char pattern[] =
                "<?xml version=\"1.0\" encoding=\"utf-8\"?>"
                "<HyphenationDescription title=\"Test\" lang=\"zz\" "
                "lefthyphenmin=\"2\" righthyphenmin=\"2\">"
                "<pattern>a1b</pattern>"
                "<pattern>b1c</pattern>"
                "</HyphenationDescription>";
        _loads.fetch_add(1, std::memory_order_relaxed);
        return LVCreateMemoryStream(
                const_cast<char *>(pattern),
                static_cast<int>(sizeof(pattern) - 1),
                true, LVOM_READ);
    }
};

class HashResizeValue {
    int _value;
    static bool _copyBlocked;
    static int _liveCount;

public:
    explicit HashResizeValue(int value = 0)
        : _value(value) {
        ++_liveCount;
    }

    HashResizeValue(const HashResizeValue &value)
        : _value(value._value) {
        if (_copyBlocked)
            throw std::runtime_error("hash value copy blocked");
        ++_liveCount;
    }

    HashResizeValue &operator=(const HashResizeValue &value) {
        if (_copyBlocked)
            throw std::runtime_error("hash value assignment blocked");
        _value = value._value;
        return *this;
    }

    ~HashResizeValue() {
        --_liveCount;
    }

    int value() const {
        return _value;
    }

    static void setCopyBlocked(bool blocked) {
        _copyBlocked = blocked;
    }

    static int liveCount() {
        return _liveCount;
    }
};

bool HashResizeValue::_copyBlocked = false;
int HashResizeValue::_liveCount = 0;

class ArrayTrackedValue : public LVRefCounter {
    static int _destroyed;

public:
    ~ArrayTrackedValue() {
        ++_destroyed;
    }

    static int destroyed() {
        return _destroyed;
    }

    static void reset() {
        _destroyed = 0;
    }
};

int ArrayTrackedValue::_destroyed = 0;

class RefCacheTestValue {
    int _value;
    static int _liveCount;

public:
    explicit RefCacheTestValue(int value)
        : _value(value) {
        ++_liveCount;
    }

    RefCacheTestValue(const RefCacheTestValue &value)
        : _value(value._value) {
        ++_liveCount;
    }

    ~RefCacheTestValue() {
        --_liveCount;
    }

    bool operator==(const RefCacheTestValue &value) const {
        return _value == value._value;
    }

    int value() const {
        return _value;
    }

    static int liveCount() {
        return _liveCount;
    }
};

int RefCacheTestValue::_liveCount = 0;

typedef LVRef<RefCacheTestValue> RefCacheTestRef;

static lUInt32 calcHash(RefCacheTestRef &value) {
    if (!value.isNull() && value->value() == 99)
        throw std::runtime_error("reference cache hash blocked");
    return 1;
}

class PointerVectorTestValue {
    int _value;
    static int _liveCount;
    static int _copyCount;
    static int _copyBudget;

public:
    explicit PointerVectorTestValue(int value)
        : _value(value) {
        ++_liveCount;
    }

    PointerVectorTestValue(const PointerVectorTestValue &value)
        : _value(value._value) {
        if (_copyBudget == 0)
            throw std::runtime_error("pointer vector copy blocked");
        if (_copyBudget > 0)
            --_copyBudget;
        ++_liveCount;
        ++_copyCount;
    }

    ~PointerVectorTestValue() {
        --_liveCount;
    }

    int value() const {
        return _value;
    }

    static int liveCount() {
        return _liveCount;
    }

    static int copyCount() {
        return _copyCount;
    }

    static void resetCopies() {
        _copyCount = 0;
        _copyBudget = -1;
    }

    static void setCopyBudget(int budget) {
        _copyBudget = budget;
    }
};

int PointerVectorTestValue::_liveCount = 0;
int PointerVectorTestValue::_copyCount = 0;
int PointerVectorTestValue::_copyBudget = -1;

class MatrixTestValue {
    int _value;
    static int _liveCount;
    static int _assignmentBudget;

public:
    explicit MatrixTestValue(int value)
        : _value(value) {
        ++_liveCount;
    }

    MatrixTestValue(const MatrixTestValue &value)
        : _value(value._value) {
        ++_liveCount;
    }

    MatrixTestValue &operator=(const MatrixTestValue &value) {
        if (_assignmentBudget == 0)
            throw std::runtime_error("matrix assignment blocked");
        if (_assignmentBudget > 0)
            --_assignmentBudget;
        _value = value._value;
        return *this;
    }

    ~MatrixTestValue() {
        --_liveCount;
    }

    int value() const {
        return _value;
    }

    static int liveCount() {
        return _liveCount;
    }

    static void setAssignmentBudget(int budget) {
        _assignmentBudget = budget;
    }
};

int MatrixTestValue::_liveCount = 0;
int MatrixTestValue::_assignmentBudget = -1;

class MatrixIntProbe : public LVMatrix<int> {
public:
    int rowCount() const {
        return numrows;
    }

    int columnCount() const {
        return numcols;
    }

    size_t cellCount() const {
        return cells.size();
    }
};

static int pointerVectorValueComparator(
        const PointerVectorTestValue **left,
        const PointerVectorTestValue **right) {
    if (!*left)
        return *right ? 1 : 0;
    if (!*right)
        return -1;
    if ((*left)->value() < (*right)->value())
        return -1;
    if ((*left)->value() > (*right)->value())
        return 1;
    return 0;
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

static int testConcurrentRenderBaseWeight() {
    static const int workerCount = 8;
    static const int iterations = 1000;
    LVRendSetBaseFontWeight(0);
    if (LVRendGetBaseFontWeight() != 1)
        return fail("render base weight lower bound was not enforced");
    LVRendSetBaseFontWeight(1000);
    if (LVRendGetBaseFontWeight() != 999)
        return fail("render base weight upper bound was not enforced");

    std::atomic<bool> failed(false);
    std::vector<std::thread> workers;
    workers.reserve(workerCount);

    for (int workerIndex = 0;
            workerIndex < workerCount; workerIndex++) {
        workers.emplace_back([workerIndex, &failed]() {
            for (int iteration = 0; iteration < iterations; iteration++) {
                int requested = ((workerIndex + iteration) % 3 == 0)
                        ? 0
                        : (((workerIndex + iteration) % 3 == 1)
                                ? 1000
                                : 500);
                LVRendSetBaseFontWeight(requested);
                int observed = LVRendGetBaseFontWeight();
                if (observed < 1 || observed > 999) {
                    failed.store(true, std::memory_order_release);
                    return;
                }
            }
        });
    }
    for (std::thread &worker : workers)
        worker.join();
    LVRendSetBaseFontWeight(400);
    if (LVRendGetBaseFontWeight() != 400)
        return fail("render base weight default was not restored");
    if (failed.load(std::memory_order_acquire))
        return fail("render base weight was published outside its bounds");
    return 0;
}

static int testConcurrentRenderDPISettings() {
    LVRendSetRenderDPI(BASE_CSS_DPI);
    if (LVRendSetRenderDPI(BASE_CSS_DPI))
        return fail("unchanged render DPI was reported as changed");
    if (!LVRendSetRenderDPI(BASE_CSS_DPI * 2)
            || scaleForRenderDPI(BASE_CSS_DPI) != BASE_CSS_DPI * 2)
        return fail("render DPI scaling did not use the configured value");
    LVRendSetRenderDPI(0);
    if (scaleForRenderDPI(BASE_CSS_DPI) != BASE_CSS_DPI)
        return fail("legacy zero render DPI unexpectedly scaled pixels");

    LVRendSetScaleFontWithDPI(false);
    if (LVRendSetScaleFontWithDPI(false))
        return fail("unchanged font DPI scaling was reported as changed");
    if (!LVRendSetScaleFontWithDPI(true)
            || !LVRendGetScaleFontWithDPI())
        return fail("font DPI scaling setting was not published");

    static const int workerCount = 8;
    static const int iterations = 1000;
    std::atomic<bool> failed(false);
    std::vector<std::thread> workers;
    workers.reserve(workerCount);
    for (int workerIndex = 0;
            workerIndex < workerCount; workerIndex++) {
        workers.emplace_back([workerIndex, &failed]() {
            for (int iteration = 0; iteration < iterations; iteration++) {
                const int selector = (workerIndex + iteration) % 3;
                const int dpi = selector == 0
                        ? 0
                        : (selector == 1 ? BASE_CSS_DPI : BASE_CSS_DPI * 2);
                LVRendSetRenderDPI(dpi);
                LVRendSetScaleFontWithDPI(selector == 2);
                const int observedDPI = LVRendGetRenderDPI();
                const int scaled = scaleForRenderDPI(BASE_CSS_DPI);
                if ((observedDPI != 0
                            && observedDPI != BASE_CSS_DPI
                            && observedDPI != BASE_CSS_DPI * 2)
                        || (scaled != BASE_CSS_DPI
                            && scaled != BASE_CSS_DPI * 2)) {
                    failed.store(true, std::memory_order_release);
                    return;
                }
            }
        });
    }
    for (std::thread &worker : workers)
        worker.join();
    LVRendSetRenderDPI(DEF_RENDER_DPI);
    LVRendSetScaleFontWithDPI(DEF_RENDER_SCALE_FONT_WITH_DPI != 0);
    if (failed.load(std::memory_order_acquire))
        return fail("render DPI settings published an invalid value");
    return 0;
}

static int testConcurrentHyphenationSettings() {
    HyphMan::overrideLeftHyphenMin(4);
    HyphMan::overrideRightHyphenMin(5);
    if (HyphMan::overrideLeftHyphenMin(-1)
            || HyphMan::overrideRightHyphenMin(
                    HYPH_MAX_HYPHEN_MIN + 1)
            || HyphMan::getOverriddenLeftHyphenMin() != 4
            || HyphMan::getOverriddenRightHyphenMin() != 5)
        return fail("invalid hyphen minima changed the configured values");

    static const int workerCount = 8;
    static const int iterations = 1000;
    std::atomic<bool> failed(false);
    std::vector<std::thread> workers;
    workers.reserve(workerCount);
    for (int workerIndex = 0;
            workerIndex < workerCount; workerIndex++) {
        workers.emplace_back([workerIndex, &failed]() {
            for (int iteration = 0; iteration < iterations; iteration++) {
                const int value = (workerIndex + iteration)
                        % (HYPH_MAX_HYPHEN_MIN + 1);
                HyphMan::overrideLeftHyphenMin(value);
                HyphMan::overrideRightHyphenMin(
                        HYPH_MAX_HYPHEN_MIN - value);
                HyphMan::setTrustSoftHyphens(value % 2);
                const int left =
                        HyphMan::getOverriddenLeftHyphenMin();
                const int right =
                        HyphMan::getOverriddenRightHyphenMin();
                const int trust = HyphMan::getTrustSoftHyphens();
                if (left < HYPH_MIN_HYPHEN_MIN
                        || left > HYPH_MAX_HYPHEN_MIN
                        || right < HYPH_MIN_HYPHEN_MIN
                        || right > HYPH_MAX_HYPHEN_MIN
                        || (trust != 0 && trust != 1)) {
                    failed.store(true, std::memory_order_release);
                    return;
                }
            }
        });
    }
    for (std::thread &worker : workers)
        worker.join();
    HyphMan::overrideLeftHyphenMin(HYPH_DEFAULT_HYPHEN_MIN);
    HyphMan::overrideRightHyphenMin(HYPH_DEFAULT_HYPHEN_MIN);
    HyphMan::setTrustSoftHyphens(HYPH_DEFAULT_TRUST_SOFT_HYPHENS);
    if (failed.load(std::memory_order_acquire))
        return fail("hyphenation settings published an invalid value");
    return 0;
}

static int testHyphenationRegistryOwnership() {
    std::atomic<int> destroyed(0);
    CountingHyphDataLoader *first =
            new CountingHyphDataLoader(destroyed);
    HyphMan::setDataLoader(first);
    HyphMan::setDataLoader(first);
    if (destroyed.load(std::memory_order_relaxed) != 0)
        return fail("hyphenation loader was destroyed when reinstalled");

    HyphMan::setDataLoader(new CountingHyphDataLoader(destroyed));
    if (destroyed.load(std::memory_order_relaxed) != 1)
        return fail("replaced hyphenation loader was not destroyed");
    HyphMan::setDataLoader(NULL);
    if (destroyed.load(std::memory_order_relaxed) != 2)
        return fail("cleared hyphenation loader was not destroyed");

    const bool initialized = HyphMan::initDictionaries(
            lString32::empty_str, true);
    HyphDictionaryList *list = HyphMan::getDictList();
    const int dictionaryCount = list ? list->length() : 0;
    HyphMan::uninit();
    if (!initialized || dictionaryCount < 3)
        return fail("hyphenation dictionary registry did not initialize");
    if (HyphMan::getDictList() != NULL)
        return fail("hyphenation dictionary registry survived uninit");
    if (HyphMan::activateDictionary(HYPH_DICT_ID_ALGORITHM))
        return fail("uninitialized hyphenation registry activated a method");
    return 0;
}

static int testHyphenationPatternOwnership() {
    if (!LVRunHyphenationPatternOwnershipRegression())
        return fail("hyphenation pattern-chain ownership regression failed");
    return 0;
}

static int testConcurrentHyphenationMethodCache() {
    if (!HyphMan::initDictionaries(lString32::empty_str, true))
        return fail("hyphenation cache fixture registry did not initialize");

    const lString32 dictionaryId(U"test-cache.pattern");
    HyphDictionary *dictionary = new HyphDictionary(
            HDT_DICT_TEX, U"Test cache", dictionaryId, U"zz",
            dictionaryId);
    if (!HyphMan::addDictionaryItem(dictionary)) {
        delete dictionary;
        HyphMan::uninit();
        return fail("hyphenation cache fixture dictionary was rejected");
    }

    std::atomic<int> loads(0);
    HyphMan::setDataLoader(new FixtureHyphDataLoader(loads));
    static const int workerCount = 8;
    std::atomic<int> ready(0);
    std::atomic<bool> start(false);
    std::vector<HyphMethod *> methods(workerCount, NULL);
    std::vector<std::thread> workers;
    workers.reserve(workerCount);
    for (int workerIndex = 0;
            workerIndex < workerCount; workerIndex++) {
        workers.emplace_back([workerIndex, &ready, &start,
                              &methods, &dictionaryId]() {
            ready.fetch_add(1, std::memory_order_release);
            while (!start.load(std::memory_order_acquire))
                std::this_thread::yield();
            methods[workerIndex] =
                    HyphMan::getHyphMethodForDictionary(dictionaryId);
        });
    }
    while (ready.load(std::memory_order_acquire) < workerCount)
        std::this_thread::yield();
    start.store(true, std::memory_order_release);
    for (std::thread &worker : workers)
        worker.join();

    HyphMethod *first = methods[0];
    bool valid = first != NULL && first->getPatternsCount() == 2;
    for (int i = 1; i < workerCount; i++)
        valid = valid && methods[i] == first;
    const int loadCount = loads.load(std::memory_order_relaxed);
    HyphMan::uninit();
    if (!valid)
        return fail("hyphenation method cache returned inconsistent methods");
    if (loadCount != 1)
        return fail("hyphenation method cache loaded one dictionary repeatedly");
    return 0;
}

static int testConcurrentTextLangRuntimeOptions() {
    if (!HyphMan::initDictionaries(lString32::empty_str, true))
        return fail("text-language fixture registry did not initialize");

    TextLangCfg *langCfg = TextLangMan::getTextLangCfg(U"en");
    HyphMethod *defaultMethod = langCfg->getDefaultHyphMethod();
    HyphMethod *noMethod =
            HyphMan::getHyphMethodForDictionary(HYPH_DICT_ID_NONE);
    HyphMethod *softMethod =
            HyphMan::getHyphMethodForDictionary(HYPH_DICT_ID_SOFTHYPHENS);
    HyphMethod *algoMethod =
            HyphMan::getHyphMethodForDictionary(HYPH_DICT_ID_ALGORITHM);
    if (!defaultMethod || !noMethod || !softMethod || !algoMethod) {
        HyphMan::uninit();
        return fail("text-language fixture methods were not initialized");
    }

    TextLangMan::setHyphenationEnabled(true);
    TextLangMan::setHyphenationSoftHyphensOnly(false);
    TextLangMan::setHyphenationForceAlgorithmic(false);
    if (langCfg->getHyphMethod() != defaultMethod) {
        HyphMan::uninit();
        return fail("text-language default hyphenation method was not selected");
    }
    TextLangMan::setHyphenationEnabled(false);
    if (langCfg->getHyphMethod() != noMethod) {
        HyphMan::uninit();
        return fail("text-language disabled hyphenation method was not selected");
    }
    TextLangMan::setHyphenationEnabled(true);
    TextLangMan::setHyphenationSoftHyphensOnly(true);
    if (langCfg->getHyphMethod() != softMethod) {
        HyphMan::uninit();
        return fail("text-language soft-hyphen method was not selected");
    }
    TextLangMan::setHyphenationSoftHyphensOnly(false);
    TextLangMan::setHyphenationForceAlgorithmic(true);
    if (langCfg->getHyphMethod() != algoMethod) {
        HyphMan::uninit();
        return fail("text-language algorithmic method was not selected");
    }

    static const int workerCount = 8;
    static const int iterations = 1000;
    std::atomic<bool> failed(false);
    std::vector<std::thread> workers;
    workers.reserve(workerCount);
    for (int workerIndex = 0;
            workerIndex < workerCount; workerIndex++) {
        workers.emplace_back([workerIndex, langCfg, defaultMethod, noMethod,
                              softMethod, algoMethod, &failed]() {
            for (int iteration = 0; iteration < iterations; iteration++) {
                const int selector = (workerIndex + iteration) % 4;
                TextLangMan::setEmbeddedLangsEnabled((selector & 1) != 0);
                TextLangMan::setHyphenationEnabled(selector != 0);
                TextLangMan::setHyphenationSoftHyphensOnly(selector == 2);
                TextLangMan::setHyphenationForceAlgorithmic(selector == 3);
                HyphMethod *observed = langCfg->getHyphMethod();
                if (observed != defaultMethod && observed != noMethod
                        && observed != softMethod && observed != algoMethod) {
                    failed.store(true, std::memory_order_release);
                    return;
                }
            }
        });
    }
    for (std::thread &worker : workers)
        worker.join();

    TextLangMan::setEmbeddedLangsEnabled(
            TEXTLANG_DEFAULT_EMBEDDED_LANGS_ENABLED);
    TextLangMan::setHyphenationEnabled(
            TEXTLANG_DEFAULT_HYPHENATION_ENABLED);
    TextLangMan::setHyphenationSoftHyphensOnly(
            TEXTLANG_DEFAULT_HYPH_SOFT_HYPHENS_ONLY);
    TextLangMan::setHyphenationForceAlgorithmic(
            TEXTLANG_DEFAULT_HYPH_FORCE_ALGORITHMIC);
    HyphMan::uninit();
    if (failed.load(std::memory_order_acquire))
        return fail("text-language options published an invalid method");
    return 0;
}

static int testConcurrentTextLangConfigCache() {
    if (!HyphMan::initDictionaries(lString32::empty_str, true))
        return fail("text-language cache fixture registry did not initialize");

    TextLangMan::setEmbeddedLangsEnabled(true);
    static const int workerCount = 8;
    std::atomic<int> ready(0);
    std::atomic<bool> start(false);
    const lString32 tags[] = { U"cache-a", U"cache-b" };
    std::vector<TextLangCfg *> configs(workerCount, NULL);
    std::vector<std::thread> workers;
    workers.reserve(workerCount);
    for (int workerIndex = 0;
            workerIndex < workerCount; workerIndex++) {
        workers.emplace_back([workerIndex, &ready, &start, &tags, &configs]() {
            ready.fetch_add(1, std::memory_order_release);
            while (!start.load(std::memory_order_acquire))
                std::this_thread::yield();
            configs[workerIndex] =
                    TextLangMan::getTextLangCfg(tags[workerIndex % 2]);
        });
    }
    while (ready.load(std::memory_order_acquire) < workerCount)
        std::this_thread::yield();
    start.store(true, std::memory_order_release);
    for (std::thread &worker : workers)
        worker.join();

    bool cacheValid = configs[0] && configs[1]
            && configs[0] != configs[1];
    for (int i = 2; i < workerCount; i++)
        cacheValid = cacheValid && configs[i] == configs[i % 2];
    if (!cacheValid) {
        TextLangMan::setEmbeddedLangsEnabled(
                TEXTLANG_DEFAULT_EMBEDDED_LANGS_ENABLED);
        HyphMan::uninit();
        return fail("text-language cache published duplicate configurations");
    }

    std::atomic<bool> failed(false);
    workers.clear();
    for (int workerIndex = 0;
            workerIndex < workerCount; workerIndex++) {
        workers.emplace_back([workerIndex, &failed, &tags]() {
            for (int iteration = 0; iteration < 1000; iteration++) {
                TextLangMan::setMainLang(
                        tags[(workerIndex + iteration) % 2]);
                lString32 observed = TextLangMan::getMainLang();
                if (observed != tags[0] && observed != tags[1]) {
                    failed.store(true, std::memory_order_release);
                    return;
                }
            }
        });
    }
    for (std::thread &worker : workers)
        worker.join();

    TextLangMan::setMainLang(TEXTLANG_DEFAULT_MAIN_LANG_32);
    TextLangMan::setEmbeddedLangsEnabled(
            TEXTLANG_DEFAULT_EMBEDDED_LANGS_ENABLED);
    HyphMan::uninit();
    if (failed.load(std::memory_order_acquire))
        return fail("text-language main language was published partially");
    return 0;
}

static int testFontManagerLifecycleOwnership() {
    ShutdownFontManager();
    if (!InitFontManager(lString8::empty_str) || !fontMan)
        return fail("font manager did not initialize");
    LVFontManager *first = fontMan;
    if (!InitFontManager(lString8::empty_str) || fontMan != first) {
        ShutdownFontManager();
        return fail("font manager initialization replaced a live instance");
    }
    if (!ShutdownFontManager() || fontMan)
        return fail("font manager shutdown did not release ownership");
    if (ShutdownFontManager())
        return fail("font manager shutdown was not idempotent");
    return 0;
}

static int testConcurrentFontGammaSettings() {
    if (!InitFontManager(lString8::empty_str) || !fontMan)
        return fail("font gamma fixture manager did not initialize");

    fontMan->SetGammaIndex(-1);
    if (fontMan->GetGammaIndex() != 0) {
        ShutdownFontManager();
        return fail("font gamma lower bound was not enforced");
    }
    fontMan->SetGammaIndex(100000);
    const int maxGammaIndex = fontMan->GetGammaIndex();
    if (maxGammaIndex <= 0 || maxGammaIndex >= 100000) {
        ShutdownFontManager();
        return fail("font gamma upper bound was not enforced");
    }

    static const int workerCount = 8;
    static const int iterations = 250;
    std::atomic<bool> failed(false);
    std::vector<std::thread> workers;
    workers.reserve(workerCount);
    for (int workerIndex = 0;
            workerIndex < workerCount; workerIndex++) {
        workers.emplace_back([workerIndex, maxGammaIndex, &failed]() {
            for (int iteration = 0; iteration < iterations; iteration++) {
                const int requested =
                        (workerIndex + iteration) % 2 ? 0 : maxGammaIndex;
                fontMan->SetGammaIndex(requested);
                const int observed = fontMan->GetGammaIndex();
                if (observed != 0 && observed != maxGammaIndex) {
                    failed.store(true, std::memory_order_release);
                    return;
                }
            }
        });
    }
    for (std::thread &worker : workers)
        worker.join();

    fontMan->SetGamma(1.0);
    const double restoredGamma = fontMan->GetGamma();
    ShutdownFontManager();
    if (failed.load(std::memory_order_acquire))
        return fail("font gamma index published an invalid value");
    if (restoredGamma <= 0.0)
        return fail("font gamma value was not restored");
    return 0;
}

static bool isValidAntialiasMode(font_antialiasing_t mode) {
    return mode >= font_aa_none && mode <= font_aa_lcd_v_pentile_m;
}

static bool isValidHintingMode(hinting_mode_t mode) {
    return mode >= HINTING_MODE_DISABLED && mode <= HINTING_MODE_AUTOHINT;
}

static bool isValidShapingMode(shaping_mode_t mode) {
    return mode >= SHAPING_MODE_FREETYPE && mode <= SHAPING_MODE_HARFBUZZ;
}

static int testConcurrentFontRenderSettings() {
    if (!InitFontManager(lString8::empty_str) || !fontMan)
        return fail("font render settings fixture manager did not initialize");

    static const int workerCount = 8;
    static const int iterations = 100;
    std::atomic<bool> failed(false);
    std::vector<std::thread> workers;
    workers.reserve(workerCount);
    for (int workerIndex = 0;
            workerIndex < workerCount; workerIndex++) {
        workers.emplace_back([workerIndex, &failed]() {
            for (int iteration = 0; iteration < iterations; iteration++) {
                const bool alternate = (workerIndex + iteration) % 2 != 0;
                fontMan->SetAntialiasMode(
                        alternate ? font_aa_none : font_aa_all);
                fontMan->SetHintingMode(alternate
                        ? HINTING_MODE_DISABLED : HINTING_MODE_AUTOHINT);
                fontMan->SetKerning(alternate);
                fontMan->SetShapingMode(alternate
                        ? SHAPING_MODE_FREETYPE
                        : SHAPING_MODE_HARFBUZZ_LIGHT);

                if (!isValidAntialiasMode(fontMan->GetAntialiasMode())
                        || !isValidHintingMode(fontMan->GetHintingMode())
                        || !isValidShapingMode(fontMan->GetShapingMode())) {
                    failed.store(true, std::memory_order_release);
                    return;
                }
            }
        });
    }
    for (std::thread &worker : workers)
        worker.join();

    fontMan->SetAntialiasMode(font_aa_all);
    fontMan->SetHintingMode(HINTING_MODE_AUTOHINT);
    fontMan->SetKerning(false);
    fontMan->SetShapingMode(SHAPING_MODE_FREETYPE);
    ShutdownFontManager();
    if (failed.load(std::memory_order_acquire))
        return fail("font render settings published an invalid value");
    return 0;
}

static LVFontGlyphCacheItemOwner newGlyphCacheTestItem(
        LVFontLocalGlyphCache *localCache, lUInt32 key) {
    static const int width = 4;
    static const int height = 4;
    static const int pitch = 4;
    return LVFontGlyphCacheItem::newItem(
            localCache, key, width, height, pitch, pitch * height);
}

static int testBoundedObservableGlyphCache() {
    LVFontGlyphCacheItemOwner sizeProbe =
            newGlyphCacheTestItem(NULL, 0);
    if (!sizeProbe)
        return fail("glyph cache size probe allocation failed");
    const int itemSize = sizeProbe->getSize();
    sizeProbe.reset();

    LVFontGlyphCacheItemOwner accountingProbe =
            LVFontGlyphCacheItem::newItem(
                    NULL, 10, 4, 1, -4, 12);
    if (!accountingProbe
            || accountingProbe->bmp_pitch != -4
            || accountingProbe->getSize()
                    != static_cast<int>(
                            offsetof(LVFontGlyphCacheItem, bmp) + 12))
        return fail("glyph cache did not retain exact bitmap byte accounting");

    LVFontGlobalGlyphCache globalCache(itemSize * 2);
    LVFontLocalGlyphCache localCache(&globalCache);
    if (localCache.get(1) != NULL)
        return fail("empty glyph cache unexpectedly returned an item");

    LVFontGlyphCacheItemOwner firstOwner =
            newGlyphCacheTestItem(&localCache, 1);
    LVFontGlyphCacheItemOwner secondOwner =
            newGlyphCacheTestItem(&localCache, 2);
    LVFontGlyphCacheItemOwner thirdOwner =
            newGlyphCacheTestItem(&localCache, 3);
    if (!firstOwner || !secondOwner || !thirdOwner)
        return fail("glyph cache fixture allocation failed");
    LVFontGlyphCacheItem *first = firstOwner.get();
    LVFontGlyphCacheItem *third = thirdOwner.get();
    localCache.put(std::move(firstOwner));
    localCache.put(std::move(secondOwner));
    if (localCache.get(1) != first)
        return fail("glyph cache did not return the first item");
    localCache.put(std::move(thirdOwner));

    if (localCache.get(1) != first)
        return fail("recently used glyph was evicted");
    if (localCache.get(2) != NULL)
        return fail("least recently used glyph was not evicted");
    if (localCache.get(3) != third)
        return fail("new glyph was not cached");

    LVCacheStats stats = globalCache.getStats();
    if (stats.capacity != itemSize * 2 || stats.size != itemSize * 2)
        return fail("glyph cache byte bounds are incorrect");
    if (stats.hits != 3 || stats.misses != 2 || stats.evictions != 1)
        return fail("glyph cache counters are incorrect");

    localCache.clear();
    stats = globalCache.getStats();
    if (stats.size != 0)
        return fail("glyph cache retained size after removing the last item");
    globalCache.resetStats();
    stats = globalCache.getStats();
    if (stats.hits != 0 || stats.misses != 0 || stats.evictions != 0)
        return fail("glyph cache counters did not reset");

    {
        LVFontGlyphCacheItemOwner rolledBack =
                newGlyphCacheTestItem(&localCache, 4);
        if (!rolledBack)
            return fail("glyph cache rollback candidate allocation failed");
        std::memset(rolledBack->bmp, 0xA5, 16);
    }
    LVFontGlyphCacheItemOwner reusedOwner =
            newGlyphCacheTestItem(&localCache, 5);
    if (!reusedOwner)
        return fail("glyph cache reuse candidate allocation failed");
    LVFontGlyphCacheItem *reused = reusedOwner.get();
    localCache.put(std::move(reusedOwner));
    if (localCache.get(5) != reused)
        return fail("glyph cache could not publish after repeated clear");
    localCache.clear();
    localCache.clear();
    if (globalCache.getStats().size != 0)
        return fail("glyph cache owner graph survived repeated clear");
    return 0;
}

static int testFontCacheOwnership() {
    LVFontCache cache;
    LVFontDef documentRegular(
            lString8("document-regular.ttf"), -1, 400, 0, 0,
            css_ff_sans_serif, lString8("Owned Face"), 0, 77);
    LVFontDef documentBold(
            lString8("document-bold.ttf"), -1, 700, 0, 0,
            css_ff_sans_serif, lString8("Owned Face"), 0, 77);
    LVFontDef otherDocument(
            lString8("other-document.ttf"), -1, 500, 0, 0,
            css_ff_sans_serif, lString8("Owned Face"), 0, 88);
    LVFontDef systemFont(
            lString8("system.ttf"), -1, 500, 0, 0,
            css_ff_sans_serif, lString8("System Face"));

    cache.update(&documentRegular, LVFontRef());
    cache.update(&documentBold, LVFontRef());
    cache.update(&otherDocument, LVFontRef());
    cache.update(&systemFont, LVFontRef());
    if (cache.length() != 4
            || !cache.getInstances().empty()
            || !cache.findDocumentFontDuplicate(
                    77, lString8("document-regular.ttf")))
        return fail("font cache did not publish owned registrations");

    cache.removeDocumentFonts(77);
    if (cache.length() != 2
            || cache.findDocumentFontDuplicate(
                    77, lString8("document-regular.ttf"))
            || !cache.findDocumentFontDuplicate(
                    88, lString8("other-document.ttf"))) {
        return fail(
                "font cache document removal retained owned entries");
    }
    cache.removeDocumentFonts(-1);
    if (cache.length() != 2)
        return fail("font cache removed process-wide registrations");

    cache.update(&documentRegular, LVFontRef());
    cache.update(&documentBold, LVFontRef());
    if (cache.length() != 4)
        return fail("font cache did not accept registrations after removal");
    cache.removefont(&documentRegular);
    if (cache.length() != 1
            || !cache.findDuplicate(&systemFont)
            || cache.findDocumentFontDuplicate(
                    88, lString8("other-document.ttf"))) {
        return fail(
                "font cache typeface removal skipped adjacent owners");
    }

    lString32Collection faces;
    cache.getFaceList(faces);
    lString32Collection files;
    cache.getFontFileNameList(files);
    LVArray<int> weights;
    cache.getAvailableFontWeights(weights, lString8("System Face"));
    if (faces.length() != 1 || faces[0] != U"System Face"
            || files.length() != 1 || files[0] != U"system.ttf"
            || weights.length() != 1 || weights[0] != 500)
        return fail("font cache views lost their surviving owner");

    cache.clear();
    if (cache.length() != 0 || !cache.getInstances().empty())
        return fail("font cache clear retained owned entries");
    return 0;
}

#if (USE_FREETYPE==1)
static int testFreeTypeMetricCacheOwnership() {
    LVFontGlyphUnsignedMetricCache cache;
    if (cache.get(0) != CACHED_UNSIGNED_METRIC_NOT_SET
            || cache.get(0x2CFFF) != CACHED_UNSIGNED_METRIC_NOT_SET)
        return fail("FreeType metric cache did not start empty");

    cache.put(0, 123);
    cache.put(0x1FF, 456);
    cache.put(0x200, 789);
    cache.put(0x2CFFF, 321);
    if (cache.get(0) != 123
            || cache.get(0x1FF) != 456
            || cache.get(0x200) != 789
            || cache.get(0x2CFFF) != 321)
        return fail("FreeType metric cache lost a page boundary value");

    cache.put(0x40000, 999);
    cache.put(0x2D000, 888);
    if (cache.get(0) != 123
            || cache.get(0x40000) != CACHED_UNSIGNED_METRIC_NOT_SET
            || cache.get(0x2D000) != CACHED_UNSIGNED_METRIC_NOT_SET)
        return fail("FreeType metric cache aliased an unsupported codepoint");

    cache.clear();
    cache.clear();
    if (cache.get(0) != CACHED_UNSIGNED_METRIC_NOT_SET
            || cache.get(0x200) != CACHED_UNSIGNED_METRIC_NOT_SET
            || cache.get(0x2CFFF) != CACHED_UNSIGNED_METRIC_NOT_SET)
        return fail("FreeType metric cache did not release its lazy pages");

    LVFontGlyphSignedMetricCache signedCache;
    signedCache.put(0x41, -321);
    signedCache.put(0x2CFFF, 1234);
    if (signedCache.get(0x41) != -321
            || signedCache.get(0x2CFFF) != 1234
            || signedCache.get(0x40000)
                    != CACHED_SIGNED_METRIC_NOT_SET)
        return fail("FreeType signed metric cache changed its encoding");
    return 0;
}

static int testFreeTypeColorGlyphScaleOwnership() {
    if (!LVRunFreeTypeColorGlyphScaleOwnershipRegression())
        return fail("FreeType color glyph scale ownership regression failed");
    return 0;
}
#endif

static int testBitmapFontFileOwnership() {
    char path[] = "/tmp/coolreader-bitmap-font-XXXXXX";
    int fd = mkstemp(path);
    if (fd < 0)
        return fail("bitmap font owner fixture could not create a file");

    lvfont_header_t header = {};
    std::memcpy(header.magic, "LFNT", 4);
    std::memcpy(header.version, "1.00", 4);
    header.fileSize = static_cast<lUInt32>(sizeof(header));
    std::vector<lUInt8> bytes(sizeof(header));
    std::memcpy(bytes.data(), &header, sizeof(header));
    if (write(fd, bytes.data(), bytes.size())
            != static_cast<ssize_t>(bytes.size())) {
        close(fd);
        unlink(path);
        return fail("bitmap font owner fixture could not write its file");
    }
    close(fd);

    for (int lifecycle = 0; lifecycle < 2; lifecycle++) {
        lvfont_handle font = NULL;
        if (lvfontOpen(path, &font) != 1 || font == NULL
                || lvfontGetHeader(font)->fileSize != bytes.size()) {
            if (font != NULL)
                lvfontClose(font);
            unlink(path);
            return fail("bitmap font candidate was not published");
        }
        lvfontClose(font);
    }

    fd = open(path, O_WRONLY);
    const char corruptMagic = 'X';
    if (fd < 0 || write(fd, &corruptMagic, 1) != 1) {
        if (fd >= 0)
            close(fd);
        unlink(path);
        return fail("bitmap font owner fixture could not corrupt its file");
    }
    close(fd);

    lvfont_handle rejected =
            reinterpret_cast<lvfont_handle>(static_cast<uintptr_t>(1));
    const int accepted = lvfontOpen(path, &rejected);
    unlink(path);
    if (accepted != 0 || rejected != NULL)
        return fail("invalid bitmap font candidate was published");
    return 0;
}

static int testWin32GlyphCacheOwnership() {
    static_assert(
            !std::is_copy_constructible<GlyphCache>::value,
            "Win32 glyph cache ownership must not be copied");
    GlyphCache cache(1);
    glyph_t *first = cache.get(U'a');
    glyph_t *second = cache.get(U'b');
    glyph_t *third = cache.get(U'c');
    if (first == NULL || second == NULL || third == NULL
            || cache.find(U'a') != first
            || cache.find(U'b') != second
            || cache.find(U'c') != third)
        return fail("Win32 glyph cache did not publish three owners");
    first->glyph.assign(8, 17);
    second->glyph.assign(4, 23);

    glyph_t *fourth = cache.get(U'd');
    if (fourth == NULL
            || cache.find(U'd') != fourth
            || cache.find(U'a') != first
            || cache.find(U'b') != second
            || cache.find(U'c') != NULL
            || first->glyph.size() != 8 || first->glyph[0] != 17
            || second->glyph.size() != 4 || second->glyph[0] != 23) {
        return fail("Win32 glyph cache eviction changed surviving owners");
    }

    cache.clear();
    cache.clear();
    if (cache.find(U'a') != NULL
            || cache.find(U'b') != NULL
            || cache.find(U'd') != NULL)
        return fail("Win32 glyph cache did not release its owner graph");
    glyph_t *replacement = cache.get(U'z');
    if (replacement == NULL || cache.find(U'z') != replacement)
        return fail("Win32 glyph cache could not publish after clear");
    return 0;
}

static int testBoundedObservableDecodedImageCache() {
    LVCacheMap<int, int> cache(2);
    int value = 0;
    if (cache.get(9, value))
        return fail("empty bounded cache unexpectedly returned an item");
    cache.set(1, 10);
    cache.set(2, 20);
    if (!cache.get(1, value) || value != 10)
        return fail("bounded cache did not return the first item");
    cache.set(3, 30);
    if (cache.get(2, value))
        return fail("bounded cache did not evict its least-recently-used item");
    if (!cache.get(3, value) || value != 30)
        return fail("bounded cache did not retain the new item");
    LVCacheStats stats = cache.getStats();
    if (stats.capacityItems != 2 || stats.itemCount != 2
            || stats.hits != 2 || stats.misses != 2
            || stats.evictions != 1)
        return fail("bounded cache counters are incorrect");
    cache.reduceSize(-1);
    cache.set(4, 40);
    stats = cache.getStats();
    if (stats.capacityItems != 0 || stats.itemCount != 0)
        return fail("bounded cache accepted an invalid reduced capacity");
    cache.restoreSize();
    cache.set(4, 40);
    if (!cache.get(4, value) || value != 40)
        return fail("bounded cache failed after restoring vector storage");

    CRSkinRef skin = LVOpenSimpleSkin(
            lString8("<?xml version=\"1.0\"?><CR3Skin/>"));
    if (skin.isNull())
        return fail("decoded image cache skin fixture did not open");
    skin->resetImageCacheStats();
    LVImageSourceRef first =
            skin->getImage(U"std_menu_item_background.xpm");
    if (first.isNull())
        return fail("decoded image cache fixture did not decode");
    if (skin->getImage(U"std_menu_item_background.xpm").isNull())
        return fail("decoded image cache did not return a cached image");
    if (!skin->getImage(U"missing-image.png").isNull())
        return fail("decoded image cache found a missing image");
    stats = skin->getImageCacheStats();
    if (stats.capacityItems != 8 || stats.itemCount != 2
            || stats.hits != 1 || stats.misses != 2
            || stats.evictions != 0)
        return fail("decoded image cache counters are incorrect");
    skin->gc();
    stats = skin->getImageCacheStats();
    if (stats.itemCount != 0 || stats.hits != 1 || stats.misses != 2)
        return fail("decoded image cache clear changed its counters");
    skin->resetImageCacheStats();
    stats = skin->getImageCacheStats();
    if (stats.hits != 0 || stats.misses != 0 || stats.evictions != 0)
        return fail("decoded image cache counters did not reset");
    return 0;
}

static int testSkinOwnership() {
    const std::string validXml =
            "<?xml version=\"1.0\" encoding=\"utf-8\"?>"
            "<CR3Skin>"
            "<menu id=\"owned-menu\" min-item-count=\"2\""
            " max-item-count=\"4\" show-shortcuts=\"true\">"
            "<background color=\"#112233\"/>"
            "<item><background color=\"#445566\"/></item>"
            "</menu>"
            "<toolbar id=\"owned-toolbar\">"
            "<background color=\"#010203\"/>"
            "<button><background color=\"#778899\"/></button>"
            "<button><background color=\"#aabbcc\"/></button>"
            "</toolbar>"
            "</CR3Skin>";
    CRSkinRef skin = LVOpenSimpleSkin(lString8(validXml.c_str()));
    if (skin.isNull())
        return fail("skin ownership fixture did not open");

    CRMenuSkinRef menu = skin->getMenuSkin(U"#owned-menu");
    CRToolBarSkinRef toolbar =
            skin->getToolBarSkin(U"#owned-toolbar");
    CRButtonListRef buttons =
            toolbar.isNull() ? CRButtonListRef() : toolbar->getButtons();
    if (menu.isNull()
            || menu->getMinItemCount() != 2
            || menu->getMaxItemCount() != 4
            || !menu->getShowShortcuts()
            || menu->getBackgroundColor() != 0x112233
            || menu->getItemSkin().isNull()
            || menu->getItemSkin()->getBackgroundColor() != 0x445566
            || buttons.isNull()
            || buttons->length() != 2
            || buttons->get(0).isNull()
            || buttons->get(0)->getBackgroundColor() != 0x778899
            || buttons->get(1).isNull()
            || buttons->get(1)->getBackgroundColor() != 0xaabbcc)
        return fail("skin intrusive owners did not publish parsed candidates");

    std::string rejectedXml =
            "<?xml version=\"1.0\" encoding=\"utf-8\"?>"
            "<CR3Skin><menu id=\"rejected\">"
            "<background color=\"#abcdef\"/></menu>";
    for (unsigned i = 0;
            i <= ParseBudgetLimits::defaults().maxXmlDepth; ++i)
        rejectedXml += "<node>";
    for (unsigned i = 0;
            i <= ParseBudgetLimits::defaults().maxXmlDepth; ++i)
        rejectedXml += "</node>";
    rejectedXml += "</CR3Skin>";
    if (!LVOpenSimpleSkin(lString8(rejectedXml.c_str())).isNull())
        return fail("skin DOM survived a rejected parse");

    CRSkinRef repeated =
            LVOpenSimpleSkin(lString8(validXml.c_str()));
    CRToolBarSkinRef repeatedToolbar = repeated.isNull()
            ? CRToolBarSkinRef()
            : repeated->getToolBarSkin(U"#owned-toolbar");
    CRButtonListRef repeatedButtons = repeatedToolbar.isNull()
            ? CRButtonListRef()
            : repeatedToolbar->getButtons();
    if (repeated.isNull()
            || repeatedToolbar.isNull()
            || repeatedButtons.isNull()
            || repeatedButtons->length() != 2)
        return fail("rejected skin parse retained shared ownership state");

    char rootTemplate[] = "/tmp/coolreader-skin-test-XXXXXX";
    char *root = mkdtemp(rootTemplate);
    if (!root)
        return fail("skin container fixture could not create its root");
    const std::string rootPath(root);
    const std::string skinPath = rootPath + "/owned-skin";
    const std::string xmlPath = skinPath + "/cr3skin.xml";
    auto cleanup = [&]() {
        unlink(xmlPath.c_str());
        rmdir(skinPath.c_str());
        rmdir(rootPath.c_str());
    };
    if (mkdir(skinPath.c_str(), 0700) != 0) {
        cleanup();
        return fail("skin container fixture could not create its directory");
    }
    int xmlFd = open(xmlPath.c_str(), O_WRONLY | O_CREAT | O_TRUNC, 0600);
    if (xmlFd < 0
            || write(xmlFd, validXml.data(), validXml.size())
                    != static_cast<ssize_t>(validXml.size())) {
        if (xmlFd >= 0)
            close(xmlFd);
        cleanup();
        return fail("skin container fixture XML setup failed");
    }
    close(xmlFd);

    const lString32 baseDir = Utf8ToUnicode(
            lString8((rootPath + "/").c_str()));
    const lString32 skinName(U"owned-skin");
    const char *containerError = NULL;
    {
        CRSkinRef containerSkin = LVOpenSkin(
                Utf8ToUnicode(lString8(skinPath.c_str())));
        std::unique_ptr<CRSkinListItem> listItem(
                CRSkinListItem::init(baseDir, skinName));
        CRSkinRef listedSkin = listItem
                ? listItem->getSkin()
                : CRSkinRef();
        CRToolBarSkinRef listedToolbar = listedSkin.isNull()
                ? CRToolBarSkinRef()
                : listedSkin->getToolBarSkin(U"#owned-toolbar");
        CRButtonListRef listedButtons = listedToolbar.isNull()
                ? CRButtonListRef()
                : listedToolbar->getButtons();
        if (containerSkin.isNull()
                || listItem.get() == NULL
                || listedSkin.isNull()
                || listedToolbar.isNull()
                || listedButtons.isNull()
                || listedButtons->length() != 2)
            containerError =
                    "skin container/list owners did not publish valid state";
    }
    cleanup();
    if (containerError)
        return fail(containerError);
    return 0;
}

class CountingGuiScreen : public CRGUIScreen {
private:
    std::atomic<int> &_destroyed;

public:
    explicit CountingGuiScreen(std::atomic<int> &destroyed)
        : _destroyed(destroyed) {
    }

    ~CountingGuiScreen() override {
        _destroyed.fetch_add(1, std::memory_order_relaxed);
    }

    void setFullUpdateInterval(int) override {
    }

    LVRef<LVDrawBuf> createCanvas(int, int) override {
        return LVRef<LVDrawBuf>();
    }

    bool setSize(int, int) override {
        return false;
    }

    int getWidth() override {
        return 0;
    }

    int getHeight() override {
        return 0;
    }

    LVRef<LVDrawBuf> getCanvas() override {
        return LVRef<LVDrawBuf>();
    }

    void draw(LVDrawBuf *, int, int) override {
    }

    void flush(bool) override {
    }
};

class TransactionalGuiResizeScreen : public CRGUIScreenBase {
private:
    int _factoryCall;
    int _nullOnCall;
    int _throwOnCall;

protected:
    void update(const lvRect &, bool) override {
    }

public:
    TransactionalGuiResizeScreen()
        : CRGUIScreenBase(20, 30, true),
          _factoryCall(0),
          _nullOnCall(0),
          _throwOnCall(0) {
    }

    LVRef<LVDrawBuf> createCanvas(int width, int height) override {
        ++_factoryCall;
        if (_factoryCall == _throwOnCall)
            throw std::runtime_error(
                    "synthetic canvas allocation failure");
        if (_factoryCall == _nullOnCall)
            return LVRef<LVDrawBuf>();
        return LVRef<LVDrawBuf>(
                new LVColorDrawBuf(width, height));
    }

    void failWithNullOnCall(int call) {
        _factoryCall = 0;
        _nullOnCall = call;
        _throwOnCall = 0;
    }

    void throwOnCall(int call) {
        _factoryCall = 0;
        _nullOnCall = 0;
        _throwOnCall = call;
    }

    void allowResize() {
        _factoryCall = 0;
        _nullOnCall = 0;
        _throwOnCall = 0;
    }

    LVRef<LVDrawBuf> getFrontCanvas() {
        return _front;
    }
};

class GuiScreenOwnershipWindowManager : public CRGUIWindowManager {
public:
    explicit GuiScreenOwnershipWindowManager(CRGUIScreen *screen)
        : CRGUIWindowManager(screen) {
    }

    void ownScreen(std::unique_ptr<CRGUIScreen> screen) {
        setOwnedScreen(std::move(screen));
    }

    void borrowScreen(CRGUIScreen *screen) {
        setBorrowedScreen(screen);
    }
};

static int testGuiScreenOwnership() {
    std::atomic<int> destroyed(0);
    {
        std::unique_ptr<CountingGuiScreen> external(
                new CountingGuiScreen(destroyed));
        {
            GuiScreenOwnershipWindowManager manager(external.get());
            if (manager.getScreen() != external.get())
                return fail("GUI manager did not publish its borrowed screen");
        }
        if (destroyed.load(std::memory_order_relaxed) != 0)
            return fail("GUI manager destroyed its borrowed screen");
        external.reset();
    }
    if (destroyed.load(std::memory_order_relaxed) != 1)
        return fail("borrowed GUI screen owner did not retain teardown");

    std::unique_ptr<CountingGuiScreen> external(
            new CountingGuiScreen(destroyed));
    {
        GuiScreenOwnershipWindowManager manager(NULL);
        CountingGuiScreen *first = new CountingGuiScreen(destroyed);
        manager.ownScreen(std::unique_ptr<CRGUIScreen>(first));
        if (manager.getScreen() != first)
            return fail("GUI manager did not publish its owned screen");

        CountingGuiScreen *second = new CountingGuiScreen(destroyed);
        manager.ownScreen(std::unique_ptr<CRGUIScreen>(second));
        if (manager.getScreen() != second
                || destroyed.load(std::memory_order_relaxed) != 2)
            return fail("GUI manager did not replace its owned screen");

        manager.borrowScreen(external.get());
        if (manager.getScreen() != external.get()
                || destroyed.load(std::memory_order_relaxed) != 3)
            return fail("GUI manager did not release ownership before borrowing");
    }
    if (destroyed.load(std::memory_order_relaxed) != 3)
        return fail("GUI manager destroyed a replacement borrowed screen");
    external.reset();
    if (destroyed.load(std::memory_order_relaxed) != 4)
        return fail("replacement borrowed screen owner did not retain teardown");

    {
        GuiScreenOwnershipWindowManager manager(NULL);
        manager.ownScreen(std::unique_ptr<CRGUIScreen>(
                new CountingGuiScreen(destroyed)));
    }
    if (destroyed.load(std::memory_order_relaxed) != 5)
        return fail("GUI manager did not destroy its final owned screen");

    TransactionalGuiResizeScreen resizeScreen;
    LVRef<LVDrawBuf> originalCanvas =
            resizeScreen.getCanvas();
    LVRef<LVDrawBuf> originalFront =
            resizeScreen.getFrontCanvas();
    if (resizeScreen.getWidth() != 20
            || resizeScreen.getHeight() != 30
            || originalCanvas.isNull()
            || originalFront.isNull())
        return fail("GUI resize fixture did not start complete");

    resizeScreen.failWithNullOnCall(2);
    if (resizeScreen.setSize(40, 50)
            || resizeScreen.getWidth() != 20
            || resizeScreen.getHeight() != 30
            || resizeScreen.getCanvas().get()
                    != originalCanvas.get()
            || resizeScreen.getFrontCanvas().get()
                    != originalFront.get())
        return fail("GUI resize published a partial null-buffer generation");

    resizeScreen.throwOnCall(2);
    bool resizeThrew = false;
    try {
        resizeScreen.setSize(60, 70);
    } catch (const std::runtime_error &) {
        resizeThrew = true;
    }
    if (!resizeThrew
            || resizeScreen.getWidth() != 20
            || resizeScreen.getHeight() != 30
            || resizeScreen.getCanvas().get()
                    != originalCanvas.get()
            || resizeScreen.getFrontCanvas().get()
                    != originalFront.get())
        return fail("GUI resize published state before a thrown allocation");

    resizeScreen.allowResize();
    if (!resizeScreen.setSize(80, 90)
            || resizeScreen.getWidth() != 80
            || resizeScreen.getHeight() != 90
            || resizeScreen.getCanvas().isNull()
            || resizeScreen.getFrontCanvas().isNull()
            || resizeScreen.getCanvas().get()
                    == originalCanvas.get()
            || resizeScreen.getFrontCanvas().get()
                    == originalFront.get())
        return fail("GUI resize did not publish one complete generation");
    return 0;
}

class CountingGuiWindow : public CRGUIWindowBase {
private:
    std::atomic<int> &_destroyed;
    std::atomic<int> &_closed;

public:
    CountingGuiWindow(CRGUIWindowManager *manager,
                      std::atomic<int> &destroyed,
                      std::atomic<int> &closed)
        : CRGUIWindowBase(manager),
          _destroyed(destroyed),
          _closed(closed) {
    }

    ~CountingGuiWindow() override {
        _destroyed.fetch_add(1, std::memory_order_relaxed);
    }

    void closing() override {
        _closed.fetch_add(1, std::memory_order_relaxed);
    }
};

class CountingGuiEvent : public CRGUIEvent {
private:
    std::atomic<int> &_destroyed;

public:
    CountingGuiEvent(int type, std::atomic<int> &destroyed)
        : CRGUIEvent(type), _destroyed(destroyed) {
    }

    ~CountingGuiEvent() override {
        _destroyed.fetch_add(1, std::memory_order_relaxed);
    }
};

class AcceleratorGuiWindow : public CRGUIWindowBase {
private:
    int _commandCount;
    int _lastCommand;
    int _lastParam;

public:
    explicit AcceleratorGuiWindow(CRGUIWindowManager *manager)
        : CRGUIWindowBase(manager),
          _commandCount(0),
          _lastCommand(0),
          _lastParam(0) {
    }

    bool onCommand(int command, int params) override {
        ++_commandCount;
        _lastCommand = command;
        _lastParam = params;
        return true;
    }

    int commandCount() const { return _commandCount; }
    int lastCommand() const { return _lastCommand; }
    int lastParam() const { return _lastParam; }
};

class CountingMenuItem : public CRMenuItem {
private:
    std::atomic<int> &_destroyed;

public:
    CountingMenuItem(CRMenu *menu, int id,
                     std::atomic<int> &destroyed)
        : CRMenuItem(
                menu, id, U"counted", LVImageSourceRef(), LVFontRef()),
          _destroyed(destroyed) {
    }

    ~CountingMenuItem() override {
        _destroyed.fetch_add(1, std::memory_order_relaxed);
    }
};

class CountingMenu : public CRMenu {
private:
    std::atomic<int> &_destroyed;

public:
    CountingMenu(CRGUIWindowManager *manager, CRMenu *parent, int id,
                 std::atomic<int> &destroyed)
        : CRMenu(
                manager, parent, id, U"counted",
                LVImageSourceRef(), LVFontRef(), LVFontRef()),
          _destroyed(destroyed) {
    }

    ~CountingMenu() override {
        _destroyed.fetch_add(1, std::memory_order_relaxed);
    }
};

class CountingGuiDocView : public LVDocView {
private:
    std::atomic<int> &_destroyed;

public:
    explicit CountingGuiDocView(std::atomic<int> &destroyed)
        : LVDocView(8, true), _destroyed(destroyed) {
    }

    ~CountingGuiDocView() override {
        _destroyed.fetch_add(1, std::memory_order_relaxed);
    }
};

class GuiDocViewOwnershipWindow : public CRDocViewWindow {
public:
    GuiDocViewOwnershipWindow(CRGUIWindowManager *manager,
                              std::atomic<int> &destroyed)
        : CRDocViewWindow(
                manager,
                std::unique_ptr<LVDocView>(
                        new CountingGuiDocView(destroyed))) {
    }
};

static int testGuiRuntimeOwnership() {
    std::atomic<int> screenDestroyed(0);
    std::unique_ptr<CountingGuiScreen> screen(
            new CountingGuiScreen(screenDestroyed));

    std::atomic<int> windowDestroyed(0);
    std::atomic<int> windowClosed(0);
    {
        GuiScreenOwnershipWindowManager manager(screen.get());
        std::unique_ptr<CountingGuiWindow> firstOwner =
                std::make_unique<CountingGuiWindow>(
                        &manager, windowDestroyed, windowClosed);
        CountingGuiWindow *first = firstOwner.get();
        CountingGuiWindow *second = new CountingGuiWindow(
                &manager, windowDestroyed, windowClosed);
        manager.activateWindow(std::move(firstOwner));
        manager.activateWindow(second);
        manager.activateWindow(first);
        if (manager.getWindowCount() != 2
                || manager.getTopVisibleWindow() != first
                || windowDestroyed.load(std::memory_order_relaxed) != 0)
            return fail("GUI window activation duplicated or lost an owner");

        manager.closeWindow(second);
        if (manager.getWindowCount() != 1
                || windowDestroyed.load(std::memory_order_relaxed) != 1
                || windowClosed.load(std::memory_order_relaxed) != 1)
            return fail("GUI window close did not release one owner");

        manager.closeWindow(new CountingGuiWindow(
                &manager, windowDestroyed, windowClosed));
        if (manager.getWindowCount() != 1
                || windowDestroyed.load(std::memory_order_relaxed) != 2
                || windowClosed.load(std::memory_order_relaxed) != 2)
            return fail("GUI unmanaged window adoption did not remain scoped");
    }
    if (windowDestroyed.load(std::memory_order_relaxed) != 3
            || windowClosed.load(std::memory_order_relaxed) != 3)
        return fail("GUI manager teardown leaked its final window");

    std::atomic<int> eventDestroyed(0);
    {
        GuiScreenOwnershipWindowManager manager(screen.get());
        CountingGuiEvent *fullUpdate =
                new CountingGuiEvent(CREV_UPDATE, eventDestroyed);
        fullUpdate->setParam1(1);
        manager.postEvent(fullUpdate);
        manager.postEvent(new CountingGuiEvent(
                CREV_UPDATE, eventDestroyed));
        if (eventDestroyed.load(std::memory_order_relaxed) != 1)
            return fail("GUI event deduplication retained the replaced owner");

        manager.postEvent(new CountingGuiEvent(
                CREV_COMMAND, eventDestroyed));
        if (!manager.peekEvent()
                || manager.peekEvent()->getType() != CREV_COMMAND)
            return fail("GUI event owner queue changed priority order");

        std::unique_ptr<CRGUIEvent> transferred(manager.getEvent());
        if (!transferred || transferred->getType() != CREV_COMMAND
                || eventDestroyed.load(std::memory_order_relaxed) != 1)
            return fail("GUI legacy event boundary did not transfer ownership");
        transferred.reset();
        if (eventDestroyed.load(std::memory_order_relaxed) != 2)
            return fail("GUI transferred event did not release exactly once");
        if (!manager.peekEvent()
                || manager.peekEvent()->getType() != CREV_UPDATE
                || manager.peekEvent()->getParam1() != 1)
            return fail("GUI event deduplication lost a full-update request");

        if (!manager.processPostedEvents()
                || eventDestroyed.load(std::memory_order_relaxed) != 3)
            return fail("GUI event dispatch did not release its scoped owner");
        manager.postEvent(new CountingGuiEvent(
                CREV_COMMAND, eventDestroyed));
    }
    if (eventDestroyed.load(std::memory_order_relaxed) != 4)
        return fail("GUI manager teardown leaked a queued event");

    std::atomic<int> menuItemDestroyed(0);
    bool menuBuildFailed = false;
    try {
        GuiScreenOwnershipWindowManager manager(screen.get());
        std::unique_ptr<CRMenu> menu(new CRMenu(
                &manager, NULL, 900, U"owner",
                LVImageSourceRef(), LVFontRef(), LVFontRef()));
        menu->addItem(std::unique_ptr<CRMenuItem>(
                new CountingMenuItem(
                        menu.get(), 1, menuItemDestroyed)));
        menu->addItem(std::unique_ptr<CRMenuItem>(
                new CountingMenuItem(
                        menu.get(), 2, menuItemDestroyed)));
        throw std::runtime_error("reject menu");
    } catch (const std::runtime_error &) {
        menuBuildFailed = true;
    }
    if (!menuBuildFailed
            || menuItemDestroyed.load(std::memory_order_relaxed) != 2)
        return fail("GUI menu build rollback leaked scoped items");

    std::atomic<int> nestedMenuDestroyed(0);
    {
        GuiScreenOwnershipWindowManager manager(screen.get());
        std::unique_ptr<CountingMenu> root =
                std::make_unique<CountingMenu>(
                        &manager, nullptr, 910, nestedMenuDestroyed);
        std::unique_ptr<CountingMenu> shownChild =
                std::make_unique<CountingMenu>(
                        &manager, root.get(), 911, nestedMenuDestroyed);
        CountingMenu *shownChildView = shownChild.get();
        root->addItem(std::move(shownChild));
        root->addItem(std::make_unique<CountingMenu>(
                &manager, root.get(), 912, nestedMenuDestroyed));
        CountingMenu *rootView = root.get();
        manager.activateWindow(std::move(root));
        manager.activateBorrowedWindow(shownChildView);
        rootView->destroyMenu();
        if (manager.getWindowCount() != 0
                || nestedMenuDestroyed.load(
                        std::memory_order_relaxed) != 3)
            return fail(
                    "GUI nested menu teardown duplicated or leaked owners");
    }

    const int acceleratorQuads[] = {
        41, KEY_FLAG_LONG_PRESS, 701, 11,
        42, 0, 702, 12,
        0
    };
    CRGUIAcceleratorTable acceleratorSource(acceleratorQuads);
    CRGUIAcceleratorTable acceleratorCopy(acceleratorSource);
    if (acceleratorSource.length() != 2
            || acceleratorCopy.length() != 2
            || acceleratorSource.get(0) == acceleratorCopy.get(0))
        return fail("GUI accelerator table did not deep-copy scoped entries");

    if (acceleratorSource.add(
                41, KEY_FLAG_LONG_PRESS, 801, 21))
        return fail("GUI accelerator update unexpectedly appended an entry");
    int translatedCommand = 0;
    int translatedParam = 0;
    if (!acceleratorCopy.translate(
                41, KEY_FLAG_LONG_PRESS,
                translatedCommand, translatedParam)
            || translatedCommand != 701 || translatedParam != 11)
        return fail("GUI accelerator copy shared a mutable entry");
    if (!acceleratorCopy.add(43, 0, 703, 13)
            || acceleratorCopy.length() != 3)
        return fail("GUI accelerator candidate was not published");

    {
        GuiScreenOwnershipWindowManager manager(screen.get());
        AcceleratorGuiWindow *window =
                new AcceleratorGuiWindow(&manager);
        manager.activateWindow(window);
        window->setAccelerators(CRGUIAcceleratorTableRef(
                new CRGUIAcceleratorTable(acceleratorCopy)));
        if (!window->onKeyPressed(41, KEY_FLAG_LONG_PRESS)
                || !manager.peekEvent()
                || manager.peekEvent()->getType() != CREV_COMMAND
                || manager.peekEvent()->getParam1() != 701
                || manager.peekEvent()->getParam2() != 11)
            return fail("GUI accelerator did not publish its command event");
        if (!manager.processPostedEvents()
                || window->commandCount() != 1
                || window->lastCommand() != 701
                || window->lastParam() != 11)
            return fail("GUI scoped command event missed its target window");
    }

    if (!InitFontManager(lString8::empty_str) || !fontMan)
        return fail("GUI document-view fixture could not initialize fonts");
    const lString8 guiFontPath(
            COOLREADER_SOURCE_DIR
            "/thirdparty/harfbuzz-14.2.1/test/subset/data/expected/"
            "retain-num-glyphs/Roboto-Regular.retain-num-glyphs.all.ttf");
    if (!fontMan->RegisterFont(guiFontPath)) {
        ShutdownFontManager();
        return fail("GUI document-view fixture font did not register");
    }
#if CR_ENABLE_PAGE_IMAGE_CACHE==1
    const char *pageImageError = NULL;
    {
        LVDocView pageView(8, true);
        pageView.createDefaultDocument(
                U"Page cache", U"Scoped page image candidate");
        pageView.setViewMode(DVM_SCROLL);
        pageView.Resize(160, 180);

        bool adoptionFailed = false;
        ref_count_rec_t::failNextAllocationForRegression();
        try {
            pageView.cachePageImage(0);
        } catch (const std::bad_alloc &) {
            adoptionFailed = true;
        }
        LVCacheStats rejectedStats =
                pageView.getPageImageCacheStats();
        if (!adoptionFailed || rejectedStats.itemCount != 0)
            pageImageError =
                    "page image cache published a rejected buffer candidate";

        if (!pageImageError) {
            pageView.cachePageImage(0);
            LVDocImageRef image = pageView.getPageImage(0);
            LVCacheStats committedStats =
                    pageView.getPageImageCacheStats();
            if (image.isNull() || !image->getDrawBuf()
                    || image->getDrawBuf()->GetWidth() != 160
                    || image->getDrawBuf()->GetHeight() != 180
                    || committedStats.itemCount != 1)
                pageImageError =
                        "page image cache did not publish one complete candidate";
        }
    }
    if (pageImageError) {
        ShutdownFontManager();
        return fail(pageImageError);
    }
#endif
    std::atomic<int> docViewDestroyed(0);
    const char *docViewError = NULL;
    {
        GuiScreenOwnershipWindowManager manager(screen.get());
        {
            GuiDocViewOwnershipWindow window(&manager, docViewDestroyed);
            if (!window.getDocView()
                    || docViewDestroyed.load(std::memory_order_relaxed) != 0)
                docViewError =
                        "GUI document window did not publish its owner";
        }
        if (!docViewError
                && docViewDestroyed.load(std::memory_order_relaxed) != 1)
            docViewError = "GUI document window leaked its document view";
    }
    ShutdownFontManager();
    if (docViewError)
        return fail(docViewError);

    if (screenDestroyed.load(std::memory_order_relaxed) != 0)
        return fail("GUI runtime owners destroyed their borrowed screen");
    screen.reset();
    if (screenDestroyed.load(std::memory_order_relaxed) != 1)
        return fail("GUI runtime screen fixture did not tear down");
    return 0;
}

#if CR_ENABLE_PAGE_IMAGE_CACHE==1
static int testBoundedObservablePageImageCache() {
    LVDocViewImageCache cache;
    if (!cache.get(-1, 1).isNull())
        return fail("empty page image cache unexpectedly returned an item");

    LVRef<LVDrawBuf> first(new LVGrayDrawBuf(4, 4, 8));
    LVRef<LVDrawBuf> second(new LVGrayDrawBuf(4, 4, 8));
    LVRef<LVDrawBuf> third(new LVGrayDrawBuf(4, 4, 8));
    LVRef<LVThread> noThread;
    const int itemSize = first->GetRowSize() * first->GetHeight();
    cache.set(-1, 1, first, noThread);
    cache.set(-1, 2, second, noThread);

    LVDocImageRef image = cache.get(-1, 1);
    if (image.isNull() || image->getDrawBuf() != first.get())
        return fail("page image cache did not return the first item");
    image.Clear();
    cache.set(-1, 3, third, noThread);

    image = cache.get(-1, 1);
    if (image.isNull() || image->getDrawBuf() != first.get())
        return fail("recently used page image was evicted");
    image.Clear();
    if (!cache.get(-1, 2).isNull())
        return fail("least recently used page image was not evicted");
    image = cache.get(-1, 3);
    if (image.isNull() || image->getDrawBuf() != third.get())
        return fail("new page image was not cached");
    image.Clear();

    LVCacheStats stats = cache.getStats();
    if (stats.capacityItems != 2 || stats.itemCount != 2
            || stats.size != itemSize * 2)
        return fail("page image cache bounds are incorrect");
    if (stats.hits != 3 || stats.misses != 2 || stats.evictions != 1)
        return fail("page image cache counters are incorrect");

    cache.clear();
    stats = cache.getStats();
    if (stats.itemCount != 0 || stats.size != 0)
        return fail("page image cache retained size after clear");
    cache.resetStats();
    stats = cache.getStats();
    if (stats.hits != 0 || stats.misses != 0 || stats.evictions != 0)
        return fail("page image cache counters did not reset");
    return 0;
}
#endif

static int testLogRedactor() {
    if (CRRedactLogMessage("Rendered 42 pages") != "Rendered 42 pages")
        return fail("safe native diagnostic was unexpectedly changed");
    const char *sensitiveMessages[] = {
        "password=native-password-secret",
        "access_token=native-access-token-secret",
        "Authorization: Bearer native-token-secret",
        "GET https://example.test/book?token=native-query-secret",
        "Opening /storage/emulated/0/Books/private-title.fb2",
        "Opening C:\\Users\\alice\\Books\\private-title.epub",
        "Opening relative/private-title.mobi"
    };
    for (const char *message : sensitiveMessages) {
        std::string safe = CRRedactLogMessage(message);
        if (safe != "[redacted native diagnostic]")
            return fail("sensitive native diagnostic was not redacted");
        if (safe.find("secret") != std::string::npos
                || safe.find("private-title") != std::string::npos)
            return fail("native diagnostic leaked its canary");
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

static int testFileStreamOwnership() {
    char path[] = "/tmp/coolreader-file-stream-test-XXXXXX";
    int fd = mkstemp(path);
    if (fd < 0)
        return fail("file-stream fixture could not create a file");

    int duplicateSlot = dup(fd);
    if (duplicateSlot < 0) {
        close(fd);
        unlink(path);
        return fail("file-stream fixture could not reserve a duplicate slot");
    }
    close(duplicateSlot);

    LVFileStream owned;
    if (owned.OpenFile(fd, LVOM_READWRITE, true) != LVERR_OK
            || fcntl(duplicateSlot, F_GETFD) == -1) {
        close(fd);
        unlink(path);
        return fail("owned file stream did not retain its descriptor duplicate");
    }
    if (owned.Close() != LVERR_OK
            || owned.Close() != LVERR_OK
            || fcntl(duplicateSlot, F_GETFD) != -1
            || errno != EBADF
            || fcntl(fd, F_GETFD) == -1
            || owned.GetMode() != LVOM_CLOSED
            || owned.GetSize() != 0) {
        close(fd);
        unlink(path);
        return fail("owned file-stream close was not scoped and idempotent");
    }

    LVFileStream borrowed;
    if (borrowed.OpenFile(fd, LVOM_READWRITE, false) != LVERR_OK
            || borrowed.Close() != LVERR_OK
            || fcntl(fd, F_GETFD) == -1) {
        close(fd);
        unlink(path);
        return fail("borrowed file-stream close released the caller descriptor");
    }
    close(fd);

    const lString32 path32 = Utf8ToUnicode(lString8(path));
    const lString32 missing =
            Utf8ToUnicode(lString8((std::string(path) + ".missing").c_str()));
    LVFileStream reusable;
    static const char payload[] = "ownership";
    lvsize_t bytesWritten = 0;
    if (reusable.OpenFile(path32, LVOM_WRITE) != LVERR_OK
            || reusable.Write(payload, sizeof(payload), &bytesWritten)
                    != LVERR_OK
            || bytesWritten != sizeof(payload)
            || reusable.SetSize(32) != LVERR_OK
            || reusable.GetSize() != 32
            || reusable.GetPos() != sizeof(payload)
            || reusable.SetSize(4) != LVERR_OK
            || reusable.GetSize() != 4
            || reusable.GetPos() != 4
            || reusable.Close() != LVERR_OK) {
        unlink(path);
        return fail("file-stream resize lifecycle is inconsistent");
    }

    static const char suffix[] = {'+', '+'};
    bytesWritten = 0;
    if (reusable.OpenFile(
                path32, LVOM_APPEND | LVOM_FLAG_SYNC) != LVERR_OK
            || reusable.GetPos() != 4
            || reusable.Write(suffix, sizeof(suffix), &bytesWritten)
                    != LVERR_OK
            || bytesWritten != sizeof(suffix)
            || reusable.GetSize() != 6
            || reusable.Flush(true) != LVERR_OK) {
        unlink(path);
        return fail("file-stream append or sync mode lost its flags/state");
    }
    if (reusable.OpenFile(missing, LVOM_READ) != LVERR_FAIL
            || reusable.GetMode() != LVOM_ERROR
            || reusable.GetSize() != 0
            || reusable.OpenFile(path32, LVOM_READ) != LVERR_OK) {
        unlink(path);
        return fail("failed file-stream reopen retained stale resources");
    }
    char resized[6] = {};
    lvsize_t bytesRead = 0;
    if (reusable.Read(resized, sizeof(resized), &bytesRead) != LVERR_OK
            || bytesRead != sizeof(resized)
            || std::memcmp(resized, "owne++", sizeof(resized)) != 0) {
        unlink(path);
        return fail("file-stream resize/append bytes were not persisted");
    }
    reusable.Close();
    unlink(path);

    int pipeFds[2] = {-1, -1};
    if (pipe(pipeFds) != 0)
        return fail("file-stream rollback fixture could not create a pipe");
    int rollbackSlot = dup(pipeFds[0]);
    if (rollbackSlot < 0) {
        close(pipeFds[0]);
        close(pipeFds[1]);
        return fail("file-stream rollback could not reserve a duplicate slot");
    }
    close(rollbackSlot);
    LVFileStream rejectedOwned;
    if (rejectedOwned.OpenFile(pipeFds[0], LVOM_READ, true)
                != LVERR_FAIL
            || rejectedOwned.GetMode() != LVOM_ERROR
            || fcntl(rollbackSlot, F_GETFD) != -1
            || errno != EBADF
            || fcntl(pipeFds[0], F_GETFD) == -1) {
        close(pipeFds[0]);
        close(pipeFds[1]);
        return fail("owned descriptor rollback leaked or damaged caller state");
    }
    LVFileStream rejectedBorrowed;
    if (rejectedBorrowed.OpenFile(pipeFds[0], LVOM_READ, false)
                != LVERR_FAIL
            || rejectedBorrowed.GetMode() != LVOM_ERROR
            || fcntl(pipeFds[0], F_GETFD) == -1) {
        close(pipeFds[0]);
        close(pipeFds[1]);
        return fail("borrowed descriptor rollback damaged caller state");
    }
    close(pipeFds[0]);
    close(pipeFds[1]);
    return 0;
}

static int testDirectoryContainerOwnership() {
    char rootTemplate[] = "/tmp/coolreader-directory-test-XXXXXX";
    char *root = mkdtemp(rootTemplate);
    if (!root)
        return fail("directory-container fixture could not create its root");

    const std::string rootPath(root);
    const std::string filePath = rootPath + "/book.bin";
    const std::string createdPath = rootPath + "/created.bin";
    const std::string subdirectoryPath = rootPath + "/shelf";
    const std::string danglingPath = rootPath + "/dangling";
    auto cleanup = [&]() {
        unlink(danglingPath.c_str());
        unlink(createdPath.c_str());
        unlink(filePath.c_str());
        rmdir(subdirectoryPath.c_str());
        rmdir(rootPath.c_str());
    };

    static const char payload[] = "directory";
    int fixtureFd =
            open(filePath.c_str(), O_WRONLY | O_CREAT | O_TRUNC, 0600);
    if (fixtureFd < 0
            || write(fixtureFd, payload, sizeof(payload))
                    != static_cast<ssize_t>(sizeof(payload))) {
        if (fixtureFd >= 0)
            close(fixtureFd);
        cleanup();
        return fail("directory-container fixture file setup failed");
    }
    close(fixtureFd);
    if (mkdir(subdirectoryPath.c_str(), 0700) != 0
            || symlink("missing-target", danglingPath.c_str()) != 0) {
        cleanup();
        return fail("directory-container fixture entries setup failed");
    }

    int scanSlot = open(rootPath.c_str(), O_RDONLY);
    if (scanSlot < 0) {
        cleanup();
        return fail("directory-container fixture could not reserve scan fd");
    }
    close(scanSlot);

    LVContainerRef directory =
            LVOpenDirectory(Utf8ToUnicode(lString8(rootPath.c_str())));
    if (directory.isNull()
            || fcntl(scanSlot, F_GETFD) != -1
            || errno != EBADF
            || directory->GetObjectCount() != 2) {
        directory.Clear();
        cleanup();
        return fail("directory scan leaked its descriptor or partial entries");
    }

    const LVContainerItemInfo *fileInfo = NULL;
    const LVContainerItemInfo *subdirectoryInfo = NULL;
    for (int index = 0; index < directory->GetObjectCount(); ++index) {
        const LVContainerItemInfo *info = directory->GetObjectInfo(index);
        if (!info || !info->GetName()) {
            directory.Clear();
            cleanup();
            return fail("directory container published an invalid item");
        }
        const lString32 name(info->GetName());
        if (name == lString32(U"book.bin"))
            fileInfo = info;
        else if (name == lString32(U"shelf"))
            subdirectoryInfo = info;
    }
    if (!fileInfo || fileInfo->IsContainer()
            || fileInfo->GetSize() != sizeof(payload)
            || !subdirectoryInfo || !subdirectoryInfo->IsContainer()) {
        directory.Clear();
        cleanup();
        return fail("directory container item metadata is inconsistent");
    }

    LVStreamRef opened = directory->OpenStream(U"book.bin", LVOM_READ);
    char readback[sizeof(payload)] = {};
    lvsize_t bytesRead = 0;
    if (opened.isNull()
            || opened->Read(readback, sizeof(readback), &bytesRead)
                    != LVERR_OK
            || bytesRead != sizeof(readback)
            || std::memcmp(readback, payload, sizeof(payload)) != 0
            || !directory->OpenStream(U"shelf", LVOM_READ).isNull()
            || !directory->OpenStream(U"missing.bin", LVOM_READ).isNull()
            || directory->GetObjectCount() != 2) {
        opened.Clear();
        directory.Clear();
        cleanup();
        return fail("directory stream lookup accepted invalid entries");
    }
    opened.Clear();

    LVStreamRef created =
            directory->OpenStream(U"created.bin", LVOM_WRITE);
    static const char createdPayload[] = {'o', 'k'};
    lvsize_t bytesWritten = 0;
    if (created.isNull()
            || created->Write(
                    createdPayload, sizeof(createdPayload), &bytesWritten)
                    != LVERR_OK
            || bytesWritten != sizeof(createdPayload)
            || directory->GetObjectCount() != 3) {
        created.Clear();
        directory.Clear();
        cleanup();
        return fail("directory container did not adopt a new stream item");
    }
    created.Clear();
    directory.Clear();

    const lString32 missingPath =
            Utf8ToUnicode(lString8((rootPath + ".missing").c_str()));
    if (!LVOpenDirectory(missingPath).isNull()
            || !LVOpenDirectory(U"").isNull()
            || !LVOpenDirectory(
                    Utf8ToUnicode(lString8(filePath.c_str()))).isNull()) {
        cleanup();
        return fail("directory factory published an invalid candidate");
    }

    cleanup();
    return 0;
}

static int testIniTranslatorOwnership() {
    char path[] = "/tmp/coolreader-i18n-test-XXXXXX";
    int fd = mkstemp(path);
    if (fd < 0)
        return fail("i18n mkstemp failed");

    const unsigned char payload[] = {
        0xEF, 0xBB, 0xBF,
        'h', 'e', 'l', 'l', 'o', '=', 'w', 'o', 'r', 'l', 'd', '\n',
        'b', 'o', 'o', 'k', '=', 'r', 'e', 'a', 'd', 'e', 'r', '\n'
    };
    if (write(fd, payload, sizeof(payload))
            != static_cast<ssize_t>(sizeof(payload))) {
        close(fd);
        unlink(path);
        return fail("i18n fixture write failed");
    }
    close(fd);

    CRIniFileTranslator translator;
    if (!translator.open(path)) {
        unlink(path);
        return fail("INI translator rejected a valid file");
    }
    lString8 translated;
    if (!translator._map.get(lString8("hello"), translated)
            || translated != lString8("world")) {
        unlink(path);
        return fail("INI translator did not parse BOM-prefixed content");
    }
    if (!translator._map.get(lString8("book"), translated)
            || translated != lString8("reader")) {
        unlink(path);
        return fail("INI translator did not parse the second entry");
    }

    CRIniFileTranslator *created =
            CRIniFileTranslator::create(path);
    unlink(path);
    if (created == NULL)
        return fail("INI translator factory rejected a valid file");
    delete created;

    CRIniFileTranslator *missing =
            CRIniFileTranslator::create(path);
    if (missing != NULL) {
        delete missing;
        return fail("INI translator factory accepted a missing file");
    }
    return 0;
}

class CountingI18NTranslator : public CRI18NTranslator {
private:
    lString8 _source;
    lString8 _translation;
    static int _destroyed;

protected:
    const char *getText(const char *source) override {
        if (_source == source)
            return _translation.c_str();
        return "";
    }

public:
    CountingI18NTranslator(const char *source, const char *translation)
        : _source(source), _translation(translation)
    {
    }

    ~CountingI18NTranslator() override {
        ++_destroyed;
    }

    static int destroyed() {
        return _destroyed;
    }

    static void reset() {
        _destroyed = 0;
    }
};

int CountingI18NTranslator::_destroyed = 0;

static int testTranslatorOwnerLifecycle() {
    CRI18NTranslator::setTranslator(NULL);
    CRI18NTranslator::setDefTranslator(NULL);
    CountingI18NTranslator::reset();

    std::unique_ptr<CountingI18NTranslator> fallback(
            new CountingI18NTranslator("fallback", "from-default"));
    CountingI18NTranslator *fallbackView = fallback.get();
    CRI18NTranslator::setDefTranslator(fallback.release());

    std::unique_ptr<CountingI18NTranslator> active(
            new CountingI18NTranslator("active", "from-active"));
    CountingI18NTranslator *activeView = active.get();
    CRI18NTranslator::setTranslator(active.release());
    if (std::strcmp(CRI18NTranslator::translate("active"), "from-active")
                    != 0
            || std::strcmp(
                    CRI18NTranslator::translate("fallback"),
                    "from-default") != 0
            || std::strcmp(CRI18NTranslator::translate("missing"), "missing")
                    != 0)
        return fail("translation owner graph did not preserve fallback order");

    CRI18NTranslator::setTranslator(activeView);
    if (CountingI18NTranslator::destroyed() != 0
            || std::strcmp(
                    CRI18NTranslator::translate("active"),
                    "from-active") != 0)
        return fail("idempotent translator publication destroyed its owner");

    CRI18NTranslator::setTranslator(
            new CountingI18NTranslator("replacement", "replaced"));
    if (CountingI18NTranslator::destroyed() != 1
            || std::strcmp(
                    CRI18NTranslator::translate("replacement"),
                    "replaced") != 0)
        return fail("translator replacement did not release the old owner");

    CRI18NTranslator::setTranslator(fallbackView);
    if (CountingI18NTranslator::destroyed() != 2
            || std::strcmp(
                    CRI18NTranslator::translate("fallback"),
                    "from-default") != 0
            || std::strcmp(CRI18NTranslator::translate("active"), "active")
                    != 0)
        return fail("translator slot transfer duplicated exclusive ownership");

    CRI18NTranslator::setTranslator(NULL);
    std::unique_ptr<CountingI18NTranslator> secondFallback(
            new CountingI18NTranslator("second", "second-default"));
    CountingI18NTranslator *secondFallbackView = secondFallback.get();
    CRI18NTranslator::setDefTranslator(secondFallback.release());
    CRI18NTranslator::setDefTranslator(secondFallbackView);
    if (CountingI18NTranslator::destroyed() != 3)
        return fail("idempotent fallback publication destroyed its owner");
    CRI18NTranslator::setDefTranslator(NULL);
    if (CountingI18NTranslator::destroyed() != 4)
        return fail("translator clear did not release both owner slots");
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

class RecordingRtfCallback : public NoOpXmlCallback {
private:
    void noteCallback()
    {
        if (stopped)
            callbackAfterStop = true;
    }

public:
    lString32 text;
    bool stopped;
    bool callbackAfterStop;
    int stopCount;
    int blobCount;

    RecordingRtfCallback()
        : stopped(false)
        , callbackAfterStop(false)
        , stopCount(0)
        , blobCount(0)
    {
    }

    void OnStart(LVFileFormatParser *parser) override
    {
        LVXMLParserCallback::OnStart(parser);
        text.clear();
        stopped = false;
        callbackAfterStop = false;
        stopCount = 0;
        blobCount = 0;
    }

    void OnStop() override
    {
        noteCallback();
        stopped = true;
        ++stopCount;
    }

    ldomNode *OnTagOpen(const lChar32 *, const lChar32 *) override
    {
        noteCallback();
        return NULL;
    }

    void OnTagBody() override
    {
        noteCallback();
    }

    void OnTagClose(const lChar32 *, const lChar32 *, bool) override
    {
        noteCallback();
    }

    void OnAttribute(const lChar32 *, const lChar32 *,
                     const lChar32 *) override
    {
        noteCallback();
    }

    void OnText(const lChar32 *value, int length, lUInt32) override
    {
        noteCallback();
        text.append(value, length);
    }

    bool OnBlob(lString32, const lUInt8 *, int) override
    {
        noteCallback();
        ++blobCount;
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

class ParserBufferProbe : public LVTextFileBase {
public:
    explicit ParserBufferProbe(LVStreamRef stream)
        : LVTextFileBase(stream)
    {
    }

    bool CheckFormat() override
    {
        return true;
    }

    bool Parse() override
    {
        return true;
    }

    bool seekWindow(lvpos_t position, int prefetch)
    {
        return Seek(position, prefetch);
    }

    int windowLength() const
    {
        return m_buf_len;
    }

    std::size_t bufferCapacity() const
    {
        return m_buf.size();
    }

    lUInt8 windowByte(int index) const
    {
        return m_buf[index];
    }
};

static LVStreamRef memoryStream(const std::string &contents) {
    return LVCreateMemoryStream(
            const_cast<char *>(contents.data()),
            static_cast<int>(contents.size()), true, LVOM_READ);
}

static int testTextLineQueueOwnership() {
    ParserBufferProbe file(memoryStream(
            "first line\nsecond line\nthird line\nfourth line\n"));
    file.SetCharset(U"utf-8");
    LVTextLineQueue queue(file, 80);
    if (!queue.ReadLines(3)
            || queue.length() != 3
            || queue.GetFirstLineIndex() != 0
            || queue.GetLineCount() != 3
            || queue.GetLine(0)->text != lString32(U"first line")
            || queue.GetLine(2)->text != lString32(U"third line")) {
        return fail("text line queue did not publish owned lines");
    }

    queue.RemoveLines(2);
    if (queue.length() != 1
            || queue.GetFirstLineIndex() != 2
            || queue.GetLineCount() != 3
            || queue.GetLine(2)->text != lString32(U"third line")) {
        return fail("text line queue head removal lost borrowed views");
    }
    if (!queue.ReadLines(8)
            || queue.length() != 2
            || queue.GetLineCount() != 4
            || queue.GetLine(3)->text != lString32(U"fourth line")) {
        return fail("text line queue could not append after owner removal");
    }

    queue.RemoveLines(99);
    if (queue.length() != 0
            || queue.GetFirstLineIndex() != 4
            || queue.GetLineCount() != 4
            || queue.ReadLines(1)) {
        return fail("text line queue teardown retained owned lines");
    }

    std::string document;
    for (int line = 0; line < 2400; ++line) {
        document += "plain text ownership line ";
        document += std::to_string(line);
        document += '\n';
    }
    NoOpXmlCallback callback;
    LVTextParser parser(memoryStream(document), &callback, false);
    parser.SetCharset(U"utf-8");
    if (!parser.Parse())
        return fail("text parser rejected the RAII line queue");
    return 0;
}

class MisreportingPropertyStream : public LVMemoryStream {
    bool _valid;

public:
    explicit MisreportingPropertyStream(const std::string &contents)
        : _valid(CreateCopy(
                reinterpret_cast<const lUInt8 *>(contents.data()),
                contents.size(), LVOM_READ) == LVERR_OK)
    {
    }

    bool valid() const {
        return _valid;
    }

    lverror_t Read(
            void *buf, lvsize_t count, lvsize_t *bytesRead) override
    {
        lvsize_t actual = 0;
        lverror_t result = LVMemoryStream::Read(buf, count, &actual);
        if (bytesRead)
            *bytesRead = actual > 0 ? actual - 1 : 0;
        return result;
    }
};

class OversizedPropertyStream : public LVStream {
    int _readCalls;

public:
    OversizedPropertyStream() : _readCalls(0) {
    }

    lvopen_mode_t GetMode() override { return LVOM_READ; }

    lverror_t Seek(lvoffset_t offset, lvseek_origin_t origin,
            lvpos_t *newPos) override
    {
        if (offset != 0 || origin != LVSEEK_CUR)
            return LVERR_FAIL;
        if (newPos)
            *newPos = 0;
        return LVERR_OK;
    }

    lvsize_t GetSize() override {
        return ParseBudgetLimits::defaults().maxInputBytes + 1;
    }

    lverror_t SetSize(lvsize_t) override { return LVERR_FAIL; }

    lverror_t Read(void *, lvsize_t, lvsize_t *bytesRead) override {
        ++_readCalls;
        if (bytesRead)
            *bytesRead = 0;
        return LVERR_FAIL;
    }

    lverror_t Write(
            const void *, lvsize_t, lvsize_t *bytesWritten) override
    {
        if (bytesWritten)
            *bytesWritten = 0;
        return LVERR_FAIL;
    }

    bool Eof() override { return false; }

    int readCalls() const {
        return _readCalls;
    }
};

class RejectingPropertyWriteStream : public LVStream {
    int _writeCalls;
    bool _shortSuccess;

public:
    explicit RejectingPropertyWriteStream(bool shortSuccess = false)
        : _writeCalls(0), _shortSuccess(shortSuccess)
    {
    }

    lvopen_mode_t GetMode() override { return LVOM_WRITE; }

    lverror_t Seek(
            lvoffset_t, lvseek_origin_t, lvpos_t *) override
    {
        return LVERR_FAIL;
    }

    lverror_t SetSize(lvsize_t) override { return LVERR_FAIL; }

    lverror_t Read(void *, lvsize_t, lvsize_t *bytesRead) override {
        if (bytesRead)
            *bytesRead = 0;
        return LVERR_FAIL;
    }

    lverror_t Write(
            const void *, lvsize_t count, lvsize_t *bytesWritten) override
    {
        ++_writeCalls;
        if (_shortSuccess) {
            if (bytesWritten)
                *bytesWritten = count > 0 ? count - 1 : 0;
            return LVERR_OK;
        }
        if (bytesWritten)
            *bytesWritten = 0;
        return LVERR_FAIL;
    }

    bool Eof() override { return false; }

    int writeCalls() const {
        return _writeCalls;
    }
};

static int testPropertyStreamOwnership() {
    CRPropRef properties = LVCreatePropsContainer();
    properties->setString("stable", "old");
    properties->setString("untouched", "keep");
    const std::string input =
            "\xEF\xBB\xBF"
            "#ignored=value\n"
            "stable=new\n"
            "escaped=line\\nnext\\\\slash\\rreturn\n"
            "trailing=value\\";
    LVStreamRef inputStream = memoryStream(input);
    if (inputStream.isNull()
            || !properties->loadFromStream(inputStream.get()))
        return fail("property loader rejected a valid scoped buffer");
    if (properties->getStringDef("stable") != U"new"
            || properties->getStringDef("untouched") != U"keep"
            || properties->getStringDef("escaped")
                    != U"line\nnext\\slash\rreturn"
            || properties->getStringDef("trailing") != U"value\\"
            || properties->hasProperty("#ignored"))
        return fail("property loader did not publish the parsed snapshot");

    properties->setString("section.alpha", "one");
    CRPropRef section = properties->getSubProps("section.");
    CRPropRef sectionSnapshot = section->clone();
    if (sectionSnapshot->getStringDef("alpha") != U"one"
            || sectionSnapshot->getCount() != 1)
        return fail("property sub-container clone did not publish its owners");
    section->setString("beta", "two");
    sectionSnapshot->setString("alpha", "snapshot");
    if (sectionSnapshot->hasProperty("beta")
            || properties->getStringDef("section.alpha") != U"one"
            || section->getStringDef("beta") != U"two"
            || sectionSnapshot->getStringDef("alpha") != U"snapshot")
        return fail("property sub-container clone shared mutable item owners");
    CRPropRef topLevelSnapshot = properties->clone();
    topLevelSnapshot->setString("stable", "snapshot");
    if (topLevelSnapshot->getStringDef("stable") != U"snapshot"
            || properties->getStringDef("stable") != U"new")
        return fail("property container clone lost independent revision state");

    CRPropRef serializedProperties = LVCreatePropsContainer();
    serializedProperties->setString("alpha", "one");
    serializedProperties->setString("beta", "two");
    SerialBuf serializedPropertiesBuffer(1, true);
    serializedProperties->serialize(serializedPropertiesBuffer);
    if (serializedPropertiesBuffer.error())
        return fail("property serialization fixture could not serialize");
    const int serializedPropertiesSize = serializedPropertiesBuffer.pos();
    std::vector<lUInt8> serializedPropertyBytes(
            serializedPropertiesBuffer.buf(),
            serializedPropertiesBuffer.buf() + serializedPropertiesSize);

    CRPropRef serializedTarget = LVCreatePropsContainer();
    serializedTarget->setString("sentinel", "keep");
    std::vector<lUInt8> corruptedPropertyBytes(serializedPropertyBytes);
    corruptedPropertyBytes.back() ^= 0x80;
    SerialBuf corruptedPropertyInput(
            corruptedPropertyBytes.data(),
            static_cast<int>(corruptedPropertyBytes.size()));
    if (serializedTarget->deserialize(corruptedPropertyInput)
            || serializedTarget->getStringDef("sentinel") != U"keep"
            || serializedTarget->hasProperty("alpha"))
        return fail(
                "property serialization CRC failure replaced committed state");

    std::vector<lUInt8> oversizedPropertyCount(serializedPropertyBytes);
    oversizedPropertyCount[4] = 0xFF;
    oversizedPropertyCount[5] = 0xFF;
    oversizedPropertyCount[6] = 0xFF;
    oversizedPropertyCount[7] = 0x7F;
    SerialBuf oversizedPropertyInput(
            oversizedPropertyCount.data(),
            static_cast<int>(oversizedPropertyCount.size()));
    if (serializedTarget->deserialize(oversizedPropertyInput)
            || !oversizedPropertyInput.error()
            || serializedTarget->getStringDef("sentinel") != U"keep"
            || serializedTarget->hasProperty("alpha"))
        return fail("property serialization accepted an oversized count");

    SerialBuf serializedPropertyInput(
            serializedPropertyBytes.data(),
            static_cast<int>(serializedPropertyBytes.size()));
    if (!serializedTarget->deserialize(serializedPropertyInput)
            || serializedTarget->getCount() != 2
            || serializedTarget->getStringDef("alpha") != U"one"
            || serializedTarget->getStringDef("beta") != U"two"
            || serializedTarget->hasProperty("sentinel"))
        return fail(
                "property serialization snapshot did not replace committed state");

    lString32 largeValue;
    largeValue.append(12000, U'x');
    properties->setString("large", largeValue);
    LVStreamRef output =
            LVCreateMemoryStream(NULL, 0, false, LVOM_WRITE);
    if (output.isNull() || !properties->saveToStream(output.get())
            || output->GetSize() <= 10000
            || output->SetPos(0) != 0)
        return fail("property saver did not write its complete snapshot");
    CRPropRef roundTrip = LVCreatePropsContainer();
    if (!roundTrip->loadFromStream(output.get())
            || roundTrip->getStringDef("escaped")
                    != U"line\nnext\\slash\rreturn"
            || roundTrip->getStringDef("trailing") != U"value\\"
            || roundTrip->getStringDef("large") != largeValue)
        return fail("property stream snapshot did not round-trip");

    CRPropRef preserved = LVCreatePropsContainer();
    preserved->setString("stable", "original");
    const std::string replacement =
            "stable=replaced\ncandidate=published\n";
    MisreportingPropertyStream shortRead(replacement);
    if (!shortRead.valid())
        return fail("property short-read fixture could not initialize");
    if (preserved->loadFromStream(&shortRead)
            || preserved->getStringDef("stable") != U"original"
            || preserved->hasProperty("candidate"))
        return fail("short property read published a partial snapshot");

    OversizedPropertyStream oversized;
    if (preserved->loadFromStream(&oversized)
            || oversized.readCalls() != 0
            || preserved->getStringDef("stable") != U"original")
        return fail("oversized property input reached allocation or publication");

    RejectingPropertyWriteStream rejecting;
    if (properties->saveToStream(&rejecting)
            || rejecting.writeCalls() != 1)
        return fail("property save reported success after target write failure");
    RejectingPropertyWriteStream shortWrite(true);
    if (properties->saveToStream(&shortWrite)
            || shortWrite.writeCalls() != 1)
        return fail("property save accepted a short target write");
    if (properties->loadFromStream(NULL)
            || properties->saveToStream(NULL)
            || properties->saveToStream(inputStream.get()))
        return fail("property streams accepted null or incompatible modes");
    return 0;
}

static std::string historyDocument(const char *fileName,
                                   const char *title,
                                   const char *startPos) {
    std::string xml =
            "<?xml version=\"1.0\" encoding=\"utf-8\"?>"
            "<FictionBookMarks><file><file-info><doc-title>";
    xml += title;
    xml += "</doc-title><doc-author>Author</doc-author>"
           "<doc-series>Series</doc-series><doc-filename>";
    xml += fileName;
    xml += "</doc-filename><doc-filepath>/books/</doc-filepath>"
           "<doc-filesize>4096</doc-filesize>"
           "<doc-dom-version>20260728</doc-dom-version>"
           "</file-info><bookmark-list>"
           "<bookmark type=\"lastpos\" percent=\"12.05%\" "
           "timestamp=\"1234\" shortcut=\"0\" page=\"7\">"
           "<start-point>";
    xml += startPos;
    xml += "</start-point><end-point/>"
           "<header-text>Last chapter</header-text>"
           "<selection-text>Last excerpt</selection-text>"
           "<comment-text/></bookmark>"
           "<bookmark type=\"position\" percent=\"42.03%\" "
           "timestamp=\"2345\" shortcut=\"3\" page=\"9\">"
           "<start-point>/saved</start-point><end-point/>"
           "<header-text>Saved chapter</header-text>"
           "<selection-text>Saved excerpt</selection-text>"
           "<comment-text>Saved note</comment-text>"
           "</bookmark></bookmark-list></file></FictionBookMarks>";
    return xml;
}

static int testHistoryOwnership() {
    CRFileHist history;
    if (!history.loadFromStream(
                memoryStream(historyDocument("first.fb2", "First", "/first"))))
        return fail("history parser rejected a valid document");
    if (history.getRecords().length() != 1)
        return fail("history parser did not publish one complete record");

    CRFileHistRecord *record = history.getRecords()[0];
    if (record->getFileName() != U"first.fb2"
            || record->getTitle() != U"First"
            || record->getFilePath() != U"/books/"
            || record->getFileSize() != 4096
            || record->getDOMversion() != 20260728)
        return fail("history parser lost file metadata");
    if (record->getLastPos()->getStartPos() != U"/first"
            || record->getLastPos()->getType() != bmkt_lastpos
            || record->getLastPos()->getTimestamp() != 1234
            || record->getLastPos()->getBookmarkPage() != 7)
        return fail("history parser lost the last-position bookmark");
    if (record->getBookmarks().length() != 1
            || record->getBookmarks()[0]->getStartPos() != U"/saved"
            || record->getBookmarks()[0]->getType() != bmkt_pos
            || record->getBookmarks()[0]->getShortcut() != 3
            || record->getBookmarks()[0]->getCommentText() != U"Saved note")
        return fail("history parser lost an adopted bookmark");

    if (!history.loadFromStream(
                memoryStream(historyDocument("second.fb2", "Second", "/second")))
            || history.getRecords().length() != 1
            || history.getRecords()[0]->getFileName() != U"second.fb2")
        return fail("history reload did not replace the committed snapshot");

    std::string rejected =
            historyDocument("partial.fb2", "Partial", "/partial");
    rejected.erase(rejected.rfind("</FictionBookMarks>"));
    for (unsigned i = 0;
            i < ParseBudgetLimits::defaults().maxXmlDepth + 1; ++i)
        rejected += "<n>";
    for (unsigned i = 0;
            i < ParseBudgetLimits::defaults().maxXmlDepth + 1; ++i)
        rejected += "</n>";
    rejected += "</FictionBookMarks>";
    if (history.loadFromStream(memoryStream(rejected)))
        return fail("history parser accepted an over-depth document");
    if (history.getRecords().length() != 1
            || history.getRecords()[0]->getFileName() != U"second.fb2"
            || history.getRecords()[0]->getLastPos()->getStartPos()
                    != U"/second")
        return fail("failed history load published its valid prefix");
    if (history.loadFromStream(LVStreamRef()))
        return fail("history parser accepted a null stream");

    CRBookmark bookmark;
    bookmark.setType(bmkt_comment);
    bookmark.setStartPos(U"/start\\segment\nline");
    bookmark.setEndPos(U"/end\rsegment");
    bookmark.setTitleText(U"Header\\line\nnext");
    bookmark.setPosText(U"Excerpt\tcolumn");
    bookmark.setCommentText(U"Comment\r\nbackslash\\");
    bookmark.setPercent(6789);
    bookmark.setShortcut(4);
    bookmark.setTimestamp(3456);
    const lString32 fileName(U"folder\\book\nname\t.fb2");
    ChangeInfo source(&bookmark, fileName, false);
    const lString8 serialized = source.toString();
    std::unique_ptr<ChangeInfo> parsed(ChangeInfo::fromString(serialized));
    if (!parsed.get() || !parsed->getBookmark()
            || parsed->getFileName() != fileName
            || parsed->getTimestamp() != 3456
            || parsed->getBookmark()->getStartPos()
                    != bookmark.getStartPos()
            || parsed->getBookmark()->getEndPos() != bookmark.getEndPos()
            || parsed->getBookmark()->getTitleText()
                    != bookmark.getTitleText()
            || parsed->getBookmark()->getPosText() != bookmark.getPosText()
            || parsed->getBookmark()->getCommentText()
                    != bookmark.getCommentText())
        return fail("change record did not round-trip escaped text");

    ChangeInfo copied(*parsed);
    parsed->getBookmark()->setStartPos(U"/changed");
    if (!copied.getBookmark()
            || copied.getBookmark()->getStartPos()
                    != bookmark.getStartPos())
        return fail("change record copy shared bookmark ownership");
    ChangeInfo assigned;
    assigned = *parsed;
    parsed->getBookmark()->setEndPos(U"/changed-end");
    if (!assigned.getBookmark()
            || assigned.getBookmark()->getEndPos() != bookmark.getEndPos())
        return fail("change record assignment shared bookmark ownership");

    std::string framed("junk");
    framed.append(serialized.c_str(), serialized.length());
    framed += "tail";
    int recordStart = -1;
    int recordEnd = -1;
    if (!ChangeInfo::findNextRecordBounds(
                &framed[0], 0, static_cast<int>(framed.size()),
                recordStart, recordEnd)
            || recordStart != 4
            || recordEnd != 4 + serialized.length())
        return fail("change record bounds did not preserve its transfer range");
    std::unique_ptr<ChangeInfo> fromBytes(ChangeInfo::fromBytes(
            &framed[0], recordStart, recordEnd));
    if (!fromBytes.get() || fromBytes->getFileName() != fileName)
        return fail("change record byte factory did not transfer a candidate");
    if (ChangeInfo::fromString(lString8(
                "# start record\nFILE=invalid\n# end record\n")) != NULL
            || ChangeInfo::fromBytes(NULL, 0, 0) != NULL
            || ChangeInfo::fromBytes(&framed[0], -1, recordEnd) != NULL
            || ChangeInfo::findNextRecordBounds(
                    NULL, 0, 0, recordStart, recordEnd))
        return fail("invalid change record input published an owner");
    return 0;
}

static int testParserOwnedBuffers() {
    std::vector<unsigned char> payload(12000);
    for (std::size_t i = 0; i < payload.size(); ++i)
        payload[i] = static_cast<unsigned char>((i * 17 + 3) % 251);
    ParserBufferProbe parser(LVCreateMemoryStream(
            payload.data(), static_cast<int>(payload.size()),
            true, LVOM_READ));

    if (!parser.seekWindow(0, 9000)
            || parser.bufferCapacity() < 9000
            || parser.windowLength() != 9000)
        return fail("parser buffer did not grow for a prefetched window");
    if (!parser.seekWindow(11000, 0)
            || parser.bufferCapacity() < 9000
            || parser.windowLength() != 1000
            || parser.windowByte(0) != payload[11000]
            || parser.windowByte(999) != payload[11999])
        return fail("parser tail window exposed stale buffer capacity");

    lChar32 charsetTable[128];
    for (int i = 0; i < 128; ++i)
        charsetTable[i] = static_cast<lChar32>(0x400 + i);
    parser.SetCharsetTable(charsetTable);
    lChar32 *ownedTable = parser.GetCharsetTable();
    if (!ownedTable || ownedTable[0] != 0x400 || ownedTable[127] != 0x47f)
        return fail("parser did not copy the charset table");
    charsetTable[0] = 0;
    if (parser.GetCharsetTable()[0] != 0x400)
        return fail("parser charset table retained caller storage");
    parser.SetCharset(U"utf-8");
    if (parser.GetCharsetTable() != NULL)
        return fail("UTF-8 parser retained an obsolete charset table");
    return 0;
}

static int testSerialBufOwnership() {
    SerialBuf serialized(1, true);
    serialized.putMagic("SB");
    serialized << static_cast<lUInt32>(0x78563412);
    serialized << lString8("payload");
    const int payloadSize = serialized.pos();
    serialized.putCRC(payloadSize);
    if (serialized.error()
            || serialized.size() <= 1
            || serialized.pos() != payloadSize + 4)
        return fail("SerialBuf did not grow its owned storage");

    SerialBuf borrowed(serialized.buf(), serialized.pos());
    lUInt32 number = 0;
    lString8 text;
    if (!borrowed.checkMagic("SB"))
        return fail("SerialBuf rejected its serialized magic");
    borrowed >> number >> text;
    if (number != 0x78563412
            || text != lString8("payload")
            || !borrowed.checkCRC(payloadSize))
        return fail("SerialBuf did not preserve serialized contents");

    lUInt8 unchanged = 0x7c;
    borrowed >> unchanged;
    if (!borrowed.error() || unchanged != 0x7c)
        return fail("borrowed SerialBuf accepted an out-of-bounds read");

    SerialBuf fixed(2, false);
    fixed << static_cast<lUInt32>(0x01020304);
    if (!fixed.error() || fixed.pos() != 0)
        return fail("fixed SerialBuf accepted an out-of-bounds write");

    lUInt8 *legacy = static_cast<lUInt8 *>(std::malloc(3));
    if (!legacy)
        return fail("SerialBuf adoption fixture allocation failed");
    legacy[0] = 0x11;
    legacy[1] = 0x22;
    legacy[2] = 0x33;
    SerialBuf adopted(0, true);
    adopted.set(legacy, 3);
    adopted << static_cast<lUInt8>(0x44);
    if (adopted.error()
            || adopted.pos() != 4
            || adopted.buf()[0] != 0x11
            || adopted.buf()[3] != 0x44)
        return fail("SerialBuf did not adopt and grow legacy storage");

    SerialBuf swapped(0, true);
    swapped.swap(adopted);
    if (swapped.pos() != 4
            || swapped.buf()[1] != 0x22
            || adopted.pos() != 0)
        return fail("SerialBuf swap lost its owned-storage view");

    std::vector<lUInt8> movedStorage = {0x55, 0x66, 0x77};
    SerialBuf moved(0, true);
    moved.set(std::move(movedStorage));
    if (moved.error() || moved.size() != 3 || moved.pos() != 3
            || moved.buf()[0] != 0x55 || moved.buf()[2] != 0x77)
        return fail("SerialBuf did not accept moved storage");
    return 0;
}

static int testEmbeddedFontOwnership() {
    LVEmbeddedFontDef serializedDefinition(
            lString32(U"fonts/definition.ttf"),
            lString8("Serialized Definition"), true, true);
    SerialBuf serializedDefinitionBuffer(1, true);
    if (!serializedDefinition.serialize(serializedDefinitionBuffer)
            || serializedDefinitionBuffer.error())
        return fail("embedded font definition fixture could not serialize");
    LVEmbeddedFontDef committedDefinition(
            lString32(U"fonts/sentinel.ttf"),
            lString8("Definition Sentinel"), false, false);
    SerialBuf truncatedDefinition(
            serializedDefinitionBuffer.buf(),
            serializedDefinitionBuffer.pos() - 1);
    if (committedDefinition.deserialize(truncatedDefinition)
            || committedDefinition.getUrl()
                    != lString32(U"fonts/sentinel.ttf")
            || committedDefinition.getFace()
                    != lString8("Definition Sentinel")
            || committedDefinition.getBold()
            || committedDefinition.getItalic())
        return fail(
                "embedded font definition truncation replaced committed state");

    LVEmbeddedFontList source;
    source.add(
            lString32(U"fonts/regular.ttf"),
            lString8("Owned Regular"), false, false);
    source.add(
            lString32(U"fonts/bold-italic.ttf"),
            lString8("Owned Bold Italic"), true, true);
    if (source.length() != 2)
        return fail("embedded font owners were not published");

    LVEmbeddedFontDef *sourceRegular =
            source.findByUrl(lString32(U"fonts/regular.ttf"));
    LVEmbeddedFontList copied(source);
    LVEmbeddedFontList replaced;
    replaced.add(lString32(U"stale.ttf"));
    replaced.set(source);
    if (!sourceRegular
            || copied.length() != 2
            || replaced.length() != 2
            || copied.findByUrl(lString32(U"fonts/regular.ttf"))
                    == sourceRegular
            || replaced.findByUrl(lString32(U"stale.ttf"))) {
        return fail("embedded font owners were not deep-copied");
    }
    source.clear();
    if (copied.length() != 2 || replaced.length() != 2)
        return fail("embedded font copy shared owner state");

    SerialBuf serialized(1, true);
    if (!copied.serialize(serialized) || serialized.error())
        return fail("embedded font owners could not serialize");
    const int serializedSize = serialized.pos();

    SerialBuf input(serialized.buf(), serializedSize);
    LVEmbeddedFontList restored;
    restored.add(
            lString32(U"sentinel.ttf"),
            lString8("Sentinel"), false, false);
    if (!restored.deserialize(input)
            || restored.length() != 3
            || !restored.findByUrl(lString32(U"sentinel.ttf"))) {
        return fail("embedded font deserialize lost append semantics");
    }
    LVEmbeddedFontDef *restoredBold = restored.findByUrl(
            lString32(U"fonts/bold-italic.ttf"));
    if (!restoredBold
            || restoredBold->getFace() != lString8("Owned Bold Italic")
            || !restoredBold->getBold()
            || !restoredBold->getItalic()) {
        return fail("embedded font round-trip lost definition state");
    }

    std::vector<lUInt8> truncated(
            serialized.buf(), serialized.buf() + serializedSize - 1);
    SerialBuf corruptedInput(
            truncated.data(), static_cast<int>(truncated.size()));
    LVEmbeddedFontList rollback;
    rollback.add(
            lString32(U"rollback-sentinel.ttf"),
            lString8("Rollback Sentinel"), false, false);
    if (rollback.deserialize(corruptedInput)
            || rollback.length() != 1
            || !rollback.findByUrl(
                    lString32(U"rollback-sentinel.ttf"))
            || rollback.findByUrl(
                    lString32(U"fonts/regular.ttf"))) {
        return fail(
                "embedded font deserialize published a partial list");
    }

    std::vector<lUInt8> oversizedCount(
            serialized.buf(), serialized.buf() + serializedSize);
    oversizedCount[4] = 0xFF;
    oversizedCount[5] = 0xFF;
    oversizedCount[6] = 0xFF;
    oversizedCount[7] = 0x7F;
    SerialBuf oversizedInput(
            oversizedCount.data(),
            static_cast<int>(oversizedCount.size()));
    if (rollback.deserialize(oversizedInput)
            || !oversizedInput.error()
            || rollback.length() != 1
            || !rollback.findByUrl(
                    lString32(U"rollback-sentinel.ttf")))
        return fail("embedded font list accepted an oversized count");
    return 0;
}

static int testCacheFileCodecOwnership() {
    std::vector<lUInt8> input(400000);
    lUInt32 state = 0x12345678U;
    for (std::size_t index = 0; index < input.size(); index++) {
        state = state * 1664525U + 1013904223U;
        input[index] = static_cast<lUInt8>(state >> 24);
    }
#if (USE_ZLIB == 1)
    if (!LVRunCacheFileCodecRegression(
                CacheCompressionZlib, input))
        return fail("zlib cache codec ownership regression failed");
#endif
#if (USE_ZSTD == 1)
    if (!LVRunCacheFileCodecRegression(
                CacheCompressionZSTD, input))
        return fail("zstd cache codec ownership regression failed");
#endif
    return 0;
}

static int testCacheFileIndexOwnership() {
    if (!LVRunCacheFileIndexRegression())
        return fail("cache-file index ownership regression failed");
    return 0;
}

static int testDomBlobOwnership() {
    if (!LVRunBlobCacheRegression())
        return fail("DOM blob ownership regression failed");
    return 0;
}

static int testDomChunkStorageOwnership() {
    if (!LVRunDomChunkStorageRegression())
        return fail("DOM chunk storage ownership regression failed");
    return 0;
}

static int testDomNodePartOwnership() {
    if (!LVRunDomNodePartOwnershipRegression())
        return fail("DOM node-part/cache-file ownership regression failed");
    return 0;
}

static int testDomMutableNodeOwnership() {
    if (!LVRunDomMutableNodeOwnershipRegression())
        return fail("DOM mutable/persistent node ownership regression failed");
    return 0;
}

static int testXPointerStateOwnership() {
    ldomXPointer pointer;
    pointer.setOffset(11);
    {
        ldomXPointer alias(pointer);
        alias.setOffset(22);
        if (pointer.getOffset() != 22)
            return fail("XPointer copy lost shared state semantics");

        ldomXPointer assigned;
        assigned = pointer;
        assigned.setOffset(33);
        if (pointer.getOffset() != 33 || alias.getOffset() != 33)
            return fail("XPointer assignment lost shared state semantics");

        alias.clear();
        alias.clear();
        if (alias.getOffset() != 0 || pointer.getOffset() != 33)
            return fail("XPointer clear did not detach shared state");
    }
    if (pointer.getOffset() != 33)
        return fail("XPointer alias teardown released live shared state");

    std::unique_ptr<ldomXPointer> clone(pointer.clone());
    clone->setOffset(44);
    if (pointer.getOffset() != 33 || clone->getOffset() != 44)
        return fail("XPointer clone did not own an independent state");

    ldomXPointerEx extended(pointer);
    extended.setOffset(55);
    if (pointer.getOffset() != 33 || extended.getOffset() != 55)
        return fail("extended XPointer copy shared base state");

    ldomXPointerEx extendedCopy;
    extendedCopy = extended;
    extendedCopy.setOffset(66);
    if (extended.getOffset() != 55 || extendedCopy.getOffset() != 66)
        return fail("extended XPointer assignment shared cloned state");
    return 0;
}

static int testDoubleCharStatOwnership() {
    if (!LVRunDoubleCharStatOwnershipRegression())
        return fail("double-character statistic ownership regression failed");
    return 0;
}

static int testEncodingStatsFileOwnership() {
    char inputPath[] = "/tmp/coolreader-encoding-stats-XXXXXX";
    int input = mkstemp(inputPath);
    if (input < 0)
        return fail("encoding stats owner fixture could not create input");
    static const char payload[] =
            "<html><body>Ownership statistics input.</body></html>";
    if (write(input, payload, sizeof(payload) - 1)
            != static_cast<ssize_t>(sizeof(payload) - 1)) {
        close(input);
        unlink(inputPath);
        return fail("encoding stats owner fixture could not write input");
    }
    close(input);

    std::unique_ptr<FILE, decltype(&fclose)> output(tmpfile(), &fclose);
    if (!output) {
        unlink(inputPath);
        return fail("encoding stats owner fixture could not create output");
    }
    lString8 list;
    MakeStatsForFile(
            inputPath, "utf8", "en", 7, output.get(), list);
    unlink(inputPath);

    if (list.pos("ch_stat_utf8_en7") < 0
            || list.pos("dbl_ch_stat_utf8_en7") < 0)
        return fail("encoding stats scoped owners lost list output");
    if (fflush(output.get()) != 0
            || fseek(output.get(), 0, SEEK_END) != 0
            || ftell(output.get()) <= 0) {
        return fail("encoding stats scoped owners lost generated output");
    }
    return 0;
}

static int testWolBufferOwnership() {
    if (!LVRunWolBufferOwnershipRegression())
        return fail("WOL buffer ownership regression failed");
    return 0;
}

static int testFormatterWorkspaceOwnership() {
    if (!LVRunFormatterWorkspaceOwnershipRegression())
        return fail("formatter workspace ownership regression failed");
    if (!LVRunFormattedTextOwnershipRegression())
        return fail("formatted text graph ownership regression failed");
    formatted_text_fragment_t * cApiBuffer =
            lvtextAllocFormatter(123);
    lvtextAddSourceObject(
            cApiBuffer, NULL, 17, 19, 0,
            11, 0, 0, NULL, 0);
    const bool cApiValid = cApiBuffer
            && cApiBuffer->width == 123
            && cApiBuffer->srctextlen == 1
            && cApiBuffer->srctext[0].o.width == 17
            && cApiBuffer->srctext[0].o.height == 19;
    lvtextFreeFormatter(cApiBuffer);
    if (!cApiValid)
        return fail("formatted text C ownership boundary failed");

    std::unique_ptr<ldomDocument> document =
            std::make_unique<ldomDocument>();
    document->setSpaceWidthScalePercent(135);
    document->setMinSpaceCondensingPercent(75);
    document->setUnusedSpaceThresholdPercent(12);
    document->setMaxAddedLetterSpacingPercent(9);
    LFormattedTextRef first =
            document->createFormattedText();
    LFormattedTextRef alias = first;
    LFormattedTextRef second =
            document->createFormattedText();
    if (first.isNull() || alias.isNull() || second.isNull()
            || first.get() != alias.get()
            || first.get() == second.get()
            || first->GetBuffer()->space_width_scale_percent != 135
            || first->GetBuffer()->min_space_condensing_percent != 75
            || first->GetBuffer()->unused_space_threshold_percent != 12
            || first->GetBuffer()
                    ->max_added_letter_spacing_percent != 9)
        return fail("formatted text factory lost scoped ownership or options");
    first.Clear();
    if (alias.isNull()
            || alias->GetBuffer()
                    ->space_width_scale_percent != 135)
        return fail("formatted text factory alias lost owner lifetime");
    return 0;
}

static int descendingString32Comparator(
        lString32 &left, lString32 &right) {
    return right.compare(left);
}

static int testStringCollectionOwnership() {
    lString8Collection narrow;
    narrow.reserve(2);
    narrow.add("alpha");
    narrow.add("beta");
    narrow.add("gamma");
    lString8Collection narrowCopy(narrow);
    narrow.erase(1, 2);
    if (narrow.length() != 1
            || narrow[0] != lString8("alpha")
            || narrowCopy.length() != 3
            || narrowCopy[2] != lString8("gamma"))
        return fail("8-bit string collection copy/erase lost ownership");
    lString32Collection wideCopy;
    {
        lString32Collection wide;
        wide.add(U"charlie");
        wide.add(U"alpha");
        wide.add(U"bravo");
        wideCopy = wide;
        const int previousLength = wide.length();
        if (wide.insert(1, wide[0]) != previousLength
                || wide.length() != 4
                || wide[1] != lString32(U"charlie"))
            return fail("32-bit string collection aliased insert failed");
        wide.erase(0, wide.length());
        if (wide.length() != 0)
            return fail("32-bit string collection could not erase its tail");
    }
    if (wideCopy.length() != 3
            || wideCopy[0] != lString32(U"charlie")
            || wideCopy[2] != lString32(U"bravo"))
        return fail("32-bit string collection copy retained source storage");
    wideCopy.sort();
    if (wideCopy[0] != lString32(U"alpha")
            || wideCopy[2] != lString32(U"charlie"))
        return fail("32-bit string collection default sort changed order");
    wideCopy.sort(descendingString32Comparator);
    if (wideCopy[0] != lString32(U"charlie")
            || wideCopy[2] != lString32(U"alpha"))
        return fail("32-bit string collection custom sort changed order");

    static const int collisionBucketCount = 16;
    std::vector<lString32> firstByBucket(collisionBucketCount);
    std::vector<bool> bucketUsed(collisionBucketCount, false);
    lString32 collisionFirst;
    lString32 collisionSecond;
    for (int i = 0; i < 256 && collisionSecond.empty(); ++i) {
        lString32 candidate(U"collision-");
        candidate += lString32::itoa(i);
        const int bucket = static_cast<int>(
                calcStringHash(candidate.c_str()) % collisionBucketCount);
        if (bucketUsed[bucket]) {
            collisionFirst = firstByBucket[bucket];
            collisionSecond = candidate;
        } else {
            firstByBucket[bucket] = candidate;
            bucketUsed[bucket] = true;
        }
    }
    if (collisionSecond.empty())
        return fail("hashed string collision fixture was not built");

    lString32HashedCollection hashed(collisionBucketCount);
    const int firstIndex = hashed.add(collisionFirst.c_str());
    const int secondIndex = hashed.add(collisionSecond.c_str());
    if (firstIndex == secondIndex
            || hashed.find(collisionFirst.c_str()) != firstIndex
            || hashed.find(collisionSecond.c_str()) != secondIndex
            || hashed.add(collisionFirst.c_str()) != firstIndex)
        return fail("hashed string collision bucket lost an entry");
    for (int i = 0; i < 96; ++i) {
        lString32 value(U"value-");
        value += lString32::itoa(i);
        const int index = hashed.add(value.c_str());
        if (hashed.find(value.c_str()) != index)
            return fail("hashed string rehash lost an entry");
    }

    lString32HashedCollection hashedCopy(hashed);
    lString32HashedCollection hashedAssigned(2);
    hashedAssigned = hashed;
    hashed.clear();
    if (hashed.length() != 0
            || hashed.find(collisionFirst.c_str()) != -1
            || hashed.add(U"replacement") != 0
            || hashed.find(U"replacement") != 0
            || hashedCopy.find(collisionSecond.c_str()) != secondIndex
            || hashedAssigned.find(collisionFirst.c_str()) != firstIndex)
        return fail("hashed string copy/clear shared ownership state");

    SerialBuf serialized(1, true);
    hashedCopy.serialize(serialized);
    if (serialized.error())
        return fail("hashed string collection could not serialize");
    const int serializedSize = serialized.pos();
    std::vector<lUInt8> serializedBytes(
            serialized.buf(),
            serialized.buf() + serializedSize);
    serialized.reset();
    lString32HashedCollection restored(4);
    restored.add(U"stale");
    if (!restored.deserialize(serialized)
            || restored.find(U"stale") != -1
            || restored.find(collisionFirst.c_str()) != firstIndex
            || restored.find(collisionSecond.c_str()) != secondIndex)
        return fail("hashed string deserialize retained stale buckets");

    restored.add(U"rollback-sentinel");
    std::vector<lUInt8> wrongMagic(serializedBytes);
    wrongMagic[0] ^= 0x20;
    SerialBuf wrongMagicInput(
            wrongMagic.data(), static_cast<int>(wrongMagic.size()));
    if (restored.deserialize(wrongMagicInput)
            || restored.find(U"rollback-sentinel") < 0
            || restored.find(collisionFirst.c_str()) != firstIndex)
        return fail(
                "hashed string bad magic replaced committed buckets");

    SerialBuf truncatedInput(
            serializedBytes.data(), serializedSize - 1);
    if (restored.deserialize(truncatedInput)
            || restored.find(U"rollback-sentinel") < 0
            || restored.find(collisionSecond.c_str()) != secondIndex)
        return fail(
                "hashed string truncation published partial buckets");

    std::vector<lUInt8> oversizedCount(serializedBytes);
    for (int index = 4; index < 8; ++index)
        oversizedCount[static_cast<std::size_t>(index)] = 0xFF;
    SerialBuf oversizedCountInput(
            oversizedCount.data(),
            static_cast<int>(oversizedCount.size()));
    if (restored.deserialize(oversizedCountInput)
            || !oversizedCountInput.error()
            || restored.find(U"rollback-sentinel") < 0)
        return fail(
                "hashed string deserialize accepted an oversized count");
    return 0;
}

static int testStyleRecordSerializationOwnership() {
    css_style_rec_t source;
    source.important[0] = 0x10203040;
    source.importance[1] = 0x01020304;
    source.display = css_d_block;
    source.font_name = "Owned Style Face";
    source.font_size = css_length_t(css_val_screen_px, 27);
    source.margin[0] = css_length_t(css_val_px, 13);
    source.background_image = "images/background.png";
    source.content = U"owned content";
    SerialBuf serialized(1, true);
    if (!source.serialize(serialized) || serialized.error())
        return fail("style record fixture could not serialize");
    const int serializedSize = serialized.pos();
    std::vector<lUInt8> serializedBytes(
            serialized.buf(), serialized.buf() + serializedSize);

    css_style_rec_t committed;
    committed.display = css_d_none;
    committed.font_name = "Rollback Sentinel";
    committed.AddRef();
    committed.AddRef();
    committed.flags = STYLE_REC_FLAG_MATCHED;
    committed.pseudo_elem_before_style =
            std::make_unique<css_style_rec_t>();
    committed.pseudo_elem_after_style =
            std::make_unique<css_style_rec_t>();
    css_style_rec_t *beforeOwner =
            committed.pseudo_elem_before_style.get();
    css_style_rec_t *afterOwner =
            committed.pseudo_elem_after_style.get();

    std::vector<lUInt8> wrongMagic(serializedBytes);
    wrongMagic[0] ^= 0x20;
    SerialBuf wrongMagicInput(
            wrongMagic.data(), static_cast<int>(wrongMagic.size()));
    if (committed.deserialize(wrongMagicInput)
            || committed.display != css_d_none
            || committed.font_name != lString8("Rollback Sentinel")
            || committed.getRefCount() != 2
            || committed.pseudo_elem_before_style.get() != beforeOwner
            || committed.pseudo_elem_after_style.get() != afterOwner)
        return fail("style record bad magic replaced committed state");

    std::vector<lUInt8> wrongHash(serializedBytes);
    wrongHash.back() ^= 0x80;
    SerialBuf wrongHashInput(
            wrongHash.data(), static_cast<int>(wrongHash.size()));
    if (committed.deserialize(wrongHashInput)
            || committed.display != css_d_none
            || committed.font_name != lString8("Rollback Sentinel")
            || committed.getRefCount() != 2
            || committed.pseudo_elem_before_style.get() != beforeOwner
            || committed.pseudo_elem_after_style.get() != afterOwner)
        return fail("style record hash failure replaced committed state");

    SerialBuf validInput(
            serializedBytes.data(), static_cast<int>(serializedBytes.size()));
    if (!committed.deserialize(validInput)
            || !(committed == source)
            || committed.hash != source.hash
            || committed.getRefCount() != 2
            || committed.flags != STYLE_REC_FLAG_MATCHED
            || committed.pseudo_elem_before_style.get() != beforeOwner
            || committed.pseudo_elem_after_style.get() != afterOwner)
        return fail(
                "style record snapshot lost serialized or live-only state");
    return 0;
}

static int testStyleIndexRestoreOwnership() {
    if (!LVRunStyleIndexRestoreRegression())
        return fail("DOM style-index restore regression failed");
    return 0;
}

static int testDocumentCacheIndexRestoreOwnership() {
    if (!LVRunDocumentCacheIndexRestoreRegression())
        return fail("document-cache directory index restore regression failed");
    return 0;
}

static int testDocumentHeaderRestoreOwnership() {
    if (!LVRunDocumentHeaderRestoreRegression())
        return fail("document render-header restore regression failed");
    return 0;
}

static int testStringBufferOwnership() {
    if (!LVRunStringBufferOwnershipRegression())
        return fail("copy-on-write string buffer ownership regression failed");
    return 0;
}

#if (LDOM_USE_OWN_MEM_MAN == 1)
static int testStringChunkStorageOwnership() {
    if (!LVRunStringChunkStorageOwnershipRegression())
        return fail("string chunk slice ownership regression failed");
    return 0;
}

static int testDomBlockStorageOwnership() {
    if (!LVRunDomBlockStorageOwnershipRegression())
        return fail("DOM block storage ownership regression failed");
    return 0;
}
#endif

static int testNameIdMapOwnership() {
    css_elem_def_props_t props = {
        true, false, css_d_block, css_ws_normal
    };
    LDOMNameIdMap map(2);
    map.AddItem(1, lString32(U"alpha"), &props);
    props.allow_text = false;
    props.display = css_d_inline;
    map.AddItem(20, lString32(U"omega"), NULL);
    map.AddItem(20, lString32(U"duplicate"), NULL);

    const LDOMNameIdMapItem *alphaById =
            map.findItem(static_cast<lUInt16>(1));
    const LDOMNameIdMapItem *alphaByName = map.findItem(U"alpha");
    const css_elem_def_props_t *storedProps = map.dataById(1);
    if (!alphaById
            || alphaById != alphaByName
            || map.idByName("omega") != 20
            || map.idByName("duplicate") != 0
            || map.findItem(static_cast<lUInt16>(500)) != NULL
            || !storedProps
            || !storedProps->allow_text
            || storedProps->display != css_d_block)
        return fail("name/id map did not preserve one owned lookup item");

    LDOMNameIdMap mapCopy(map);
    LDOMNameIdMap mapAssigned(1);
    mapAssigned = map;
    const LDOMNameIdMapItem *copyAlpha =
            mapCopy.findItem(static_cast<lUInt16>(1));
    if (!copyAlpha
            || copyAlpha == alphaById
            || copyAlpha != mapCopy.findItem(U"alpha")
            || mapCopy.dataById(1) == storedProps
            || mapAssigned.idByName(U"omega") != 20)
        return fail("name/id map copy shared owner or name index");
    map.Clear();
    if (map.findItem(static_cast<lUInt16>(1)) != NULL
            || map.idByName(U"omega") != 0
            || mapCopy.idByName(U"alpha") != 1
            || mapAssigned.nameById(20) != lString32(U"omega"))
        return fail("name/id map clear invalidated an independent copy");

    SerialBuf serialized(1, true);
    mapCopy.serialize(serialized);
    if (serialized.error())
        return fail("name/id map could not serialize RAII storage");
    const int serializedSize = serialized.pos();

    serialized.reset();
    LDOMNameIdMap restored(32);
    restored.AddItem(2, lString32(U"stale"), NULL);
    if (!restored.deserialize(serialized)
            || restored.idByName(U"stale") != 0
            || restored.idByName(U"alpha") != 1
            || restored.idByName(U"omega") != 20
            || !restored.dataById(1)
            || restored.dataById(1)->display != css_d_block)
        return fail("name/id map round-trip lost items or metadata");

    std::vector<lUInt8> corrupted(
            serialized.buf(), serialized.buf() + serializedSize);
    corrupted.back() ^= 0x80;
    SerialBuf corruptedInput(corrupted.data(), corrupted.size());
    LDOMNameIdMap rollback(32);
    rollback.AddItem(3, lString32(U"sentinel"), NULL);
    if (rollback.deserialize(corruptedInput)
            || rollback.idByName(U"sentinel") != 3
            || rollback.idByName(U"alpha") != 0)
        return fail("name/id map failure replaced committed state");
    return 0;
}

static int testHashTableOwnership() {
    LVHashTable<lUInt32, lString32> table(1);
    table.set(14, lString32(U"fourteen"));
    table.set(30, lString32(U"thirty"));
    table.set(46, lString32(U"forty-six"));

    int iterated = 0;
    LVHashTable<lUInt32, lString32>::iterator collisionIterator =
            table.forwardIterator();
    while (collisionIterator.next())
        ++iterated;
    if (iterated != 3)
        return fail("hash iterator dropped a last-bucket collision chain");

    for (lUInt32 key = 100; key < 220; ++key) {
        lString32 value(U"value-");
        value += lString32::itoa(key);
        table.set(key, value);
    }
    table.set(14, lString32(U"updated"));
    table.remove(30);
    if (table.length() != 122
            || table.get(14) != lString32(U"updated")
            || table.get(30) != lString32()
            || table.get(219) != lString32(U"value-219"))
        return fail("hash resize/update/remove lost an entry");

    table.resize(1);
    iterated = 0;
    LVHashTable<lUInt32, lString32>::iterator compactIterator =
            table.forwardIterator();
    while (compactIterator.next())
        ++iterated;
    if (table.size() != 1 || iterated != table.length())
        return fail("hash explicit compact resize lost a collision node");

    LVHashTable<lUInt32, lString32> tableCopy(table);
    LVHashTable<lUInt32, lString32> tableAssigned(16);
    tableAssigned = table;
    LVHashTable<lUInt32, lString32>::pair *originalPair =
            table.forwardIterator().next();
    LVHashTable<lUInt32, lString32>::pair *copiedPair =
            tableCopy.forwardIterator().next();
    const bool copiesOwnDistinctNodes =
            originalPair && copiedPair && originalPair != copiedPair;
    table.clear();
    if (!copiesOwnDistinctNodes
            || table.length() != 0
            || tableCopy.get(14) != lString32(U"updated")
            || tableAssigned.get(219) != lString32(U"value-219"))
        return fail("hash copy/assignment shared collision-node ownership");

    if (HashResizeValue::liveCount() != 0)
        return fail("hash resize fixture started with live values");
    {
        LVHashTable<lUInt32, HashResizeValue> values(16);
        values.set(1, HashResizeValue(11));
        values.set(2, HashResizeValue(22));
        HashResizeValue::setCopyBlocked(true);
        bool resizeThrew = false;
        try {
            values.resize(64);
        } catch (const std::runtime_error &) {
            resizeThrew = true;
        }
        HashResizeValue::setCopyBlocked(false);
        if (resizeThrew
                || values.get(1).value() != 11
                || values.get(2).value() != 22)
            return fail("hash resize copied values or lost owned nodes");
        values.resize(0);
        values.set(3, HashResizeValue(33));
        if (values.size() < 3 || values.get(3).value() != 33)
            return fail("hash zero-size resize broke subsequent growth");
    }
    if (HashResizeValue::liveCount() != 0)
        return fail("hash clear/destructor leaked stored values");
    return 0;
}

static int testValueArrayOwnership() {
    LVArray<bool> bits;
    bits.add(true);
    bits.add(false);
    bits.set(4, true);
    if (bits.length() != 5
            || !bits[0] || bits[1] || bits[2] || bits[3] || !bits[4])
        return fail("value array sparse set did not initialize bool gaps");
    bits.reserve(32);
    bits.trim(1, 4, 8);
    bits.erase(1, 2);
    if (bits.length() != 2 || bits.size() != 8
            || bits[0] || !bits[1])
        return fail("value array bool reserve/trim/erase lost values");

    LVArray<int> values;
    values.add(1);
    values.add(2);
    values.add(3);
    values.append(values.ptr() + 1, 2);
    values.add(values);
    const int expected[] = {1, 2, 3, 2, 3, 1, 2, 3, 2, 3};
    if (values.length() != 10)
        return fail("value array aliased append changed the logical length");
    for (int i = 0; i < values.length(); ++i) {
        if (values[i] != expected[i])
            return fail("value array aliased append lost an element");
    }

    LVArray<int> copied(values);
    LVArray<int> assigned;
    assigned = values;
    LVArray<int> *assignedAlias = &assigned;
    assigned = *assignedAlias;
    values[0] = 99;
    if (copied[0] != 1 || assigned[0] != 1)
        return fail("value array copy/assignment shared backing storage");
    const int retainedSize = values.size();
    values.reset();
    if (!values.empty() || values.size() != retainedSize)
        return fail("value array reset did not retain backing storage");
    values.add(7);
    values.erase(-1, 1);
    values.trim(-1, 1, 0);
    if (values.length() != 1 || values[0] != 7 || values.remove(8) != 0)
        return fail("value array invalid range changed committed state");

    if (HashResizeValue::liveCount() != 0)
        return fail("value array exception fixture started with live values");
    {
        LVArray<HashResizeValue> source;
        source.add(HashResizeValue(11));
        source.add(HashResizeValue(22));
        const int originalSize = source.size();
        const int originalLiveCount = HashResizeValue::liveCount();
        HashResizeValue::setCopyBlocked(true);
        bool reserveThrew = false;
        try {
            source.reserve(originalSize + 5);
        } catch (const std::runtime_error &) {
            reserveThrew = true;
        }
        HashResizeValue::setCopyBlocked(false);
        if (!reserveThrew
                || source.size() != originalSize
                || source.length() != 2
                || source[0].value() != 11
                || source[1].value() != 22
                || HashResizeValue::liveCount() != originalLiveCount)
            return fail("value array failed reserve replaced committed storage");

        LVArray<HashResizeValue> target;
        target.add(HashResizeValue(99));
        HashResizeValue::setCopyBlocked(true);
        bool assignmentThrew = false;
        try {
            target = source;
        } catch (const std::runtime_error &) {
            assignmentThrew = true;
        }
        HashResizeValue::setCopyBlocked(false);
        if (!assignmentThrew
                || target.length() != 1
                || target[0].value() != 99)
            return fail("value array failed copy replaced assigned storage");
    }
    if (HashResizeValue::liveCount() != 0)
        return fail("value array backing storage leaked constructed values");

    ArrayTrackedValue::reset();
    {
        typedef LVFastRef<ArrayTrackedValue> TrackedRef;
        LVArray<TrackedRef> refs;
        refs.add(TrackedRef(new ArrayTrackedValue()));
        refs.add(TrackedRef(new ArrayTrackedValue()));
        const int reserved = refs.size();
        refs.erase(0, 1);
        if (ArrayTrackedValue::destroyed() != 1)
            return fail("value array erase retained an inactive reference");
        refs.reset();
        if (ArrayTrackedValue::destroyed() != 2
                || refs.size() != reserved || !refs.empty())
            return fail("value array reset retained active references");
        refs.add(TrackedRef(new ArrayTrackedValue()));
        refs.clear();
        if (ArrayTrackedValue::destroyed() != 3
                || refs.size() != 0 || !refs.empty())
            return fail("value array clear retained owned storage");
    }
    if (ArrayTrackedValue::destroyed() != 3)
        return fail("value array reference fixture escaped its lifecycle");
    return 0;
}

class RefAdoptionTestValue {
    int _value;
    static int _liveCount;

public:
    explicit RefAdoptionTestValue(int value)
        : _value(value)
    {
        ++_liveCount;
    }

    RefAdoptionTestValue(const RefAdoptionTestValue &value)
        : _value(value._value)
    {
        ++_liveCount;
    }

    ~RefAdoptionTestValue()
    {
        --_liveCount;
    }

    int value() const { return _value; }
    void setValue(int value) { _value = value; }
    static int liveCount() { return _liveCount; }
};

int RefAdoptionTestValue::_liveCount = 0;

static int testReferenceAdoptionOwnership() {
    bool constructorFailed = false;
    ref_count_rec_t::failNextAllocationForRegression();
    try {
        LVRef<RefAdoptionTestValue> rejected(
                new RefAdoptionTestValue(7));
    } catch (const std::bad_alloc &) {
        constructorFailed = true;
    }
    if (!constructorFailed
            || RefAdoptionTestValue::liveCount() != 0)
        return fail("LVRef constructor leaked a rejected adoption candidate");

    {
        LVRef<RefAdoptionTestValue> current(
                new RefAdoptionTestValue(11));
        bool assignmentFailed = false;
        ref_count_rec_t::failNextAllocationForRegression();
        try {
            current = new RefAdoptionTestValue(22);
        } catch (const std::bad_alloc &) {
            assignmentFailed = true;
        }
        if (!assignmentFailed
                || current->value() != 11
                || RefAdoptionTestValue::liveCount() != 1)
            return fail("LVRef assignment lost committed state on rejection");

        LVRef<RefAdoptionTestValue> cloned = current.clone();
        cloned->setValue(33);
        if (current->value() != 11
                || cloned->value() != 33
                || RefAdoptionTestValue::liveCount() != 2)
            return fail("LVRef clone did not publish an independent object");
    }
    if (RefAdoptionTestValue::liveCount() != 0)
        return fail("LVRef adoption owners survived final teardown");
    return 0;
}

static int testReferenceVectorOwnership() {
    if (RefCacheTestValue::liveCount() != 0)
        return fail("reference vector fixture started with live values");
    {
        LVRefVec<RefCacheTestValue> values;
        values.add(RefCacheTestRef(new RefCacheTestValue(11)));
        values.add(RefCacheTestRef(new RefCacheTestValue(22)));
        values.append(values.ptr(), values.length());
        values.add(values);
        if (values.length() != 8
                || values[0].isNull() || values[0]->value() != 11
                || values[1].isNull() || values[1]->value() != 22
                || values[4].get() != values[0].get()
                || values[5].get() != values[1].get()
                || RefCacheTestValue::liveCount() != 2)
            return fail("reference vector aliased append lost values");

        LVRefVec<RefCacheTestValue> copied(values);
        LVRefVec<RefCacheTestValue> assigned;
        assigned = values;
        LVRefVec<RefCacheTestValue> *assignedAlias = &assigned;
        assigned = *assignedAlias;
        if (copied.ptr() == values.ptr()
                || assigned.ptr() == values.ptr()
                || copied[0].get() != values[0].get()
                || assigned[1].get() != values[1].get())
            return fail("reference vector copy shared backing storage");
        values.clear();
        if (RefCacheTestValue::liveCount() != 2
                || copied.length() != 8 || assigned.length() != 8)
            return fail("reference vector clear invalidated independent copies");
    }
    if (RefCacheTestValue::liveCount() != 0)
        return fail("reference vector copies leaked referenced values");

    {
        LVRefVec<RefCacheTestValue> values;
        values.add(RefCacheTestRef(new RefCacheTestValue(31)));
        values.add(RefCacheTestRef(new RefCacheTestValue(32)));
        const int retainedSize = values.size();
        values.erase(0, 1);
        if (values.length() != 1 || values.size() != retainedSize
                || values[0].isNull() || values[0]->value() != 32
                || RefCacheTestValue::liveCount() != 1)
            return fail("reference vector erase retained an inactive value");

        values.set(3, RefCacheTestRef(new RefCacheTestValue(33)));
        if (values.length() != 4
                || !values[1].isNull() || !values[2].isNull()
                || values[3].isNull() || values[3]->value() != 33)
            return fail("reference vector sparse set did not initialize gaps");
        values.trim(3, 1, retainedSize);
        if (values.length() != 1 || values.size() != retainedSize
                || values[0].isNull() || values[0]->value() != 33
                || RefCacheTestValue::liveCount() != 1)
            return fail("reference vector trim corrupted reference storage");
        values.erase(-1, 1);
        values.trim(-1, 1, 0);
        if (values.length() != 1
                || values[0].isNull() || values[0]->value() != 33)
            return fail("reference vector invalid range changed state");
        values.clear();
        if (RefCacheTestValue::liveCount() != 0)
            return fail("reference vector clear retained a value");
    }

    {
        LVRefVec<RefCacheTestValue> values;
        RefCacheTestRef *slots = values.addSpace(2);
        slots[1] = RefCacheTestRef(new RefCacheTestValue(41));
        if (values.length() != 2 || !values[0].isNull()
                || values[1].isNull() || values[1]->value() != 41)
            return fail("reference vector addSpace did not publish null slots");
    }
    if (RefCacheTestValue::liveCount() != 0)
        return fail("reference vector addSpace leaked a value");

    {
        RefCacheTestRef shared(new RefCacheTestValue(51));
        LVRefVec<RefCacheTestValue> filled(3, shared);
        shared.Clear();
        if (filled.length() != 3
                || filled[0].get() != filled[2].get()
                || RefCacheTestValue::liveCount() != 1)
            return fail("reference vector fill construction lost references");
    }
    if (RefCacheTestValue::liveCount() != 0)
        return fail("reference vector fill storage leaked a value");
    return 0;
}

static int testPointerVectorOwnership() {
    if (PointerVectorTestValue::liveCount() != 0)
        return fail("pointer vector fixture started with live values");
    {
        LVPtrVector<PointerVectorTestValue> rejected;
        bool rejectedSetThrew = false;
        try {
            rejected.set(std::numeric_limits<int>::max(),
                    new PointerVectorTestValue(99));
        } catch (const std::length_error &) {
            rejectedSetThrew = true;
        }
        rejected.set(-1, new PointerVectorTestValue(98));
        if (!rejectedSetThrew || !rejected.empty()
                || PointerVectorTestValue::liveCount() != 0)
            return fail("owning pointer vector leaked rejected adoption candidates");
    }

    {
        LVPtrVector<PointerVectorTestValue> source;
        source.add(new PointerVectorTestValue(1));
        source.add(new PointerVectorTestValue(2));
        PointerVectorTestValue::setCopyBudget(1);
        bool copyThrew = false;
        try {
            LVPtrVector<PointerVectorTestValue> failedCopy(source);
        } catch (const std::runtime_error &) {
            copyThrew = true;
        }
        PointerVectorTestValue::resetCopies();
        if (!copyThrew || PointerVectorTestValue::liveCount() != 2)
            return fail("failed pointer vector copy leaked a partial clone");
    }
    if (PointerVectorTestValue::liveCount() != 0)
        return fail("pointer vector failed-copy source leaked values");

    {
        LVPtrVector<PointerVectorTestValue> values;
        values.add(new PointerVectorTestValue(3));
        values.pushHead(new PointerVectorTestValue(1));
        values.insert(1, new PointerVectorTestValue(2));
        values.reverse();
        values.move(0, 2);
        values.sort(pointerVectorValueComparator);
        PointerVectorTestValue *orderedFirst = values.first();
        PointerVectorTestValue *orderedMiddle = values[1];
        PointerVectorTestValue *orderedLast = values.last();
        if (values.length() != 3
                || !orderedFirst || orderedFirst->value() != 1
                || !orderedMiddle || orderedMiddle->value() != 2
                || !orderedLast || orderedLast->value() != 3)
            return fail("pointer vector ordering operations lost an item");

        PointerVectorTestValue *same = values[0];
        values.set(0, same);
        if (values[0] != same || PointerVectorTestValue::liveCount() != 3)
            return fail("pointer vector same-slot set deleted its item");
        values.set(0, new PointerVectorTestValue(4));
        values.set(5, new PointerVectorTestValue(6));
        if (values.length() != 6
                || !values[0] || values[0]->value() != 4
                || !values[1] || values[1]->value() != 2
                || !values[2] || values[2]->value() != 3
                || values[3] != NULL || values[4] != NULL
                || !values[5] || values[5]->value() != 6
                || PointerVectorTestValue::liveCount() != 4)
            return fail("pointer vector sparse set corrupted owned slots");

        LVPtrVector<PointerVectorTestValue> swapPeer;
        swapPeer.add(new PointerVectorTestValue(7));
        values.swap(swapPeer);
        if (values.length() != 1
                || !values[0] || values[0]->value() != 7
                || swapPeer.length() != 6
                || !swapPeer[0] || swapPeer[0]->value() != 4
                || PointerVectorTestValue::liveCount() != 5)
            return fail("pointer vector swap changed ownership");
        values.swap(swapPeer);
        if (values.length() != 6
                || swapPeer.length() != 1
                || !swapPeer[0] || swapPeer[0]->value() != 7)
            return fail("pointer vector swap could not restore storage");
        swapPeer.clear();
        if (PointerVectorTestValue::liveCount() != 4)
            return fail("pointer vector swap peer leaked its owner");

        PointerVectorTestValue::resetCopies();
        LVPtrVector<PointerVectorTestValue> copied(values);
        if (copied.length() != 6 || copied.size() != 6
                || !copied[0] || !copied[1] || !copied[5]
                || copied[0] == values[0] || copied[1] == values[1]
                || copied[0]->value() != 4 || copied[5]->value() != 6
                || copied[3] != NULL || copied[4] != NULL
                || PointerVectorTestValue::copyCount() != 4
                || PointerVectorTestValue::liveCount() != 8)
            return fail("owning pointer vector copy was not deep");
        values.clear();
        if (PointerVectorTestValue::liveCount() != 4)
            return fail("pointer vector clear invalidated its deep copy");

        PointerVectorTestValue *transferred = copied.remove(0);
        if (!transferred || transferred->value() != 4
                || copied.length() != 5 || copied.get()[5] != NULL
                || PointerVectorTestValue::liveCount() != 4)
            return fail("pointer vector remove did not transfer ownership");
        delete transferred;
        copied.erase(2, 2);
        if (copied.length() != 3
                || !copied[2] || copied[2]->value() != 6
                || PointerVectorTestValue::liveCount() != 3)
            return fail("pointer vector erase lost shifted ownership");
        transferred = copied.popHead();
        if (!transferred || transferred->value() != 2)
            return fail("pointer vector popHead returned the wrong owner");
        delete transferred;
        copied.erase(0, 1);
        transferred = copied.pop();
        if (!transferred || transferred->value() != 6
                || !copied.empty() || copied.get()[0] != NULL)
            return fail("pointer vector pop did not clear its inactive slot");
        delete transferred;
    }
    if (PointerVectorTestValue::liveCount() != 0)
        return fail("owning pointer vector lifecycle leaked values");

    {
        PointerVectorTestValue *first = new PointerVectorTestValue(11);
        PointerVectorTestValue *second = new PointerVectorTestValue(12);
        PointerVectorTestValue *rejected =
                new PointerVectorTestValue(13);
        LVPtrVector<PointerVectorTestValue, false> views;
        views.add(first);
        views.set(2, second);
        bool rejectedSetThrew = false;
        try {
            views.set(std::numeric_limits<int>::max(), rejected);
        } catch (const std::length_error &) {
            rejectedSetThrew = true;
        }
        if (!rejectedSetThrew
                || PointerVectorTestValue::liveCount() != 3) {
            delete rejected;
            delete first;
            delete second;
            return fail("borrowed pointer vector consumed a rejected view");
        }
        PointerVectorTestValue::resetCopies();
        PointerVectorTestValue::setCopyBudget(0);
        LVPtrVector<PointerVectorTestValue, false> copiedViews(views);
        PointerVectorTestValue::resetCopies();
        if (copiedViews.length() != 3
                || copiedViews[0] != first || copiedViews[1] != NULL
                || copiedViews[2] != second
                || PointerVectorTestValue::copyCount() != 0)
            return fail("borrowed pointer vector copy cloned its views");
        views.erase(0, 1);
        views.clear();
        copiedViews.clear();
        if (PointerVectorTestValue::liveCount() != 3)
            return fail("borrowed pointer vector deleted a viewed item");
        delete rejected;
        delete first;
        delete second;
    }
    if (PointerVectorTestValue::liveCount() != 0)
        return fail("borrowed pointer vector fixture leaked values");
    return 0;
}

static int testMatrixOwnership() {
    {
        MatrixIntProbe matrix;
        matrix.SetSize(2, 3, -1);
        if (matrix.rowCount() != 2 || matrix.columnCount() != 3
                || matrix.cellCount() != 6)
            return fail("matrix initial dimensions were not published");
        for (int row = 0; row < 2; ++row) {
            for (int column = 0; column < 3; ++column) {
                if (matrix[row][column] != -1)
                    return fail("matrix initial fill left an uninitialized cell");
            }
        }
        matrix[0][0] = 1;
        matrix[0][1] = 2;
        matrix[0][2] = 3;
        matrix[1][0] = 4;
        matrix[1][1] = 5;
        matrix[1][2] = 6;

        matrix.SetSize(3, 4, 9);
        const MatrixIntProbe &constMatrix = matrix;
        if (matrix.rowCount() != 3 || matrix.columnCount() != 4
                || matrix.cellCount() != 12
                || constMatrix[0][0] != 1 || constMatrix[0][2] != 3
                || constMatrix[0][3] != 9 || constMatrix[1][0] != 4
                || constMatrix[1][2] != 6 || constMatrix[1][3] != 9
                || constMatrix[2][0] != 9 || constMatrix[2][3] != 9)
            return fail("matrix growth lost retained or filled cells");

        matrix.SetSize(2, 2, 7);
        if (matrix.rowCount() != 2 || matrix.columnCount() != 2
                || matrix.cellCount() != 4
                || matrix[0][0] != 1 || matrix[0][1] != 2
                || matrix[1][0] != 4 || matrix[1][1] != 5)
            return fail("matrix shrink did not retain its overlap");

        MatrixIntProbe copied(matrix);
        MatrixIntProbe assigned;
        assigned = matrix;
        copied[0][0] = 100;
        assigned[1][1] = 200;
        if (matrix[0][0] != 1 || matrix[1][1] != 5
                || copied[0][0] != 100 || assigned[1][1] != 200)
            return fail("matrix copy shared its backing cells");

        MatrixIntProbe moved(std::move(copied));
        MatrixIntProbe moveAssigned;
        moveAssigned = std::move(assigned);
        if (copied.rowCount() != 0 || copied.columnCount() != 0
                || copied.cellCount() != 0
                || assigned.rowCount() != 0 || assigned.columnCount() != 0
                || assigned.cellCount() != 0
                || moved[0][0] != 100 || moveAssigned[1][1] != 200)
            return fail("matrix move left aliased or inconsistent storage");
        copied.SetSize(1, 1, 8);
        MatrixIntProbe *moveAssignedAlias = &moveAssigned;
        moveAssigned = std::move(*moveAssignedAlias);
        if (copied[0][0] != 8 || moveAssigned[1][1] != 200)
            return fail("moved-from or self-moved matrix was not reusable");

        bool overflowRejected = false;
        try {
            matrix.SetSize(INT_MAX, INT_MAX, 0);
        } catch (const std::length_error &) {
            overflowRejected = true;
        }
        if (!overflowRejected
                || matrix.rowCount() != 2 || matrix.columnCount() != 2
                || matrix[0][0] != 1 || matrix[1][1] != 5)
            return fail("matrix dimension overflow changed live storage");

        matrix.SetSize(-1, 2, 0);
        if (matrix.rowCount() != 0 || matrix.columnCount() != 0
                || matrix.cellCount() != 0)
            return fail("matrix invalid dimensions did not clear storage");
    }

    if (MatrixTestValue::liveCount() != 0)
        return fail("matrix fixture started with live values");
    {
        MatrixTestValue initial(7);
        MatrixTestValue changed(3);
        MatrixTestValue fill(9);
        LVMatrix<MatrixTestValue> matrix;
        matrix.SetSize(2, 2, initial);
        matrix[0][0] = changed;

        MatrixTestValue::setAssignmentBudget(0);
        bool resizeThrew = false;
        try {
            matrix.SetSize(3, 3, fill);
        } catch (const std::runtime_error &) {
            resizeThrew = true;
        }
        MatrixTestValue::setAssignmentBudget(-1);
        if (!resizeThrew || matrix[0][0].value() != 3
                || matrix[0][1].value() != 7
                || matrix[1][0].value() != 7
                || MatrixTestValue::liveCount() != 7)
            return fail("failed matrix resize changed or leaked live cells");

        matrix.SetSize(3, 3, fill);
        if (matrix[0][0].value() != 3
                || matrix[0][1].value() != 7
                || matrix[2][2].value() != 9)
            return fail("matrix value resize lost constructed cells");
        matrix.Clear();
        if (MatrixTestValue::liveCount() != 3)
            return fail("matrix clear retained constructed cells");
    }
    if (MatrixTestValue::liveCount() != 0)
        return fail("matrix value lifecycle leaked cells");
    return 0;
}

static int testPaginationAuxiliaryOwnership() {
    {
        CompactArray<int, 1, 2> values;
        values.reserve(-1);
        values.add(static_cast<int *>(NULL), 0);
        values.add(static_cast<int *>(NULL), 1);
        values.add(1);
        values.add(2);
        int tail[] = { 3, 4 };
        values.add(tail, 2);
        values.reserve(5);
        values.add(&values[1], 3);
        if (values.length() != 7
                || values[0] != 1 || values[1] != 2
                || values[2] != 3 || values[3] != 4
                || values[4] != 2 || values[5] != 3
                || values[6] != 4)
            return fail("compact array aliased append lost values");

        LVArray<int> extra(2, 8);
        values.add(extra);
        const CompactArray<int, 1, 2> &constValues = values;
        if (values.length() != 9
                || constValues.get(7) != 8 || constValues[8] != 8)
            return fail("compact array LVArray append lost values");

        CompactArray<int, 1, 2> copied(values);
        CompactArray<int, 1, 2> assigned;
        assigned = values;
        CompactArray<int, 1, 2> *assignedAlias = &assigned;
        assigned = *assignedAlias;
        values[0] = 99;
        copied[1] = 77;
        assigned[2] = 66;
        if (copied[0] != 1 || assigned[0] != 1
                || values[1] != 2 || values[2] != 3
                || copied[1] != 77 || assigned[2] != 66)
            return fail("compact array copy shared backing storage");

        CompactArray<int, 1, 2> moved(std::move(copied));
        CompactArray<int, 1, 2> moveAssigned;
        moveAssigned = std::move(assigned);
        if (!copied.empty() || !assigned.empty()
                || moved.length() != 9 || moved[1] != 77
                || moveAssigned.length() != 9
                || moveAssigned[2] != 66)
            return fail("compact array move retained shared storage");
        values.clear();
        if (!values.empty() || values.length() != 0
                || moved.length() != 9 || moveAssigned.length() != 9)
            return fail("compact array clear invalidated independent copies");
    }

    {
        LVRendPageInfo page(100, 200, 3);
        page.footnotes.add(LVPageFootNoteInfo(10, 20));
        page.footnotes.add(LVPageFootNoteInfo(30, 40));
        LVRendPageInfo copiedPage(page);
        page.footnotes[0].start = 99;
        if (copiedPage.footnotes.length() != 2
                || copiedPage.footnotes[0].start != 10
                || copiedPage.footnotes[1].height != 40)
            return fail("rendered page copy shared compact footnotes");
    }

    {
        LVRendPageInfo serializedPage(100, 200, 3);
        serializedPage.flags = RN_PAGE_TYPE_COVER;
        serializedPage.flow = 2;
        serializedPage.footnotes.add(
                LVPageFootNoteInfo(10, 20));
        SerialBuf serialized(1, true);
        if (!serializedPage.serialize(serialized)
                || serialized.error())
            return fail("rendered page fixture could not serialize");

        LVRendPageInfo committed(700, 80, 9);
        committed.flags = RN_PAGE_TYPE_NORMAL;
        committed.flow = 7;
        committed.footnotes.add(
                LVPageFootNoteInfo(30, 40));
        SerialBuf truncated(
                serialized.buf(), serialized.pos() - 1);
        if (committed.deserialize(truncated)
                || !truncated.error()
                || committed.start != 700
                || committed.height != 80
                || committed.index != 9
                || committed.flow != 7
                || committed.footnotes.length() != 1
                || committed.footnotes[0].start != 30)
            return fail(
                    "rendered page truncation replaced committed state");
    }

    {
        LVRendPageList source;
        source.add(new LVRendPageInfo(100, 200, 0));
        source[0]->footnotes.add(
                LVPageFootNoteInfo(10, 20));
        source.add(new LVRendPageInfo(300, 150, 1));
        source[1]->flow = 2;

        SerialBuf serialized(1, true);
        if (!source.serialize(serialized)
                || serialized.error())
            return fail("rendered page-list fixture could not serialize");
        const int serializedSize = serialized.pos();
        std::vector<lUInt8> serializedBytes(
                serialized.buf(),
                serialized.buf() + serializedSize);

        LVRendPageList restored;
        restored.add(new LVRendPageInfo(900, 90, 0));
        SerialBuf input(
                serializedBytes.data(), serializedSize);
        if (!restored.deserialize(input)
                || restored.length() != 2
                || restored[0]->start != 100
                || restored[0]->index != 0
                || restored[0]->footnotes.length() != 1
                || restored[1]->start != 300
                || restored[1]->index != 1
                || restored[1]->flow != 2
                || !restored.hasNonLinearFlows())
            return fail(
                    "rendered page-list round-trip lost owned state");

        std::vector<lUInt8> corrupted(serializedBytes);
        corrupted.back() ^= 0x80;
        SerialBuf corruptedInput(
                corrupted.data(),
                static_cast<int>(corrupted.size()));
        if (restored.deserialize(corruptedInput)
                || restored.length() != 2
                || restored[0]->start != 100
                || restored[1]->start != 300
                || !restored.hasNonLinearFlows())
            return fail(
                    "rendered page-list CRC failure replaced committed state");

        std::vector<lUInt8> oversizedCount(serializedBytes);
        for (int index = 8; index < 12; ++index)
            oversizedCount[static_cast<size_t>(index)] = 0xFF;
        SerialBuf oversizedInput(
                oversizedCount.data(),
                static_cast<int>(oversizedCount.size()));
        if (restored.deserialize(oversizedInput)
                || !oversizedInput.error()
                || restored.length() != 2
                || restored[0]->start != 100
                || restored[1]->start != 300)
            return fail(
                    "rendered page-list accepted an oversized count");

        LVRendPageList linearSource;
        linearSource.add(new LVRendPageInfo(500, 100, 0));
        SerialBuf linearSerialized(1, true);
        if (!linearSource.serialize(linearSerialized)
                || linearSerialized.error())
            return fail(
                    "linear rendered page-list fixture could not serialize");
        SerialBuf linearInput(
                linearSerialized.buf(), linearSerialized.pos());
        if (!restored.deserialize(linearInput)
                || restored.length() != 1
                || restored[0]->start != 500
                || restored.hasNonLinearFlows())
            return fail(
                    "rendered page-list retained stale nonlinear-flow state");
    }

    {
        LVFootNoteRef first(
                new LVFootNote(lString32(U"first")));
        LVFootNoteRef second(
                new LVFootNote(lString32(U"second")));
        LVRendLineInfo line(10, 20, RN_SPLIT_ALWAYS, 3);
        line.addLink(first.get());
        line.addLink(second.get(), 0);
        if (line.getLinksCount() != 2
                || !line.getLinks()
                || line.getLinks()->get(0) != second.get()
                || line.getLinks()->get(1) != first.get()
                || !(line.getFlags() & RN_SPLIT_FOOT_LINK))
            return fail("rendered line link insertion lost a footnote");

        LVRendLineInfo copied(line);
        LVRendLineInfo assigned;
        assigned = line;
        LVRendLineInfo *assignedAlias = &assigned;
        assigned = *assignedAlias;
        if (!copied.getLinks() || !assigned.getLinks()
                || copied.getLinks() == line.getLinks()
                || assigned.getLinks() == line.getLinks()
                || copied.getLinksCount() != 2
                || assigned.getLinksCount() != 2
                || copied.getStart() != 10 || copied.getEnd() != 20
                || copied.flow != 3)
            return fail("rendered line copy shared its link-list owner");

        copied.getLinks()->erase(0, 1);
        if (copied.getLinksCount() != 1
                || line.getLinksCount() != 2
                || assigned.getLinksCount() != 2)
            return fail("rendered line link-list copy was not independent");

        LVRendLineInfo moved(std::move(copied));
        LVRendLineInfo moveAssigned;
        moveAssigned = std::move(assigned);
        if (copied.getLinks() || assigned.getLinks()
                || moved.getLinksCount() != 1
                || moveAssigned.getLinksCount() != 2)
            return fail("rendered line move retained a link-list alias");
        line.clear();
        if (!line.empty() || line.getLinks()
                || moved.getLinksCount() != 1
                || moveAssigned.getLinksCount() != 2)
            return fail("rendered line clear invalidated moved link lists");
    }

    {
        LVRendPageList pages;
        LVRendPageContext context(&pages, 100, 16, true);
        context.AddLine(0, 250, RN_SPLIT_AUTO);
        context.Finalize();
        if (pages.length() != 3
                || pages[0]->start != 0 || pages[0]->height != 100
                || pages[1]->start != 100 || pages[1]->height != 100
                || pages[2]->start != 200 || pages[2]->height != 50
                || pages[0]->index != 0
                || pages[1]->index != 1
                || pages[2]->index != 2)
            return fail(
                    "live page-split candidates lost overflow-line ownership");
    }
    return 0;
}

static int testImporterTransientOwnership() {
    std::vector<lUInt8> payload(600000);
    for (size_t index = 0; index < payload.size(); ++index) {
        payload[index] = static_cast<lUInt8>(
                (index * 31 + index / 257) & 0xFF);
    }
    uLongf compressedSize =
            compressBound(static_cast<uLong>(payload.size()));
    std::vector<lUInt8> compressed(compressedSize);
    if (compress2(
                compressed.data(),
                &compressedSize,
                payload.data(),
                static_cast<uLong>(payload.size()),
                Z_BEST_SPEED) != Z_OK)
        return fail("PDB inflate fixture compression failed");
    compressed.resize(static_cast<size_t>(compressedSize));

    std::vector<lUInt8> uncompressed(3, 0xFF);
    if (!LVInflatePDBBuffer(
                compressed.data(), compressed.size(), uncompressed)
            || uncompressed != payload)
        return fail("PDB inflate lost a multi-chunk payload");

    std::vector<lUInt8> truncated(
            compressed.begin(), compressed.end() - 4);
    std::vector<lUInt8> sentinel = { 7, 8, 9 };
    if (LVInflatePDBBuffer(
                truncated.data(), truncated.size(), sentinel)
            || sentinel != std::vector<lUInt8>({ 7, 8, 9 }))
        return fail("failed PDB inflate published partial output");
    if (LVInflatePDBBuffer(NULL, 1, sentinel)
            || sentinel != std::vector<lUInt8>({ 7, 8, 9 }))
        return fail("invalid PDB inflate input changed output");

    lUInt8 invalidPdb[96] = {};
    LVStreamRef detectionStream = LVCreateMemoryStream(
            invalidPdb, sizeof(invalidPdb), true);
    doc_format_t contentFormat = doc_format_none;
    if (DetectPDBFormat(detectionStream, contentFormat))
        return fail("invalid PDB unexpectedly passed detection");

    LVStreamRef coverStream = LVCreateMemoryStream(
            invalidPdb, sizeof(invalidPdb), true);
    if (!GetPDBCoverpage(coverStream).isNull())
        return fail("invalid PDB unexpectedly returned a cover");

    LVStreamRef importStream = LVCreateMemoryStream(
            invalidPdb, sizeof(invalidPdb), true);
    std::unique_ptr<ldomDocument> document(new ldomDocument());
    if (ImportPDBDocument(
                importStream,
                document.get(),
                NULL,
                NULL,
                contentFormat))
        return fail("invalid PDB unexpectedly imported");
    return 0;
}

static int testReferenceCacheOwnership() {
    if (RefCacheTestValue::liveCount() != 0)
        return fail("reference cache fixture started with live values");
    {
        LVRefCache<RefCacheTestRef> cache(0);
        RefCacheTestRef empty;
        cache.cacheIt(empty);

        RefCacheTestRef first(new RefCacheTestValue(11));
        RefCacheTestRef duplicate(new RefCacheTestValue(11));
        RefCacheTestRef collision(new RefCacheTestValue(22));
        cache.cacheIt(first);
        cache.cacheIt(duplicate);
        cache.cacheIt(collision);
        if (duplicate.get() != first.get()
                || collision.get() == first.get()
                || RefCacheTestValue::liveCount() != 2)
            return fail("reference cache did not canonicalize a collision");

        first.Clear();
        duplicate.Clear();
        collision.Clear();
        cache.gc();
        if (RefCacheTestValue::liveCount() != 0)
            return fail("reference cache GC retained a collision chain");
    }
    if (RefCacheTestValue::liveCount() != 0)
        return fail("reference cache teardown leaked a value");

    {
        LVIndexedRefCache<RefCacheTestRef> cache(0);
        RefCacheTestRef empty;
        if (cache.cache(empty) != 0 || cache.length() != 0)
            return fail("indexed reference cache stored a null value");

        RefCacheTestRef first(new RefCacheTestValue(31));
        RefCacheTestRef duplicate(new RefCacheTestValue(31));
        RefCacheTestRef collision(new RefCacheTestValue(32));
        const int firstIndex = cache.cache(first);
        const int duplicateIndex = cache.cache(duplicate);
        const int collisionIndex = cache.cache(collision);
        if (firstIndex != 1 || duplicateIndex != firstIndex
                || collisionIndex != 2
                || duplicate.get() != first.get()
                || cache.length() != 2)
            return fail("indexed reference cache lost collision/index state");

        cache.release(firstIndex);
        if (cache.length() != 2 || cache.get(firstIndex).isNull())
            return fail("indexed reference cache released a shared index early");
        cache.release(firstIndex);
        if (cache.length() != 1 || !cache.get(firstIndex).isNull())
            return fail("indexed reference cache retained an unreferenced index");

        RefCacheTestRef reused(new RefCacheTestValue(33));
        if (cache.cache(reused) != firstIndex)
            return fail("indexed reference cache did not reuse a free index");
        if (!cache.addIndexRef(static_cast<lUInt16>(firstIndex)))
            return fail("indexed reference cache rejected a live index");
        cache.release(firstIndex);
        cache.release(firstIndex);

        lUInt16 holder = 0;
        RefCacheTestRef held(new RefCacheTestValue(34));
        if (!cache.cache(holder, held) || holder == 0)
            return fail("indexed reference cache did not publish an index holder");
        const int heldLength = cache.length();
        if (cache.cache(holder, held) || cache.length() != heldLength)
            return fail("indexed reference cache duplicated an index holder");
        cache.release(holder);

        cache.clear(-1);
        if (cache.length() != 0 || !cache.get(collisionIndex).isNull())
            return fail("indexed reference cache clear retained index views");
    }
    if (RefCacheTestValue::liveCount() != 0)
        return fail("indexed reference cache teardown leaked a value");

    {
        LVArray<RefCacheTestRef> saved(5, RefCacheTestRef());
        saved.set(1, RefCacheTestRef(new RefCacheTestValue(41)));
        saved.set(3, RefCacheTestRef(new RefCacheTestValue(43)));
        LVIndexedRefCache<RefCacheTestRef> cache(saved);
        RefCacheTestRef restoredFirst = cache.get(1);
        RefCacheTestRef restoredThird = cache.get(3);
        if (cache.length() != 2
                || restoredFirst.isNull() || restoredFirst->value() != 41
                || restoredThird.isNull() || restoredThird->value() != 43)
            return fail("indexed reference cache restore lost sparse entries");

        std::unique_ptr<LVArray<RefCacheTestRef> > roundTrip =
                cache.getIndex();
        RefCacheTestRef roundTripFirst = roundTrip->get(1);
        RefCacheTestRef roundTripThird = roundTrip->get(3);
        if (roundTrip->length() != 5
                || roundTripFirst.isNull() || roundTripFirst->value() != 41
                || !roundTrip->get(2).isNull()
                || roundTripThird.isNull() || roundTripThird->value() != 43
                || !roundTrip->get(4).isNull())
            return fail("indexed reference cache export lost sparse entries");

        cache.release(1);
        RefCacheTestRef replacementValue(new RefCacheTestValue(44));
        if (cache.cache(replacementValue) != 1)
            return fail("restored reference cache did not reuse a freed index");

        LVArray<RefCacheTestRef> failingReplacement(4, RefCacheTestRef());
        failingReplacement.set(
                1, RefCacheTestRef(new RefCacheTestValue(71)));
        failingReplacement.set(
                2, RefCacheTestRef(new RefCacheTestValue(99)));
        bool replacementThrew = false;
        try {
            cache.setIndex(failingReplacement);
        } catch (const std::runtime_error &) {
            replacementThrew = true;
        }
        RefCacheTestRef retainedFirst = cache.get(1);
        RefCacheTestRef retainedThird = cache.get(3);
        if (!replacementThrew || cache.length() != 2
                || retainedFirst.isNull() || retainedFirst->value() != 44
                || retainedThird.isNull() || retainedThird->value() != 43)
            return fail("failed reference cache restore replaced committed state");

        LVArray<RefCacheTestRef> replacement(3, RefCacheTestRef());
        replacement.set(2, RefCacheTestRef(new RefCacheTestValue(52)));
        cache.setIndex(replacement);
        RefCacheTestRef replacementSecond = cache.get(2);
        if (cache.length() != 1
                || !cache.get(1).isNull()
                || replacementSecond.isNull()
                || replacementSecond->value() != 52)
            return fail("indexed reference cache replacement mixed generations");
        std::unique_ptr<LVArray<RefCacheTestRef> > replacedIndex =
                cache.getIndex();
        RefCacheTestRef exportedSecond = replacedIndex->get(2);
        if (replacedIndex->length() != 3
                || exportedSecond.isNull() || exportedSecond->value() != 52)
            return fail("indexed reference cache replacement export failed");

        cache.clear(3);
        RefCacheTestRef afterClear(new RefCacheTestValue(61));
        if (cache.cache(afterClear) != 1 || cache.length() != 1)
            return fail("indexed reference cache failed after resized clear");
    }
    if (RefCacheTestValue::liveCount() != 0)
        return fail("indexed reference cache round-trip leaked values");
    return 0;
}

static int testRtfTextBufferOwnership() {
    RecordingRtfCallback callback;
    bool recognized = false;
    bool parsed = false;
    {
        LVRtfParser parser(memoryStream(
                "{\\rtf1\\ansi trailing text"), &callback);
        recognized = parser.CheckFormat();
        parsed = parser.Parse();
    }

    if (!recognized)
        return fail("RTF parser rejected a truncated RTF header");
    if (!parsed)
        return fail("RTF parser rejected a recoverable truncated document");
    if (callback.text.pos(U"trailing text") < 0)
        return fail("RTF parser discarded its final buffered text");
    if (!callback.stopped || callback.stopCount != 1)
        return fail("RTF parser did not finish its callback lifecycle once");
    if (callback.callbackAfterStop)
        return fail("RTF parser emitted callbacks after OnStop");

    RecordingRtfCallback pictureCallback;
    {
        LVRtfParser parser(memoryStream(
                "{\\rtf1{\\pict\\pngblip 89504e47"), &pictureCallback);
        if (!parser.CheckFormat() || !parser.Parse())
            return fail("RTF parser rejected a truncated picture group");
    }
    if (pictureCallback.blobCount != 1)
        return fail("RTF parser did not finalize an open picture destination");
    if (!pictureCallback.stopped || pictureCallback.stopCount != 1
            || pictureCallback.callbackAfterStop)
        return fail("RTF destination teardown escaped the callback lifecycle");
    return 0;
}

static int testParserFormatDetectionBuffers() {
    NoOpXmlCallback callback;

    const std::string fb2 =
            "<?xml version=\"1.0\" encoding=\"utf-8\"?>"
            "<FictionBook><body/></FictionBook>";
    LVXMLParser xmlParser(memoryStream(fb2), &callback, false, true);
    if (!xmlParser.CheckFormat())
        return fail("XML format detector rejected valid FB2");
    LVXMLParser nonXmlParser(memoryStream(
            "ordinary text without any XML markup at all"), &callback,
            false, true);
    if (nonXmlParser.CheckFormat())
        return fail("XML format detector accepted plain text");

    const std::string html =
            "<!doctype html><html><head><title>x</title></head>"
            "<body>text</body></html>";
    LVHTMLParser htmlParser(memoryStream(html), &callback);
    if (!htmlParser.CheckFormat())
        return fail("HTML format detector rejected valid HTML");
    LVHTMLParser nonHtmlParser(memoryStream(
            "ordinary text without any HTML markup at all"), &callback);
    if (nonHtmlParser.CheckFormat())
        return fail("HTML format detector accepted plain text");

    LVTextParser textParser(memoryStream(
            "plain text line one\nplain text line two\n"), &callback, false);
    if (!textParser.CheckFormat())
        return fail("text format detector rejected valid plain text");
    LVTextParser nonTextParser(memoryStream(
            "abcdefghijklmnopqrstuvwxyz"), &callback, false);
    if (nonTextParser.CheckFormat())
        return fail("text format detector accepted text without separators");

    LVStreamRef autodetectStream = memoryStream(
            "plain text line one\nplain text line two\n");
    autodetectStream->SetPos(7);
    LVTextParser autodetectParser(autodetectStream, &callback, false);
    if (!autodetectParser.AutodetectEncoding()
            || autodetectStream->GetPos() != 7)
        return fail("encoding detector did not restore a valid stream");

    LVStreamRef shortStream = memoryStream("short input");
    shortStream->SetPos(5);
    LVTextParser shortParser(shortStream, &callback, false);
    if (shortParser.AutodetectEncoding() || shortStream->GetPos() != 5)
        return fail("encoding detector did not restore a short stream");

    const std::string bookmark =
            "\xEF\xBB\xBF# Cool Reader 3 - exported bookmarks\r\n"
            "# file name: sample.fb2\r\n";
    LVTextBookmarkParser bookmarkParser(memoryStream(bookmark), &callback);
    if (!bookmarkParser.CheckFormat())
        return fail("bookmark format detector rejected a valid header");
    LVTextBookmarkParser nonBookmarkParser(memoryStream(
            "# Cool Reader exported bookmarks\r\n"
            "# file name: sample.fb2\r\n"), &callback);
    if (nonBookmarkParser.CheckFormat())
        return fail("bookmark format detector accepted an invalid header");

    return 0;
}

class CountingImageDecodeCallback : public LVImageDecoderCallback {
public:
    int starts = 0;
    int lines = 0;
    int ends = 0;
    int errorEnds = 0;

    void OnStartDecode(LVImageSource *) override
    {
        ++starts;
    }

    bool OnLineDecoded(LVImageSource *, int, lUInt32 *data) override
    {
        if (data)
            ++lines;
        return data != NULL;
    }

    void OnEndDecode(LVImageSource *, bool errors) override
    {
        if (errors)
            ++errorEnds;
        else
            ++ends;
    }
};

class RejectingImageDecodeCallback : public LVImageDecoderCallback {
public:
    int starts = 0;
    int lines = 0;
    int errorEnds = 0;

    void OnStartDecode(LVImageSource *) override
    {
        ++starts;
    }

    bool OnLineDecoded(LVImageSource *, int, lUInt32 *) override
    {
        ++lines;
        return false;
    }

    void OnEndDecode(LVImageSource *, bool errors) override
    {
        if (errors)
            ++errorEnds;
    }
};

class FailingImageSource : public LVImageSource {
private:
    int &_decodeCalls;

public:
    explicit FailingImageSource(int &decodeCalls)
        : _decodeCalls(decodeCalls)
    {
    }

    ldomNode *GetSourceNode() override { return NULL; }
    LVStream *GetSourceStream() override { return NULL; }
    void Compact() override {}
    int GetWidth() const override { return 2; }
    int GetHeight() const override { return 2; }

    bool Decode(LVImageDecoderCallback *callback) override
    {
        ++_decodeCalls;
        if (callback) {
            lUInt32 row[] = {0x000000, 0xffffff};
            callback->OnStartDecode(this);
            callback->OnLineDecoded(this, 0, row);
            callback->OnEndDecode(this, true);
        }
        return false;
    }
};

class AbortedImageSource : public LVImageSource {
public:
    ldomNode *GetSourceNode() override { return NULL; }
    LVStream *GetSourceStream() override { return NULL; }
    void Compact() override {}
    int GetWidth() const override { return 2; }
    int GetHeight() const override { return 2; }

    bool Decode(LVImageDecoderCallback *callback) override
    {
        if (callback) {
            lUInt32 row[] = {0x000000, 0xffffff};
            callback->OnStartDecode(this);
            callback->OnLineDecoded(this, 0, row);
        }
        return false;
    }
};

class DimensionOnlyImageSource : public LVImageSource {
private:
    int _width;
    int _height;
    int &_decodeCalls;

public:
    DimensionOnlyImageSource(
            int width, int height, int &decodeCalls)
        : _width(width), _height(height),
          _decodeCalls(decodeCalls)
    {
    }

    ldomNode *GetSourceNode() override { return NULL; }
    LVStream *GetSourceStream() override { return NULL; }
    void Compact() override {}
    int GetWidth() const override { return _width; }
    int GetHeight() const override { return _height; }

    bool Decode(LVImageDecoderCallback *) override
    {
        ++_decodeCalls;
        return false;
    }
};

class NinePatchFixtureImageSource : public LVImageSource {
private:
    bool _markers;
    bool _decodeResult;
    int _decodeCalls;

public:
    NinePatchFixtureImageSource(bool markers, bool decodeResult)
        : _markers(markers), _decodeResult(decodeResult), _decodeCalls(0)
    {
    }

    int decodeCalls() const { return _decodeCalls; }
    ldomNode *GetSourceNode() override { return NULL; }
    LVStream *GetSourceStream() override { return NULL; }
    void Compact() override {}
    int GetWidth() const override { return 6; }
    int GetHeight() const override { return 6; }

    bool Decode(LVImageDecoderCallback *callback) override
    {
        ++_decodeCalls;
        if (!callback)
            return false;
        callback->OnStartDecode(this);
        for (int y = 0; y < 6; ++y) {
            lUInt32 row[] = {
                0xffffff, 0xffffff, 0xffffff,
                0xffffff, 0xffffff, 0xffffff
            };
            if (_markers) {
                if (y == 0)
                    row[2] = row[3] = 0x000000;
                if (y == 5)
                    row[1] = row[2] = row[3] = row[4] = 0x000000;
                if (y == 2 || y == 3)
                    row[0] = 0x000000;
                if (y >= 1 && y <= 4)
                    row[5] = 0x000000;
            }
            if (!callback->OnLineDecoded(this, y, row)) {
                callback->OnEndDecode(this, true);
                return false;
            }
        }
        callback->OnEndDecode(this, !_decodeResult);
        return _decodeResult;
    }
};

class CapturingImageDecodeCallback : public LVImageDecoderCallback {
private:
    LVImageSource *_expectedLifecycleSource;
    LVImageSource *_expectedLineSource;
    int _width;

public:
    int starts = 0;
    int lines = 0;
    int ends = 0;
    int errorEnds = 0;
    bool sourceMismatch = false;
    std::vector<lUInt32> pixels;

    CapturingImageDecodeCallback(
            LVImageSource *expectedLifecycleSource,
            LVImageSource *expectedLineSource, int width)
        : _expectedLifecycleSource(expectedLifecycleSource)
        , _expectedLineSource(expectedLineSource), _width(width)
    {
    }

    void OnStartDecode(LVImageSource *source) override
    {
        ++starts;
        sourceMismatch =
                sourceMismatch || source != _expectedLifecycleSource;
    }

    bool OnLineDecoded(
            LVImageSource *source, int, lUInt32 *data) override
    {
        ++lines;
        sourceMismatch = sourceMismatch || source != _expectedLineSource;
        if (!data)
            return false;
        pixels.insert(pixels.end(), data, data + _width);
        return true;
    }

    void OnEndDecode(LVImageSource *source, bool errors) override
    {
        sourceMismatch =
                sourceMismatch || source != _expectedLifecycleSource;
        if (errors)
            ++errorEnds;
        else
            ++ends;
    }
};

class ThrowingImageDecodeCallback : public LVImageDecoderCallback {
public:
    void OnStartDecode(LVImageSource *) override
    {
        throw std::runtime_error("blocked image callback");
    }

    bool OnLineDecoded(LVImageSource *, int, lUInt32 *) override
    {
        return true;
    }

    void OnEndDecode(LVImageSource *, bool) override
    {
    }
};

#if (USE_LIBJPEG==1)
static std::vector<unsigned char> buildJpegFixture() {
    static const int width = 128;
    static const int height = 128;
    jpeg_compress_struct encoder;
    jpeg_error_mgr error;
    std::memset(&encoder, 0, sizeof(encoder));
    encoder.err = jpeg_std_error(&error);
    jpeg_create_compress(&encoder);

    unsigned char *encoded = NULL;
    unsigned long encodedSize = 0;
    jpeg_mem_dest(&encoder, &encoded, &encodedSize);
    encoder.image_width = width;
    encoder.image_height = height;
    encoder.input_components = 3;
    encoder.in_color_space = JCS_RGB;
    jpeg_set_defaults(&encoder);
    jpeg_set_quality(&encoder, 90, TRUE);
    jpeg_start_compress(&encoder, TRUE);

    std::vector<unsigned char> row(width * 3);
    while (encoder.next_scanline < encoder.image_height) {
        const int y = static_cast<int>(encoder.next_scanline);
        for (int x = 0; x < width; ++x) {
            row[x * 3] = static_cast<unsigned char>((x * 17 + y * 3) & 0xff);
            row[x * 3 + 1] =
                    static_cast<unsigned char>((x * 5 + y * 19) & 0xff);
            row[x * 3 + 2] =
                    static_cast<unsigned char>((x * 23 + y * 7) & 0xff);
        }
        JSAMPROW scanline = row.data();
        jpeg_write_scanlines(&encoder, &scanline, 1);
    }
    jpeg_finish_compress(&encoder);
    std::unique_ptr<unsigned char, decltype(&std::free)> encodedOwner(
            encoded, &std::free);
    std::vector<unsigned char> result(
            encodedOwner.get(), encodedOwner.get() + encodedSize);
    jpeg_destroy_compress(&encoder);
    return result;
}

class FailingJpegStream : public LVStream {
private:
    std::vector<unsigned char> _bytes;
    lvpos_t _pos;
    int _readCalls;

public:
    explicit FailingJpegStream(const std::vector<unsigned char> &bytes)
        : _bytes(bytes), _pos(0), _readCalls(0)
    {
    }

    lvopen_mode_t GetMode() override { return LVOM_READ; }

    lverror_t Seek(lvoffset_t offset, lvseek_origin_t origin,
            lvpos_t *newPos) override
    {
        lvoffset_t target = offset;
        if (origin == LVSEEK_CUR)
            target += static_cast<lvoffset_t>(_pos);
        else if (origin == LVSEEK_END)
            target += static_cast<lvoffset_t>(_bytes.size());
        if (target < 0
                || static_cast<std::size_t>(target) > _bytes.size())
            return LVERR_FAIL;
        _pos = static_cast<lvpos_t>(target);
        if (origin == LVSEEK_SET && _pos == 0)
            _readCalls = 0;
        if (newPos)
            *newPos = _pos;
        return LVERR_OK;
    }

    lverror_t GetSize(lvsize_t *size) override
    {
        *size = _bytes.size();
        return LVERR_OK;
    }

    lverror_t SetSize(lvsize_t) override { return LVERR_FAIL; }

    lverror_t Read(void *buffer, lvsize_t count,
            lvsize_t *bytesRead) override
    {
        ++_readCalls;
        if (_readCalls > 1) {
            if (bytesRead)
                *bytesRead = 0;
            return LVERR_FAIL;
        }
        const std::size_t available = _bytes.size()
                - static_cast<std::size_t>(_pos);
        const std::size_t amount = std::min(
                static_cast<std::size_t>(count), available);
        std::memcpy(buffer, _bytes.data() + _pos, amount);
        _pos += amount;
        if (bytesRead)
            *bytesRead = amount;
        return LVERR_OK;
    }

    lverror_t Write(const void *, lvsize_t,
            lvsize_t *bytesWritten) override
    {
        if (bytesWritten)
            *bytesWritten = 0;
        return LVERR_FAIL;
    }

    bool Eof() override { return _pos >= _bytes.size(); }
};

static int testJpegDecoderOwnership() {
    std::vector<unsigned char> jpeg = buildJpegFixture();
    if (jpeg.size() <= 4096)
        return fail("generated JPEG did not span two source buffers");

    LVImageSourceRef image = LVCreateStreamImageSource(
            LVCreateMemoryStream(
                    jpeg.data(), static_cast<int>(jpeg.size()),
                    true, LVOM_READ));
    if (image.isNull()
            || image->GetWidth() != 128
            || image->GetHeight() != 128)
        return fail("JPEG decoder rejected its generated fixture");

    CountingImageDecodeCallback callback;
    if (!image->Decode(&callback) || !image->Decode(&callback))
        return fail("JPEG decoder could not reuse its pool-owned buffers");
    if (callback.starts != 2 || callback.lines != 256
            || callback.ends != 2 || callback.errorEnds != 0)
        return fail("JPEG decoder callback lifecycle is incomplete");

    LVJpegImageSource failing(
            NULL, LVStreamRef(new FailingJpegStream(jpeg)));
    CountingImageDecodeCallback errorCallback;
    if (failing.Decode(&errorCallback)
            || failing.Decode(&errorCallback))
        return fail("JPEG decoder accepted a failed source refill");
    if (errorCallback.starts != 2
            || errorCallback.ends != 0
            || errorCallback.errorEnds != 2)
        return fail("failed JPEG escaped the callback error lifecycle");
    return 0;
}
#endif

#if (USE_LIBPNG==1)
static int testPngDecoderOwnership() {
    static const unsigned char validPng[] = {
        0x89, 0x50, 0x4e, 0x47, 0x0d, 0x0a, 0x1a, 0x0a,
        0x00, 0x00, 0x00, 0x0d, 0x49, 0x48, 0x44, 0x52,
        0x00, 0x00, 0x00, 0x01, 0x00, 0x00, 0x00, 0x01,
        0x08, 0x06, 0x00, 0x00, 0x00, 0x1f, 0x15, 0xc4,
        0x89, 0x00, 0x00, 0x00, 0x0a, 0x49, 0x44, 0x41,
        0x54, 0x78, 0x9c, 0x63, 0x00, 0x01, 0x00, 0x00,
        0x05, 0x00, 0x01, 0x0d, 0x0a, 0x2d, 0xb4, 0x00,
        0x00, 0x00, 0x00, 0x49, 0x45, 0x4e, 0x44, 0xae,
        0x42, 0x60, 0x82
    };
    LVImageSourceRef image = LVCreateStreamImageSource(
            LVCreateMemoryStream(
                    const_cast<unsigned char *>(validPng),
                    static_cast<int>(sizeof(validPng)), true, LVOM_READ));
    if (image.isNull() || image->GetWidth() != 1 || image->GetHeight() != 1)
        return fail("PNG decoder rejected a valid 1x1 image");

    CountingImageDecodeCallback callback;
    if (!image->Decode(&callback) || !image->Decode(&callback))
        return fail("PNG decoder could not reuse its RAII buffers");
    if (callback.starts != 2 || callback.lines != 2
            || callback.ends != 2 || callback.errorEnds != 0)
        return fail("PNG decoder callback lifecycle is incomplete");

    static const std::size_t factoryRejectedSize = 4;
    if (!LVCreateStreamImageSource(
                LVCreateMemoryStream(
                        const_cast<unsigned char *>(validPng),
                        static_cast<int>(factoryRejectedSize),
                        true, LVOM_READ)).isNull())
        return fail("stream image factory published a failed decoder");
    static const std::size_t truncatedSize = 46;
    LVPngImageSource truncated(
            NULL,
            LVCreateMemoryStream(
                    const_cast<unsigned char *>(validPng),
                    static_cast<int>(truncatedSize), true, LVOM_READ));
    CountingImageDecodeCallback errorCallback;
    if (truncated.Decode(&errorCallback)
            || truncated.Decode(&errorCallback))
        return fail("PNG decoder accepted a truncated IDAT");
    if (errorCallback.starts != 2
            || errorCallback.ends != 0
            || errorCallback.errorEnds != 2)
        return fail("truncated PNG escaped the callback error lifecycle");
    return 0;
}
#endif

#if (USE_GIF==1)
static int testGifLzwBoundedReads() {
    unsigned char input = 0;
    unsigned char output = 0;
    CLZWDecoder decoder;
    decoder.SetOutputStream(&output, 1);
    decoder.Init(2);
    if (decoder.CodeExists(-1)
            || decoder.WriteOutString(-1) != 0
            || decoder.WriteOutString(6) != 0
            || decoder.AddString(-1, 0) != -1)
        return fail("GIF LZW decoder accepted an invalid table index");

    decoder.SetOutputStream(NULL, 1);
    if (decoder.WriteOutChar(0) != 0)
        return fail("GIF LZW decoder wrote through a null output stream");
    decoder.FillRestOfOutStream(0);

    decoder.SetInputStream(&input, 1);
    decoder.Init(2);
    if (decoder.ReadInCode() != 0 || decoder.ReadInCode() != 0)
        return fail("GIF LZW decoder rejected complete buffered codes");
    if (decoder.ReadInCode() != -1)
        return fail("GIF LZW decoder read a code past the input boundary");

    decoder.SetInputStream(NULL, 0);
    decoder.Init(2);
    if (decoder.ReadInCode() != -1)
        return fail("GIF LZW decoder accepted an empty input stream");

    decoder.SetInputStream(&input, 1);
    decoder.SetOutputStream(&output, 1);
    if (decoder.Decode(LSWDECODER_MAX_BITS) != 0)
        return fail("GIF LZW decoder accepted an oversized initial code");
    return 0;
}

static int testGifDecoderOwnership() {
    unsigned char shortPattern[] = {'G'};
    if (LVGifImageSource::CheckPattern(NULL, 0)
            || LVGifImageSource::CheckPattern(shortPattern, 1))
        return fail("GIF signature check accepted a truncated buffer");

    static const unsigned char validGif[] = {
        'G', 'I', 'F', '8', '9', 'a',
        0x01, 0x00, 0x01, 0x00, 0x80, 0x00, 0x00,
        0x00, 0x00, 0x00, 0xff, 0xff, 0xff,
        0x2c, 0x00, 0x00, 0x00, 0x00,
        0x01, 0x00, 0x01, 0x00, 0x00,
        0x02, 0x02, 0x4c, 0x01, 0x00, 0x3b
    };
    LVStreamRef validStream = LVCreateMemoryStream(
            const_cast<unsigned char *>(validGif),
            static_cast<int>(sizeof(validGif)), true, LVOM_READ);
    LVImageSourceRef image = LVCreateStreamImageSource(validStream);
    if (image.isNull() || image->GetWidth() != 1 || image->GetHeight() != 1)
        return fail("GIF decoder rejected a valid 1x1 image");

    CountingImageDecodeCallback callback;
    if (!image->Decode(&callback) || !image->Decode(&callback))
        return fail("GIF decoder could not reuse its owned buffers");
    if (callback.starts != 2 || callback.lines != 2 || callback.ends != 2)
        return fail("GIF decoder callback lifecycle is incomplete");

    unsigned char invalidGif[32] = {
        'G', 'I', 'F', '8', '9', 'a'
    };
    LVStreamRef invalidStream = LVCreateMemoryStream(
            invalidGif, static_cast<int>(sizeof(invalidGif)),
            true, LVOM_READ);
    LVGifImageSource invalidImage(NULL, invalidStream);
    CountingImageDecodeCallback invalidCallback;
    if (invalidImage.Decode(&invalidCallback))
        return fail("GIF decoder accepted zero dimensions");
    if (invalidCallback.starts != 0 || invalidCallback.lines != 0
            || invalidCallback.ends != 0)
        return fail("invalid GIF entered the decode callback lifecycle");

    unsigned char unterminatedBlocks[] = {
        'G', 'I', 'F', '8', '9', 'a',
        0x01, 0x00, 0x01, 0x00, 0x80, 0x00, 0x00,
        0x00, 0x00, 0x00, 0xff, 0xff, 0xff,
        0x2c, 0x00, 0x00, 0x00, 0x00,
        0x01, 0x00, 0x01, 0x00, 0x00,
        0x02, 0x02, 0x4c, 0x01
    };
    LVGifImageSource unterminatedImage(
            NULL, LVCreateMemoryStream(
                    unterminatedBlocks,
                    static_cast<int>(sizeof(unterminatedBlocks)),
                    true, LVOM_READ));
    CountingImageDecodeCallback unterminatedCallback;
    if (unterminatedImage.Decode(&unterminatedCallback))
        return fail("GIF decoder accepted unterminated raster sub-blocks");
    if (unterminatedCallback.starts != 0
            || unterminatedCallback.lines != 0
            || unterminatedCallback.ends != 0)
        return fail("truncated GIF raster entered the callback lifecycle");

    unsigned char truncatedExtension[] = {
        'G', 'I', 'F', '8', '9', 'a',
        0x01, 0x00, 0x01, 0x00, 0x80, 0x00, 0x00,
        0x00, 0x00, 0x00, 0xff, 0xff, 0xff,
        0x21, 0xfe, 0x08,
        0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,
        0x00, 0x21
    };
    LVGifImageSource extensionImage(
            NULL, LVCreateMemoryStream(
                    truncatedExtension,
                    static_cast<int>(sizeof(truncatedExtension)),
                    true, LVOM_READ));
    CountingImageDecodeCallback extensionCallback;
    if (extensionImage.Decode(&extensionCallback))
        return fail("GIF decoder accepted a truncated extension record");
    if (extensionCallback.starts != 0
            || extensionCallback.lines != 0
            || extensionCallback.ends != 0)
        return fail("truncated GIF extension entered the callback lifecycle");

    unsigned char missingPalette[] = {
        'G', 'I', 'F', '8', '9', 'a',
        0x01, 0x00, 0x01, 0x00, 0x00, 0x00, 0x00,
        0x2c, 0x00, 0x00, 0x00, 0x00,
        0x01, 0x00, 0x01, 0x00, 0x00,
        0x02, 0x02, 0x4c, 0x01, 0x00, 0x3b,
        0x00, 0x00, 0x00
    };
    LVGifImageSource missingPaletteImage(
            NULL, LVCreateMemoryStream(
                    missingPalette, static_cast<int>(sizeof(missingPalette)),
                    true, LVOM_READ));
    CountingImageDecodeCallback missingPaletteCallback;
    if (missingPaletteImage.Decode(&missingPaletteCallback))
        return fail("GIF decoder accepted a frame without a color table");
    if (missingPaletteCallback.starts != 0
            || missingPaletteCallback.lines != 0
            || missingPaletteCallback.ends != 0)
        return fail("palette-less GIF entered the callback lifecycle");

    unsigned char invalidColor[] = {
        'G', 'I', 'F', '8', '9', 'a',
        0x01, 0x00, 0x01, 0x00, 0x80, 0x00, 0x00,
        0x00, 0x00, 0x00, 0xff, 0xff, 0xff,
        0x2c, 0x00, 0x00, 0x00, 0x00,
        0x01, 0x00, 0x01, 0x00, 0x00,
        0x02, 0x02, 0x5c, 0x01, 0x00, 0x3b
    };
    LVGifImageSource invalidColorImage(
            NULL, LVCreateMemoryStream(
                    invalidColor, static_cast<int>(sizeof(invalidColor)),
                    true, LVOM_READ));
    CountingImageDecodeCallback invalidColorCallback;
    if (invalidColorImage.Decode(&invalidColorCallback))
        return fail("GIF decoder accepted a pixel outside its color table");
    if (invalidColorCallback.starts != 0
            || invalidColorCallback.lines != 0
            || invalidColorCallback.ends != 0)
        return fail("invalid GIF color index entered the callback lifecycle");

    unsigned char shortInterlacedFrame[] = {
        'G', 'I', 'F', '8', '9', 'a',
        0x01, 0x00, 0x10, 0x00, 0x80, 0x00, 0x00,
        0x00, 0x00, 0x00, 0xff, 0xff, 0xff,
        0x2c, 0x00, 0x00, 0x00, 0x00,
        0x01, 0x00, 0x01, 0x00, 0x40,
        0x02, 0x02, 0x4c, 0x01, 0x00, 0x3b
    };
    LVGifImageSource interlacedImage(
            NULL, LVCreateMemoryStream(
                    shortInterlacedFrame,
                    static_cast<int>(sizeof(shortInterlacedFrame)),
                    true, LVOM_READ));
    CountingImageDecodeCallback interlacedCallback;
    if (!interlacedImage.Decode(&interlacedCallback))
        return fail("GIF decoder rejected a bounded interlaced frame");
    if (interlacedCallback.starts != 1
            || interlacedCallback.lines != 16
            || interlacedCallback.ends != 1)
        return fail("interlaced GIF callback lifecycle is incomplete");
    return 0;
}
#endif

#if (USE_NANOSVG==1)
static int testSvgDecoderOwnership() {
    const std::string svg =
            "<svg xmlns=\"http://www.w3.org/2000/svg\" "
            "width=\"4\" height=\"3\">"
            "<rect width=\"4\" height=\"3\" fill=\"#2468ac\"/>"
            "</svg>";
    LVImageSourceRef image = LVCreateStreamImageSource(
            LVCreateMemoryStream(
                    const_cast<char *>(svg.data()),
                    static_cast<int>(svg.size()), true, LVOM_READ));
    if (image.isNull()
            || image->GetWidth() != 6
            || image->GetHeight() != 5)
        return fail("SVG decoder rejected its bounded fixture");

    CountingImageDecodeCallback callback;
    if (!image->Decode(&callback))
        return fail("SVG decoder did not rasterize its owned workspace");
    bool callbackThrew = false;
    try {
        ThrowingImageDecodeCallback throwingCallback;
        image->Decode(&throwingCallback);
    } catch (const std::runtime_error &) {
        callbackThrew = true;
    }
    if (!callbackThrew || !image->Decode(&callback))
        return fail("SVG callback unwind did not release its workspace");
    if (callback.starts != 2 || callback.lines != 10
            || callback.ends != 2 || callback.errorEnds != 0)
        return fail("SVG decoder callback lifecycle is incomplete");

    std::vector<unsigned char> mutableSvg(
            svg.begin(), svg.end());
    mutableSvg.push_back(0);
    int pngSize = -1;
    unsigned char *png = convertSVGtoPNG(
            mutableSvg.data(),
            static_cast<int>(svg.size()), 2.0f, &pngSize);
    if (!png || pngSize <= 8) {
        std::free(png);
        return fail("SVG to PNG conversion lost its RAII workspace");
    }
    std::free(png);

    pngSize = -1;
    if (convertSVGtoPNG(
                mutableSvg.data(),
                static_cast<int>(svg.size()), 0.0f, &pngSize)
            || pngSize != 0)
        return fail("SVG conversion accepted an invalid scale");
    return 0;
}
#endif

static int testDrawBufferStorageOwnership() {
    draw_buf_t legacy = {};
    if (lvdrawbufAlloc(&legacy, 3, 8, 2)
            || legacy.data != NULL
            || legacy.height != 0
            || legacy.bitsPerPixel != 0
            || legacy.bytesPerRow != 0)
        return fail("legacy draw buffer published an invalid layout");
    if (!lvdrawbufAlloc(&legacy, 2, 9, 3)
            || legacy.data == NULL
            || legacy.height != 3
            || legacy.bitsPerPixel != 2
            || legacy.bytesPerRow != 3)
        return fail("legacy draw buffer did not publish its owned layout");
    lvdrawbufFill(&legacy, 0x5A);
    for (int i = 0; i < legacy.height * legacy.bytesPerRow; ++i) {
        if (legacy.data[i] != 0x5A)
            return fail("legacy draw buffer fill lost owned bytes");
    }
    lvdrawbufFree(&legacy);
    lvdrawbufFree(&legacy);
    if (legacy.data != NULL
            || legacy.height != 0
            || legacy.bitsPerPixel != 0
            || legacy.bytesPerRow != 0)
        return fail("legacy draw buffer teardown is not idempotent");

    LVColorDrawBuf ownedColor(3, 2, 32);
    lUInt32 colorValue = 1;
    for (int y = 0; y < ownedColor.GetHeight(); ++y) {
        lUInt32 *row = reinterpret_cast<lUInt32 *>(
                ownedColor.GetScanLine(y));
        for (int x = 0; x < ownedColor.GetWidth(); ++x)
            row[x] = colorValue++;
    }
    lUInt8 *ownedColorData = ownedColor.GetScanLine(0);
    bool colorResizeRejected = false;
    try {
        ownedColor.Resize(
                std::numeric_limits<int>::max(),
                std::numeric_limits<int>::max());
    } catch (const std::length_error &) {
        colorResizeRejected = true;
    }
    if (!colorResizeRejected
            || ownedColor.GetWidth() != 3
            || ownedColor.GetHeight() != 2
            || ownedColor.GetRowSize() != 12
            || ownedColor.GetScanLine(0) != ownedColorData)
        return fail("color draw-buffer resize did not roll back");
    ownedColor.Rotate(CR_ROTATE_ANGLE_90);
    if (ownedColor.GetWidth() != 2
            || ownedColor.GetHeight() != 3
            || ownedColor.GetRowSize() != 8)
        return fail("owned color draw-buffer rotation lost its layout");
    std::vector<lUInt32> rotatedColor;
    for (int y = 0; y < ownedColor.GetHeight(); ++y) {
        lUInt32 *row = reinterpret_cast<lUInt32 *>(
                ownedColor.GetScanLine(y));
        rotatedColor.insert(
                rotatedColor.end(), row, row + ownedColor.GetWidth());
    }
    std::sort(rotatedColor.begin(), rotatedColor.end());
    if (rotatedColor != std::vector<lUInt32>({1, 2, 3, 4, 5, 6}))
        return fail("owned color draw-buffer rotation lost pixels");

    lUInt32 externalColor[] = {11, 12, 13, 14, 15, 16};
    lUInt32 externalColorCopy[6];
    std::memcpy(
            externalColorCopy, externalColor, sizeof(externalColor));
    LVColorDrawBuf borrowedColor(
            3, 2, reinterpret_cast<lUInt8 *>(externalColor), 32);
    borrowedColor.Resize(4, 4);
    if (borrowedColor.GetWidth() != 3
            || borrowedColor.GetHeight() != 2
            || borrowedColor.GetScanLine(0)
                != reinterpret_cast<lUInt8 *>(externalColor))
        return fail("borrowed color draw-buffer changed its resize contract");
    borrowedColor.Rotate(CR_ROTATE_ANGLE_90);
    if (borrowedColor.GetWidth() != 2
            || borrowedColor.GetHeight() != 3
            || borrowedColor.GetScanLine(0)
                == reinterpret_cast<lUInt8 *>(externalColor)
            || std::memcmp(
                    externalColorCopy, externalColor,
                    sizeof(externalColor)) != 0)
        return fail("color draw-buffer did not detach borrowed storage");

    LVGrayDrawBuf ownedGray(5, 3, 2);
    ownedGray.GetScanLine(0)[0] = 0xFF;
    ownedGray.GetScanLine(0)[1] = 0xFF;
    ownedGray.GetScanLine(1)[0] = 0x00;
    ownedGray.GetScanLine(1)[1] = 0x00;
    ownedGray.GetScanLine(2)[0] = 0xAA;
    ownedGray.GetScanLine(2)[1] = 0xAA;
    ownedGray.ConvertToBitmap(false);
    if (ownedGray.GetBitsPerPixel() != 1
            || ownedGray.GetRowSize() != 1
            || ownedGray.GetScanLine(0)[0] != 0xF8
            || ownedGray.GetScanLine(1)[0] != 0x00
            || ownedGray.GetScanLine(2)[0] != 0xF8)
        return fail("gray bitmap conversion lost row storage");
    ownedGray.Rotate(CR_ROTATE_ANGLE_90);
    if (ownedGray.GetWidth() != 3
            || ownedGray.GetHeight() != 5
            || ownedGray.GetRowSize() != 1)
        return fail("owned gray draw-buffer rotation lost its layout");
    lUInt8 *ownedGrayData = ownedGray.GetScanLine(0);
    bool grayResizeRejected = false;
    try {
        ownedGray.Resize(
                std::numeric_limits<int>::max(),
                std::numeric_limits<int>::max());
    } catch (const std::length_error &) {
        grayResizeRejected = true;
    }
    if (!grayResizeRejected
            || ownedGray.GetWidth() != 3
            || ownedGray.GetHeight() != 5
            || ownedGray.GetScanLine(0) != ownedGrayData)
        return fail("gray draw-buffer resize did not roll back");

    lUInt8 externalGray[] = {
        0xFF, 0xFF, 0x00, 0x00, 0xAA, 0xAA};
    lUInt8 externalGrayCopy[sizeof(externalGray)];
    std::memcpy(externalGrayCopy, externalGray, sizeof(externalGray));
    LVGrayDrawBuf borrowedGray(5, 3, 2, externalGray);
    borrowedGray.ConvertToBitmap(false);
    if (borrowedGray.GetBitsPerPixel() != 1
            || borrowedGray.GetRowSize() != 1
            || borrowedGray.GetScanLine(0) == externalGray
            || std::memcmp(
                    externalGrayCopy, externalGray,
                    sizeof(externalGray)) != 0)
        return fail("gray conversion did not detach borrowed storage");

    LVGrayDrawBuf rotatedBorrowedGray(5, 3, 2, externalGray);
    rotatedBorrowedGray.Rotate(CR_ROTATE_ANGLE_90);
    if (rotatedBorrowedGray.GetWidth() != 3
            || rotatedBorrowedGray.GetHeight() != 5
            || rotatedBorrowedGray.GetRowSize() != 1
            || rotatedBorrowedGray.GetScanLine(0) == externalGray
            || std::memcmp(
                    externalGrayCopy, externalGray,
                    sizeof(externalGray)) != 0)
        return fail("gray rotation did not detach borrowed storage");

    lUInt8 resizedExternalGray[] = {
        0x11, 0x22, 0x33, 0x44, 0x55, 0x66};
    lUInt8 resizedExternalGrayCopy[sizeof(resizedExternalGray)];
    std::memcpy(
            resizedExternalGrayCopy, resizedExternalGray,
            sizeof(resizedExternalGray));
    LVGrayDrawBuf resizedBorrowedGray(
            5, 3, 2, resizedExternalGray);
    resizedBorrowedGray.Resize(4, 4);
    if (resizedBorrowedGray.GetWidth() != 4
            || resizedBorrowedGray.GetHeight() != 4
            || resizedBorrowedGray.GetRowSize() != 1
            || resizedBorrowedGray.GetScanLine(0) == resizedExternalGray
            || std::memcmp(
                    resizedExternalGrayCopy, resizedExternalGray,
                    sizeof(resizedExternalGray)) != 0)
        return fail("gray resize did not detach borrowed storage");
    return 0;
}

class CountingColorDrawBuf : public LVColorDrawBuf {
private:
    int &m_destroyed;

public:
    explicit CountingColorDrawBuf(int &destroyed)
        : LVColorDrawBuf(2, 2, 16), m_destroyed(destroyed)
    {
    }

    ~CountingColorDrawBuf() override
    {
        ++m_destroyed;
    }
};

static int testImageSourceOwnership() {
    static const char *validXpm[] = {
        "2 2 2 1",
        "a c #000000",
        "b c #ffffff",
        "ab",
        "ba"
    };
    LVImageSourceRef xpm = LVCreateXPMImageSource(validXpm);
    if (xpm.isNull() || xpm->GetWidth() != 2 || xpm->GetHeight() != 2)
        return fail("XPM source rejected a valid fixture");
    CountingImageDecodeCallback xpmCallback;
    if (!xpm->Decode(&xpmCallback) || !xpm->Decode(&xpmCallback))
        return fail("XPM source could not reuse its RAII buffers");
    if (xpmCallback.starts != 2 || xpmCallback.lines != 4
            || xpmCallback.ends != 2)
        return fail("XPM source callback lifecycle is incomplete");

    NinePatchFixtureImageSource validNinePatch(true, true);
    CR9PatchInfo *ninePatch = validNinePatch.DetectNinePatch();
    if (!ninePatch
            || validNinePatch.GetNinePatchInfo() != ninePatch
            || validNinePatch.DetectNinePatch() != ninePatch
            || validNinePatch.decodeCalls() != 1
            || ninePatch->frame.left != 1
            || ninePatch->frame.top != 1
            || ninePatch->frame.right != 1
            || ninePatch->frame.bottom != 1)
        return fail("valid nine-patch metadata was not cached");
    NinePatchFixtureImageSource failedNinePatch(true, false);
    if (failedNinePatch.DetectNinePatch() != NULL
            || failedNinePatch.GetNinePatchInfo() != NULL
            || failedNinePatch.decodeCalls() != 1)
        return fail("failed nine-patch decode published partial metadata");
    NinePatchFixtureImageSource invalidNinePatch(false, true);
    if (invalidNinePatch.DetectNinePatch() != NULL
            || invalidNinePatch.DetectNinePatch() != NULL
            || invalidNinePatch.decodeCalls() != 2)
        return fail("invalid nine-patch metadata was retained");

    CapturingImageDecodeCallback sourceCapture(
            xpm.get(), xpm.get(), 2);
    if (!xpm->Decode(&sourceCapture)
            || sourceCapture.sourceMismatch
            || sourceCapture.starts != 1
            || sourceCapture.lines != 2
            || sourceCapture.ends != 1
            || sourceCapture.errorEnds != 0)
        return fail("color-transform source fixture did not decode");
    LVImageSourceRef colorTransform =
            LVCreateColorTransformImageSource(
                    xpm, 0x808080, 0x202020);
    CapturingImageDecodeCallback transformedCapture(
            colorTransform.get(), xpm.get(), 2);
    if (colorTransform.isNull()
            || !colorTransform->Decode(&transformedCapture)
            || !colorTransform->Decode(&transformedCapture)
            || transformedCapture.sourceMismatch
            || transformedCapture.starts != 2
            || transformedCapture.lines != 4
            || transformedCapture.ends != 2
            || transformedCapture.errorEnds != 0)
        return fail("color-transform workspace could not be reused");
    std::vector<lUInt32> expectedTransformed = sourceCapture.pixels;
    expectedTransformed.insert(
            expectedTransformed.end(),
            sourceCapture.pixels.begin(), sourceCapture.pixels.end());
    if (transformedCapture.pixels != expectedTransformed)
        return fail("neutral color transform changed decoded pixels");
    RejectingImageDecodeCallback rejectingColorTransformCallback;
    if (colorTransform->Decode(
                &rejectingColorTransformCallback)
            || rejectingColorTransformCallback.starts != 1
            || rejectingColorTransformCallback.lines != 1
            || rejectingColorTransformCallback.errorEnds != 1)
        return fail("color transform ignored callback cancellation");
    ThrowingImageDecodeCallback throwingTransformCallback;
    bool transformCallbackThrew = false;
    try {
        colorTransform->Decode(&throwingTransformCallback);
    } catch (const std::runtime_error &) {
        transformCallbackThrew = true;
    }
    CapturingImageDecodeCallback recoveredTransformCapture(
            colorTransform.get(), xpm.get(), 2);
    if (!transformCallbackThrew
            || !colorTransform->Decode(&recoveredTransformCapture)
            || recoveredTransformCapture.sourceMismatch
            || recoveredTransformCapture.starts != 1
            || recoveredTransformCapture.lines != 2
            || recoveredTransformCapture.ends != 1)
        return fail("color-transform workspace survived callback exception");
    if (!LVCreateColorTransformImageSource(
                LVImageSourceRef(), 0x808080, 0x202020).isNull())
        return fail("color-transform factory wrapped a null source");

    LVImageSourceRef alphaTransform =
            LVCreateAlphaTransformImageSource(xpm, 1);
    CapturingImageDecodeCallback alphaCapture(
            alphaTransform.get(), xpm.get(), 2);
    if (alphaTransform.isNull()
            || !alphaTransform->Decode(&alphaCapture)
            || !alphaTransform->Decode(&alphaCapture)
            || alphaCapture.sourceMismatch
            || alphaCapture.starts != 2
            || alphaCapture.lines != 4
            || alphaCapture.ends != 2
            || alphaCapture.errorEnds != 0)
        return fail("alpha-transform callback borrow could not be reused");
    std::vector<lUInt32> expectedAlphaPixels =
            sourceCapture.pixels;
    for (lUInt32 &pixel : expectedAlphaPixels)
        pixel |= 0x01000000;
    const std::vector<lUInt32> oneAlphaPass =
            expectedAlphaPixels;
    expectedAlphaPixels.insert(
            expectedAlphaPixels.end(),
            oneAlphaPass.begin(), oneAlphaPass.end());
    if (alphaCapture.pixels != expectedAlphaPixels)
        return fail("alpha transform used non-normalized opacity");
    LVImageSourceRef transparentTransform =
            LVCreateAlphaTransformImageSource(xpm, 1024);
    CapturingImageDecodeCallback transparentCapture(
            transparentTransform.get(), xpm.get(), 2);
    if (transparentTransform.isNull()
            || !transparentTransform->Decode(
                    &transparentCapture)
            || transparentCapture.sourceMismatch
            || transparentCapture.starts != 1
            || transparentCapture.lines != 2
            || transparentCapture.ends != 1)
        return fail("alpha-transform factory did not clamp transparency");
    for (lUInt32 pixel : transparentCapture.pixels) {
        if ((pixel & 0xFF000000) != 0xFF000000)
            return fail("clamped alpha transform was not transparent");
    }
    RejectingImageDecodeCallback rejectingAlphaCallback;
    if (alphaTransform->Decode(&rejectingAlphaCallback)
            || rejectingAlphaCallback.starts != 1
            || rejectingAlphaCallback.lines != 1
            || rejectingAlphaCallback.errorEnds != 1)
        return fail("alpha transform ignored callback cancellation");
    ThrowingImageDecodeCallback throwingAlphaCallback;
    bool alphaCallbackThrew = false;
    try {
        alphaTransform->Decode(&throwingAlphaCallback);
    } catch (const std::runtime_error &) {
        alphaCallbackThrew = true;
    }
    CountingImageDecodeCallback recoveredAlphaCallback;
    if (!alphaCallbackThrew
            || !alphaTransform->Decode(&recoveredAlphaCallback)
            || recoveredAlphaCallback.starts != 1
            || recoveredAlphaCallback.lines != 2
            || recoveredAlphaCallback.ends != 1
            || alphaTransform->Decode(NULL))
        return fail("alpha-transform borrow survived callback exception");
    if (!LVCreateAlphaTransformImageSource(
                LVImageSourceRef(), 1).isNull())
        return fail("alpha-transform factory wrapped a null source");

    LVImageSourceRef stretchTransform =
            LVCreateStretchFilledTransform(
                    xpm, 4, 4,
                    IMG_TRANSFORM_STRETCH,
                    IMG_TRANSFORM_STRETCH);
    CapturingImageDecodeCallback stretchCapture(
            stretchTransform.get(), xpm.get(), 4);
    if (stretchTransform.isNull()
            || !stretchTransform->Decode(&stretchCapture)
            || !stretchTransform->Decode(&stretchCapture)
            || stretchCapture.sourceMismatch
            || stretchCapture.starts != 2
            || stretchCapture.lines != 8
            || stretchCapture.ends != 2
            || stretchCapture.errorEnds != 0)
        return fail("stretch-transform row workspace could not be reused");
    static const std::vector<lUInt32> expectedStretchPixels = {
        0x000000, 0x000000, 0xffffff, 0xffffff,
        0x000000, 0x000000, 0xffffff, 0xffffff,
        0xffffff, 0xffffff, 0x000000, 0x000000,
        0xffffff, 0xffffff, 0x000000, 0x000000,
        0x000000, 0x000000, 0xffffff, 0xffffff,
        0x000000, 0x000000, 0xffffff, 0xffffff,
        0xffffff, 0xffffff, 0x000000, 0x000000,
        0xffffff, 0xffffff, 0x000000, 0x000000
    };
    if (stretchCapture.pixels != expectedStretchPixels)
        return fail("stretch-transform pixel mapping changed");
    LVImageSourceRef shrinkTransform =
            LVCreateStretchFilledTransform(
                    xpm, 1, 1,
                    IMG_TRANSFORM_STRETCH,
                    IMG_TRANSFORM_STRETCH);
    CapturingImageDecodeCallback shrinkCapture(
            shrinkTransform.get(), xpm.get(), 1);
    if (shrinkTransform.isNull()
            || !shrinkTransform->Decode(&shrinkCapture)
            || shrinkCapture.sourceMismatch
            || shrinkCapture.starts != 1
            || shrinkCapture.lines != 1
            || shrinkCapture.ends != 1
            || shrinkCapture.pixels
                    != std::vector<lUInt32>{0xffffff})
        return fail("vertical shrink stopped before its output row");
    LVImageSourceRef splitShrinkTransform =
            LVCreateStretchFilledTransform(xpm, 1, 1);
    CapturingImageDecodeCallback splitShrinkCapture(
            splitShrinkTransform.get(), xpm.get(), 1);
    if (splitShrinkTransform.isNull()
            || !splitShrinkTransform->Decode(&splitShrinkCapture)
            || splitShrinkCapture.sourceMismatch
            || splitShrinkCapture.starts != 1
            || splitShrinkCapture.lines != 1
            || splitShrinkCapture.ends != 1
            || splitShrinkCapture.pixels
                    != std::vector<lUInt32>{0x000000})
        return fail("split shrink did not mirror horizontal mapping");
    RejectingImageDecodeCallback rejectingStretchCallback;
    if (stretchTransform->Decode(&rejectingStretchCallback)
            || rejectingStretchCallback.starts != 1
            || rejectingStretchCallback.lines != 1
            || rejectingStretchCallback.errorEnds != 1)
        return fail("stretch transform ignored callback cancellation");
    ThrowingImageDecodeCallback throwingStretchCallback;
    bool stretchCallbackThrew = false;
    try {
        stretchTransform->Decode(&throwingStretchCallback);
    } catch (const std::runtime_error &) {
        stretchCallbackThrew = true;
    }
    CountingImageDecodeCallback recoveredStretchCallback;
    if (!stretchCallbackThrew
            || !stretchTransform->Decode(&recoveredStretchCallback)
            || recoveredStretchCallback.starts != 1
            || recoveredStretchCallback.lines != 4
            || recoveredStretchCallback.ends != 1
            || stretchTransform->Decode(NULL))
        return fail("stretch-transform borrow survived callback exception");
    if (!LVCreateStretchFilledTransform(
                LVImageSourceRef(), 1, 1).isNull()
            || !LVCreateStretchFilledTransform(
                    xpm, 0, 1).isNull()
            || !LVCreateTileTransform(
                    xpm, 1, 0, 0, 0).isNull())
        return fail("stretch-transform factory accepted invalid dimensions");

    int failedTransformCalls = 0;
    LVImageSourceRef failedTransform =
            LVCreateColorTransformImageSource(
                    LVImageSourceRef(
                            new FailingImageSource(failedTransformCalls)),
                    0x808080, 0x202020);
    CountingImageDecodeCallback failedTransformCallback;
    if (failedTransform->Decode(&failedTransformCallback)
            || failedTransform->Decode(&failedTransformCallback)
            || failedTransformCalls != 2
            || failedTransformCallback.starts != 2
            || failedTransformCallback.lines != 0
            || failedTransformCallback.ends != 0
            || failedTransformCallback.errorEnds != 2)
        return fail("failed color transform published partial rows");
    int failedAlphaCalls = 0;
    LVImageSourceRef failedAlpha =
            LVCreateAlphaTransformImageSource(
                    LVImageSourceRef(
                            new FailingImageSource(failedAlphaCalls)),
                    1);
    CountingImageDecodeCallback failedAlphaCallback;
    if (failedAlpha->Decode(&failedAlphaCallback)
            || failedAlpha->Decode(&failedAlphaCallback)
            || failedAlphaCalls != 2
            || failedAlphaCallback.starts != 2
            || failedAlphaCallback.lines != 2
            || failedAlphaCallback.ends != 0
            || failedAlphaCallback.errorEnds != 2)
        return fail("failed alpha transform retained its callback borrow");
    int failedStretchCalls = 0;
    LVImageSourceRef failedStretch =
            LVCreateStretchFilledTransform(
                    LVImageSourceRef(
                            new FailingImageSource(failedStretchCalls)),
                    4, 4,
                    IMG_TRANSFORM_STRETCH,
                    IMG_TRANSFORM_STRETCH);
    CountingImageDecodeCallback failedStretchCallback;
    if (failedStretch->Decode(&failedStretchCallback)
            || failedStretch->Decode(&failedStretchCallback)
            || failedStretchCalls != 2
            || failedStretchCallback.starts != 2
            || failedStretchCallback.lines != 4
            || failedStretchCallback.ends != 0
            || failedStretchCallback.errorEnds != 2)
        return fail("failed stretch transform retained its row workspace");
    LVImageSourceRef abortedTransform =
            LVCreateColorTransformImageSource(
                    LVImageSourceRef(new AbortedImageSource()),
                    0x808080, 0x202020);
    CountingImageDecodeCallback abortedTransformCallback;
    if (abortedTransform->Decode(&abortedTransformCallback)
            || abortedTransformCallback.starts != 1
            || abortedTransformCallback.lines != 0
            || abortedTransformCallback.ends != 0
            || abortedTransformCallback.errorEnds != 1)
        return fail("aborted color transform retained its workspace");
    LVImageSourceRef abortedAlpha =
            LVCreateAlphaTransformImageSource(
                    LVImageSourceRef(new AbortedImageSource()), 1);
    CountingImageDecodeCallback abortedAlphaCallback;
    if (abortedAlpha->Decode(&abortedAlphaCallback)
            || abortedAlphaCallback.starts != 1
            || abortedAlphaCallback.lines != 1
            || abortedAlphaCallback.ends != 0
            || abortedAlphaCallback.errorEnds != 1)
        return fail("aborted alpha transform retained its callback borrow");
    LVImageSourceRef abortedStretch =
            LVCreateStretchFilledTransform(
                    LVImageSourceRef(new AbortedImageSource()),
                    4, 4,
                    IMG_TRANSFORM_STRETCH,
                    IMG_TRANSFORM_STRETCH);
    CountingImageDecodeCallback abortedStretchCallback;
    if (abortedStretch->Decode(&abortedStretchCallback)
            || abortedStretchCallback.starts != 1
            || abortedStretchCallback.lines != 2
            || abortedStretchCallback.ends != 0
            || abortedStretchCallback.errorEnds != 1)
        return fail("aborted stretch transform retained its row workspace");

    const std::vector<int> ninePatchMap =
            LVImageScaledDrawCallback::GenNinePatchMap(6, 8, 1, 1);
    if (ninePatchMap.size() != 8
            || ninePatchMap.front() != 1
            || ninePatchMap[3] != 2
            || ninePatchMap[4] != 3
            || ninePatchMap.back() != 4)
        return fail("nine-patch scaling map changed during RAII migration");

    static const lUInt32 scaledSentinel = 0x00123456;
    for (bool smooth : {false, true}) {
        LVColorDrawBuf scaled(4, 4, 32);
        scaled.Clear(scaledSentinel);
        scaled.setSmoothScalingImages(smooth);
        scaled.Draw(xpm, 0, 0, 4, 4, false);
        bool changed = false;
        for (int y = 0; y < 4; ++y) {
            for (int x = 0; x < 4; ++x)
                changed = changed
                        || scaled.GetPixel(x, y) != scaledSentinel;
        }
        if (!changed)
            return fail(smooth
                    ? "smooth image scaling did not render its RAII snapshot"
                    : "mapped image scaling did not render its RAII maps");
    }

    static const int unpackedBpps[] = {8, 16, 32};
    for (int bpp : unpackedBpps) {
        LVImageSourceRef unpacked =
                LVCreateUnpackedImageSource(xpm, 1024, bpp);
        RejectingImageDecodeCallback rejectedUnpackedCallback;
        if (unpacked.isNull()
                || unpacked.get() == xpm.get()
                || unpacked->Decode(&rejectedUnpackedCallback)
                || rejectedUnpackedCallback.starts != 1
                || rejectedUnpackedCallback.lines != 1
                || rejectedUnpackedCallback.errorEnds != 1)
            return fail("unpacked image ignored callback cancellation");
        CountingImageDecodeCallback unpackedCallback;
        if (!unpacked->Decode(&unpackedCallback)
                || !unpacked->Decode(&unpackedCallback))
            return fail("unpacked image source could not reuse its buffers");
        if (unpackedCallback.starts != 2
                || unpackedCallback.lines != 4
                || unpackedCallback.ends != 2)
            return fail("unpacked image callback lifecycle is incomplete");
    }

    if (LVCreateUnpackedImageSource(
                xpm, 1024, 12).get() != xpm.get()
            || LVCreateUnpackedImageSource(
                    xpm, -1, 16).get() != xpm.get())
        return fail("unpacked image factory accepted an invalid contract");
    int oversizedUnpackCalls = 0;
    LVImageSourceRef oversizedUnpackSource(
            new DimensionOnlyImageSource(
                    std::numeric_limits<int>::max(),
                    std::numeric_limits<int>::max(),
                    oversizedUnpackCalls));
    if (LVCreateUnpackedImageSource(
                oversizedUnpackSource,
                std::numeric_limits<int>::max(),
                32).get() != oversizedUnpackSource.get()
            || oversizedUnpackCalls != 0)
        return fail("unpacked image size arithmetic overflowed");

    int failingDecodeCalls = 0;
    LVImageSourceRef failingSource(
            new FailingImageSource(failingDecodeCalls));
    LVImageSourceRef unpackFallback =
            LVCreateUnpackedImageSource(failingSource, 1024, 16);
    if (unpackFallback.get() != failingSource.get()
            || failingDecodeCalls != 1)
        return fail("failed unpack did not release its candidate buffers");

    LVColorDrawBuf failedScale(4, 4, 32);
    failedScale.Clear(scaledSentinel);
    failedScale.setSmoothScalingImages(true);
    failedScale.Draw(failingSource, 0, 0, 4, 4, false);
    if (failingDecodeCalls != 2)
        return fail("failed scaling fixture did not enter decode");
    for (int y = 0; y < 4; ++y) {
        for (int x = 0; x < 4; ++x) {
            if (failedScale.GetPixel(x, y) != scaledSentinel)
                return fail("failed smooth scaling rendered partial data");
        }
    }

    static const char *invalidXpm[] = {
        "2 2 2 1",
        "a invalid",
        "b c #ffffff",
        "ab",
        "ba"
    };
    if (!LVCreateXPMImageSource(invalidXpm).isNull())
        return fail("XPM source accepted an invalid palette");

    static const char *nullXpm[] = {NULL};
    if (!LVCreateXPMImageSource(nullXpm).isNull())
        return fail("XPM source accepted a null header");

    static const char *shortXpm[] = {
        "2 1 2 1",
        "a c #000000",
        "b c #ffffff",
        ""
    };
    if (!LVCreateXPMImageSource(shortXpm).isNull())
        return fail("XPM source accepted a truncated raster row");

    static const char *unknownSymbolXpm[] = {
        "2 1 2 1",
        "a c #000000",
        "b c #ffffff",
        "ac"
    };
    if (!LVCreateXPMImageSource(unknownSymbolXpm).isNull())
        return fail("XPM source accepted an undefined color symbol");

    static const char *highByteXpm[] = {
        "1 1 2 1",
        "\x80 c #123456",
        "a c #ffffff",
        "\x80"
    };
    LVImageSourceRef highByteImage = LVCreateXPMImageSource(highByteXpm);
    CountingImageDecodeCallback highByteCallback;
    if (highByteImage.isNull()
            || !highByteImage->Decode(&highByteCallback)
            || highByteCallback.starts != 1
            || highByteCallback.lines != 1
            || highByteCallback.ends != 1)
        return fail("XPM source rejected a bounded high-byte symbol");

    LVImageSourceRef dummy = LVCreateDummyImageSource(NULL, 3, 3);
    CountingImageDecodeCallback dummyCallback;
    if (!dummy->Decode(&dummyCallback) || !dummy->Decode(&dummyCallback))
        return fail("dummy image source could not reuse its row buffer");
    if (dummyCallback.starts != 2 || dummyCallback.lines != 6
            || dummyCallback.ends != 2)
        return fail("dummy image callback lifecycle is incomplete");
    if (LVCreateDummyImageSource(NULL, 0, 3)->Decode(NULL))
        return fail("dummy image source accepted zero width");

    int destroyed = 0;
    LVImageSourceRef drawBufferImage = LVCreateDrawBufImageSource(
            new CountingColorDrawBuf(destroyed), true);
    CountingImageDecodeCallback drawBufferCallback;
    if (!drawBufferImage->Decode(&drawBufferCallback)
            || drawBufferCallback.starts != 1
            || drawBufferCallback.lines != 2
            || drawBufferCallback.ends != 1)
        return fail("16-bit draw-buffer image did not decode");
    drawBufferImage.Clear();
    if (destroyed != 1)
        return fail("owned draw buffer was not released exactly once");

    return 0;
}

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

struct StoredZipEntrySpec {
    std::string name;
    std::vector<unsigned char> payload;
};

static std::vector<unsigned char> byteVector(const std::string &contents) {
    return std::vector<unsigned char>(contents.begin(), contents.end());
}

static std::vector<unsigned char> buildStoredZipEntries(
        const std::vector<StoredZipEntrySpec> &entries) {
    std::vector<unsigned char> bytes;
    std::vector<std::uint32_t> offsets;
    for (const StoredZipEntrySpec &entry : entries) {
        offsets.push_back(static_cast<std::uint32_t>(bytes.size()));
        appendLe32(bytes, 0x04034b50);
        appendLe16(bytes, 20);
        appendLe16(bytes, 0);
        appendLe16(bytes, 0);
        appendLe16(bytes, 0);
        appendLe16(bytes, 0);
        appendLe32(bytes, 0);
        appendLe32(
                bytes, static_cast<std::uint32_t>(entry.payload.size()));
        appendLe32(
                bytes, static_cast<std::uint32_t>(entry.payload.size()));
        appendLe16(bytes, static_cast<std::uint16_t>(entry.name.size()));
        appendLe16(bytes, 0);
        bytes.insert(bytes.end(), entry.name.begin(), entry.name.end());
        bytes.insert(
                bytes.end(), entry.payload.begin(), entry.payload.end());
    }

    const std::uint32_t centralOffset =
            static_cast<std::uint32_t>(bytes.size());
    for (std::size_t i = 0; i < entries.size(); ++i) {
        const StoredZipEntrySpec &entry = entries[i];
        appendLe32(bytes, 0x02014b50);
        appendLe16(bytes, 20);
        appendLe16(bytes, 20);
        appendLe16(bytes, 0);
        appendLe16(bytes, 0);
        appendLe16(bytes, 0);
        appendLe16(bytes, 0);
        appendLe32(bytes, 0);
        appendLe32(
                bytes, static_cast<std::uint32_t>(entry.payload.size()));
        appendLe32(
                bytes, static_cast<std::uint32_t>(entry.payload.size()));
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

static std::vector<unsigned char> buildFb3Fixture(
        const std::string &body) {
    const std::string contentTypes =
            "<?xml version=\"1.0\" encoding=\"utf-8\"?>"
            "<Types>"
            "<Override PartName=\"/body.xml\" "
            "ContentType=\"application/fb3-body+xml\"/>"
            "<Override PartName=\"/description.xml\" "
            "ContentType=\"application/fb3-description+xml\"/>"
            "<Override PartName=\"/docProps/core.xml\" "
            "ContentType=\"application/vnd.openxmlformats-package."
            "core-properties+xml\"/>"
            "</Types>";
    const std::string rootRelations =
            "<?xml version=\"1.0\" encoding=\"utf-8\"?>"
            "<Relationships>"
            "<Relationship Id=\"cover\" "
            "Type=\"http://schemas.openxmlformats.org/package/2006/"
            "relationships/metadata/thumbnail\" "
            "Target=\"images/cover.png\"/>"
            "</Relationships>";
    const std::string bodyRelations =
            "<?xml version=\"1.0\" encoding=\"utf-8\"?>"
            "<Relationships>"
            "<Relationship Id=\"img1\" "
            "Type=\"http://www.fictionbook.org/FictionBook3/"
            "relationships/image\" Target=\"images/pic.png\"/>"
            "</Relationships>";
    const std::string description =
            "<?xml version=\"1.0\" encoding=\"utf-8\"?>"
            "<fb3-description><lang>en-US</lang></fb3-description>";
    const std::string coreProperties =
            "<?xml version=\"1.0\" encoding=\"utf-8\"?>"
            "<coreProperties>"
            "<creator>Fixture Author</creator>"
            "<title>Fixture Title</title>"
            "<language>core-lang</language>"
            "<description>Fixture Description</description>"
            "</coreProperties>";
    return buildStoredZipEntries({
        {"[Content_Types].xml", byteVector(contentTypes)},
        {"_rels/.rels", byteVector(rootRelations)},
        {"_rels/body.xml.rels", byteVector(bodyRelations)},
        {"body.xml", byteVector(body)},
        {"description.xml", byteVector(description)},
        {"docProps/core.xml", byteVector(coreProperties)},
        {"images/cover.png", {}},
        {"images/pic.png", {}},
    });
}

static std::vector<unsigned char> buildOdtFixture(
        const std::string &styles) {
    const std::string meta =
            "<?xml version=\"1.0\" encoding=\"utf-8\"?>"
            "<office:document-meta"
            " xmlns:office=\"urn:oasis:names:tc:opendocument:"
            "xmlns:office:1.0\""
            " xmlns:dc=\"http://purl.org/dc/elements/1.1/\">"
            "<office:meta>"
            "<dc:creator>ODT Owner</dc:creator>"
            "<dc:title>Owned ODT</dc:title>"
            "<dc:description>Scoped metadata DOM</dc:description>"
            "</office:meta></office:document-meta>";
    const std::string content =
            "<?xml version=\"1.0\" encoding=\"utf-8\"?>"
            "<office:document-content"
            " xmlns:office=\"urn:oasis:names:tc:opendocument:"
            "xmlns:office:1.0\""
            " xmlns:text=\"urn:oasis:names:tc:opendocument:"
            "xmlns:text:1.0\""
            " xmlns:style=\"urn:oasis:names:tc:opendocument:"
            "xmlns:style:1.0\">"
            "<office:body><office:text>"
            "<text:h text:outline-level=\"1\">ODT Chapter</text:h>"
            "<text:p text:style-name=\"Body\">"
            "Owned <text:span text:style-name=\"Body\">ODT</text:span>"
            " text</text:p>"
            "<text:list text:style-name=\"Numbers\">"
            "<text:list-item><text:p>List item</text:p></text:list-item>"
            "</text:list>"
            "</office:text></office:body></office:document-content>";
    return buildStoredZipEntries({
        {"mimetype", byteVector(
                "application/vnd.oasis.opendocument.text")},
        {"meta.xml", byteVector(meta)},
        {"styles.xml", byteVector(styles)},
        {"content.xml", byteVector(content)},
    });
}

static std::string validOdtStyles() {
    return
            "<?xml version=\"1.0\" encoding=\"utf-8\"?>"
            "<office:document-styles"
            " xmlns:office=\"urn:oasis:names:tc:opendocument:"
            "xmlns:office:1.0\""
            " xmlns:style=\"urn:oasis:names:tc:opendocument:"
            "xmlns:style:1.0\""
            " xmlns:text=\"urn:oasis:names:tc:opendocument:"
            "xmlns:text:1.0\""
            " xmlns:fo=\"urn:oasis:names:tc:opendocument:"
            "xmlns:xsl-fo-compatible:1.0\">"
            "<office:styles>"
            "<style:default-style style:family=\"paragraph\">"
            "<style:text-properties fo:language=\"en-US\"/>"
            "</style:default-style>"
            "<style:style style:name=\"Body\""
            " style:family=\"paragraph\">"
            "<style:paragraph-properties fo:text-align=\"center\"/>"
            "<style:text-properties fo:font-weight=\"bold\"/>"
            "</style:style>"
            "<text:list-style style:name=\"Numbers\">"
            "<text:list-level-style-number text:level=\"1\""
            " style:num-format=\"1\" text:start-value=\"3\"/>"
            "</text:list-style>"
            "</office:styles></office:document-styles>";
}

static std::vector<unsigned char> deflateRaw(
        const std::vector<unsigned char> &payload) {
    z_stream stream = {};
    if (deflateInit2(&stream, Z_DEFAULT_COMPRESSION, Z_DEFLATED,
            -MAX_WBITS, 8, Z_DEFAULT_STRATEGY) != Z_OK)
        return {};

    std::vector<unsigned char> compressed(compressBound(payload.size()));
    stream.next_in = const_cast<Bytef *>(payload.data());
    stream.avail_in = static_cast<uInt>(payload.size());
    stream.next_out = compressed.data();
    stream.avail_out = static_cast<uInt>(compressed.size());
    const int result = deflate(&stream, Z_FINISH);
    const std::size_t compressedSize = stream.total_out;
    deflateEnd(&stream);
    if (result != Z_STREAM_END)
        return {};
    compressed.resize(compressedSize);
    return compressed;
}

static std::vector<unsigned char> buildDeflatedZip(
        const std::string &name, const std::vector<unsigned char> &payload,
        bool storeCrc) {
    const std::vector<unsigned char> compressed = deflateRaw(payload);
    if (compressed.empty())
        return {};
    const std::uint32_t checksum = storeCrc
            ? static_cast<std::uint32_t>(
                    ::crc32(0, payload.data(), payload.size()))
            : 0;

    std::vector<unsigned char> bytes;
    appendLe32(bytes, 0x04034b50);
    appendLe16(bytes, 20);
    appendLe16(bytes, 0);
    appendLe16(bytes, 8);
    appendLe16(bytes, 0);
    appendLe16(bytes, 0);
    appendLe32(bytes, checksum);
    appendLe32(bytes, static_cast<std::uint32_t>(compressed.size()));
    appendLe32(bytes, static_cast<std::uint32_t>(payload.size()));
    appendLe16(bytes, static_cast<std::uint16_t>(name.size()));
    appendLe16(bytes, 0);
    bytes.insert(bytes.end(), name.begin(), name.end());
    bytes.insert(bytes.end(), compressed.begin(), compressed.end());

    const std::uint32_t centralOffset =
            static_cast<std::uint32_t>(bytes.size());
    appendLe32(bytes, 0x02014b50);
    appendLe16(bytes, 20);
    appendLe16(bytes, 20);
    appendLe16(bytes, 0);
    appendLe16(bytes, 8);
    appendLe16(bytes, 0);
    appendLe16(bytes, 0);
    appendLe32(bytes, checksum);
    appendLe32(bytes, static_cast<std::uint32_t>(compressed.size()));
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

static bool verifyStreamRange(LVStreamRef stream,
        const std::vector<unsigned char> &expected,
        std::size_t offset, std::size_t count) {
    if (stream->SetPos(offset) != offset)
        return false;
    std::vector<unsigned char> actual(count);
    lvsize_t bytesRead = 0;
    if (stream->Read(actual.data(), count, &bytesRead) != LVERR_OK
            || bytesRead != count)
        return false;
    for (std::size_t i = 0; i < count; ++i) {
        if (actual[i] != expected[offset + i])
            return false;
    }
    return true;
}

static int testOdtOwnership() {
    class ScopedFontManager {
        bool _owned;
    public:
        explicit ScopedFontManager(bool owned) : _owned(owned) {}
        ~ScopedFontManager() {
            if (_owned)
                ShutdownFontManager();
        }
    };

    const bool ownsFontManager = fontMan == NULL;
    if (ownsFontManager
            && (!InitFontManager(lString8::empty_str) || !fontMan))
        return fail("ODT ownership fixture could not initialize fonts");
    ScopedFontManager fontManagerScope(ownsFontManager);
    const lString8 fontPath(
            COOLREADER_SOURCE_DIR
            "/thirdparty/harfbuzz-14.2.1/test/subset/data/expected/"
            "retain-num-glyphs/Roboto-Regular.retain-num-glyphs.all.ttf");
    if (!fontMan->RegisterFont(fontPath))
        return fail("ODT ownership fixture font did not register");

    std::vector<unsigned char> archiveBytes =
            buildOdtFixture(validOdtStyles());
    LVStreamRef stream = LVCreateMemoryStream(
            archiveBytes.data(), static_cast<int>(archiveBytes.size()),
            true, LVOM_READ);
    if (stream.isNull() || !DetectOpenDocumentFormat(stream))
        return fail("ODT ownership fixture was not detected");
    if (stream->SetPos(0) != 0)
        return fail("ODT ownership fixture could not rewind");

    LVDocView view(16, true);
    view.setShowCover(false);
    if (!view.LoadDocument(stream, U"owned-fixture.odt", false))
        return fail("ODT ownership fixture did not import");
    ldomDocument *imported = view.getDocument();
    CRPropRef properties = imported->getProps();
    ldomNode *body =
            imported->nodeFromXPath(cs32("FictionBook/body"));
    ldomNode *paragraph =
            imported->nodeFromXPath(cs32("FictionBook/body/section/p"));
    ldomNode *list =
            imported->nodeFromXPath(cs32("FictionBook/body/section/ol"));
    if (properties->getStringDef(DOC_PROP_TITLE) != U"Owned ODT"
            || properties->getStringDef(DOC_PROP_AUTHORS) != U"ODT Owner"
            || properties->getStringDef(DOC_PROP_DESCRIPTION)
                    != U"Scoped metadata DOM"
            || properties->getStringDef(DOC_PROP_LANGUAGE) != U"en"
            || !body
            || body->getText(U' ').pos(U"Owned ODT text") < 0
            || body->getText(U' ').pos(U"List item") < 0
            || !paragraph
            || paragraph->getAttributeValue("style").pos(
                    U"text-align: center") < 0
            || !list
            || list->getAttributeValue("style").pos(
                    U"list-style-type: decimal") < 0
            || list->getAttributeValue("start") != U"3")
        return fail("ODT scoped owners did not publish the complete import");

    std::string rejectedStyles = validOdtStyles();
    const std::size_t closePos =
            rejectedStyles.rfind("</office:styles>");
    if (closePos == std::string::npos)
        return fail("ODT rejected-style fixture could not be assembled");
    std::string deep;
    for (unsigned i = 0;
            i <= ParseBudgetLimits::defaults().maxXmlDepth; ++i)
        deep += "<n>";
    for (unsigned i = 0;
            i <= ParseBudgetLimits::defaults().maxXmlDepth; ++i)
        deep += "</n>";
    rejectedStyles.insert(closePos, deep);

    std::vector<unsigned char> rejectedArchive =
            buildOdtFixture(rejectedStyles);
    LVStreamRef rejectedStream = LVCreateMemoryStream(
            rejectedArchive.data(),
            static_cast<int>(rejectedArchive.size()),
            true, LVOM_READ);
    if (view.LoadDocument(
            rejectedStream, U"rejected-fixture.odt", false))
        return fail("ODT style candidates survived a rejected parse");

    LVStreamRef repeatedStream = LVCreateMemoryStream(
            archiveBytes.data(), static_cast<int>(archiveBytes.size()),
            true, LVOM_READ);
    if (!view.LoadDocument(
            repeatedStream, U"repeated-fixture.odt", false)
            || view.getTitle() != U"Owned ODT")
        return fail("ODT rejected parse retained shared ownership state");
    return 0;
}

static int testOpcFb3Ownership() {
    class ScopedFontManager {
        bool _owned;
    public:
        explicit ScopedFontManager(bool owned) : _owned(owned) {}
        ~ScopedFontManager() {
            if (_owned)
                ShutdownFontManager();
        }
    };

    const bool ownsFontManager = fontMan == NULL;
    if (ownsFontManager
            && (!InitFontManager(lString8::empty_str) || !fontMan))
        return fail("FB3 ownership fixture could not initialize fonts");
    ScopedFontManager fontManagerScope(ownsFontManager);

    const std::string body =
            "<?xml version=\"1.0\" encoding=\"utf-8\"?>"
            "<fb3-body><section><title><p>Chapter</p></title>"
            "<p>Hello FB3</p><image src=\"img1\"/>"
            "</section></fb3-body>";
    std::vector<unsigned char> archiveBytes = buildFb3Fixture(body);
    LVStreamRef stream = LVCreateMemoryStream(
            archiveBytes.data(), static_cast<int>(archiveBytes.size()),
            true, LVOM_READ);
    if (stream.isNull() || !DetectFb3Format(stream))
        return fail("FB3/OPC ownership fixture was not detected");
    if (stream->SetPos(0) != 0)
        return fail("FB3/OPC fixture could not rewind");

    LVContainerRef archive = LVOpenArchieve(stream);
    if (archive.isNull())
        return fail("FB3/OPC ownership fixture could not open");
    OpcPackage package(archive);
    if (package.getContentPartName(U"application/fb3-body+xml")
                    != U"/body.xml"
            || !package.partExist(U"/body.xml"))
        return fail("OPC content-type snapshot was not published");

    CRPropRef properties = LVCreatePropsContainer();
    package.readCoreProperties(properties);
    if (properties->getStringDef(DOC_PROP_TITLE) != U"Fixture Title"
            || properties->getStringDef(DOC_PROP_AUTHORS)
                    != U"Fixture Author"
            || properties->getStringDef(DOC_PROP_LANGUAGE) != U"core-lang"
            || properties->getStringDef(DOC_PROP_DESCRIPTION)
                    != U"Fixture Description")
        return fail("OPC core-properties document was not retained");

    fb3ImportContext context(&package);
    ldomDocument *description = context.getDescription();
    if (!description
            || context.getDescription() != description
            || description->textFromXPath(
                    cs32("fb3-description/lang")) != U"en-US")
        return fail("FB3 description owner did not retain its cached view");
    if (context.openBook().isNull()
            || context.m_coverImage != U"images/cover.png"
            || context.geImageTarget(U"img1") != U"images/pic.png")
        return fail("OPC relation owners did not retain indexed targets");

    if (stream->SetPos(0) != 0)
        return fail("FB3 import fixture could not rewind");
    std::unique_ptr<ldomDocument> imported(new ldomDocument());
    if (!ImportFb3Document(stream, imported.get(), NULL, NULL)
            || imported->getProps()->getStringDef(DOC_PROP_TITLE)
                    != U"Fixture Title"
            || imported->getProps()->getStringDef(DOC_PROP_LANGUAGE)
                    != U"en-US"
            || !imported->nodeFromXPath(cs32("FictionBook/body")))
        return fail("FB3 import did not publish its complete document");

    std::string deepXml = "<root>";
    for (unsigned i = 0;
            i <= ParseBudgetLimits::defaults().maxXmlDepth; ++i)
        deepXml += "<n>";
    for (unsigned i = 0;
            i <= ParseBudgetLimits::defaults().maxXmlDepth; ++i)
        deepXml += "</n>";
    deepXml += "</root>";
    std::unique_ptr<ldomDocument> rejected(
            LVParseXMLStream(memoryStream(deepXml)));
    if (rejected)
        return fail("XML document factory published a rejected candidate");

    std::unique_ptr<ldomDocument> html(LVParseHTMLStream(
            memoryStream("<!doctype html><html><head><title>x</title></head>"
                         "<body>owned</body></html>")));
    if (!html)
        return fail("HTML document factory did not transfer its owner");

    std::string deepBody = "<fb3-body>";
    for (unsigned i = 0;
            i <= ParseBudgetLimits::defaults().maxXmlDepth; ++i)
        deepBody += "<section>";
    for (unsigned i = 0;
            i <= ParseBudgetLimits::defaults().maxXmlDepth; ++i)
        deepBody += "</section>";
    deepBody += "</fb3-body>";
    std::vector<unsigned char> rejectedArchive = buildFb3Fixture(deepBody);
    LVStreamRef rejectedStream = LVCreateMemoryStream(
            rejectedArchive.data(), static_cast<int>(rejectedArchive.size()),
            true, LVOM_READ);
    std::unique_ptr<ldomDocument> rejectedImport(new ldomDocument());
    if (ImportFb3Document(
            rejectedStream, rejectedImport.get(), NULL, NULL))
        return fail("FB3 parser published a depth-budget failure");
    return 0;
}

static int testChmOwnership() {
#if CHM_SUPPORT_ENABLED==1
    const lString32 fixturePath = Utf8ToUnicode(lString8(
            COOLREADER_SOURCE_DIR
            "/thirdparty/zlib-1.3.2/contrib/dotzlib/DotZLib.chm"));
    LVStreamRef source = LVOpenFileStream(fixturePath.c_str(), LVOM_READ);
    if (source.isNull() || !DetectCHMFormat(source))
        return fail("CHM ownership fixture was not detected");
    if (source->SetPos(0) != 0)
        return fail("CHM ownership fixture could not rewind");

    LVContainerRef container = LVOpenCHMContainer(source);
    if (container.isNull() || container->GetObjectCount() <= 0)
        return fail("CHM ownership fixture could not open");
    lString32 entryName;
    lvsize_t entrySize = 0;
    for (int i = 0; i < container->GetObjectCount(); ++i) {
        const LVContainerItemInfo *candidate =
                container->GetObjectInfo(i);
        if (candidate && !candidate->IsContainer()
                && candidate->GetSize() > 0) {
            LVStreamRef probe = container->OpenStream(
                    candidate->GetName(), LVOM_READ);
            unsigned char probeByte = 0;
            lvsize_t probeRead = 0;
            if (!probe.isNull()
                    && probe->Read(&probeByte, 1, &probeRead) == LVERR_OK
                    && probeRead == 1) {
                entryName = candidate->GetName();
                entrySize = candidate->GetSize();
                break;
            }
        }
    }
    if (entryName.empty())
        return fail("CHM ownership fixture has no readable entry");
    LVStreamRef entry = container->OpenStream(
            entryName.c_str(), LVOM_READ);
    source.Clear();
    container.Clear();
    unsigned char firstByte = 0;
    lvsize_t bytesRead = 0;
    const lverror_t entryReadStatus = entry.isNull()
            ? LVERR_FAIL
            : entry->Read(&firstByte, 1, &bytesRead);
    if (entry.isNull() || entryReadStatus != LVERR_OK || bytesRead != 1) {
        std::fprintf(stderr,
                "CHM detached entry '%s': size=%llu status=%d read=%llu\n",
                UnicodeToUtf8(entryName).c_str(),
                static_cast<unsigned long long>(entrySize),
                static_cast<int>(entryReadStatus),
                static_cast<unsigned long long>(bytesRead));
        return fail("CHM entry did not retain its CHM file owner");
    }
    entry.Clear();

    std::unique_ptr<ldomDocument> parent(new ldomDocument());
    std::unique_ptr<ldomDocument> parsed(LVParseCHMHTMLStream(
            memoryStream("<!doctype html><html><head><title>x</title>"
                         "</head><body>owned</body></html>"),
            U"utf-8", parent.get()));
    if (!parsed)
        return fail("CHM HTML factory did not transfer its document");
    std::unique_ptr<ldomDocument> rejected(LVParseCHMHTMLStream(
            memoryStream("ordinary text without HTML markup"),
            U"utf-8", parent.get()));
    if (rejected)
        return fail("CHM HTML factory published a rejected candidate");

    if (!LVRunChmMetadataOwnershipRegression())
        return fail("CHM metadata owner chain did not complete");
    if (!LVOpenCHMContainer(
            memoryStream("not a CHM container")).isNull())
        return fail("CHM container factory published a failed candidate");
#endif
    return 0;
}

static int testArchiveContainerOwnership() {
    class PayloadFailingMemoryStream : public LVMemoryStream {
        lvpos_t _payloadStart;
        lvpos_t _payloadEnd;
    public:
        PayloadFailingMemoryStream(
                lvpos_t payloadStart, lvpos_t payloadEnd)
            : _payloadStart(payloadStart)
            , _payloadEnd(payloadEnd)
        {
        }

        lverror_t Read(
                void *buf, lvsize_t count,
                lvsize_t *bytesRead) override
        {
            if (count > 0
                    && m_pos >= _payloadStart
                    && m_pos < _payloadEnd) {
                if (bytesRead)
                    *bytesRead = 0;
                return LVERR_FAIL;
            }
            return LVMemoryStream::Read(buf, count, bytesRead);
        }
    };

    const std::vector<unsigned char> payload =
            {'a', 'r', 'c', 'h', 'i', 'v', 'e'};
    std::vector<unsigned char> zipBytes =
            buildStoredZip("entry.txt", payload);
    LVStreamRef source = LVCreateMemoryStream(
            zipBytes.data(), static_cast<int>(zipBytes.size()),
            true, LVOM_READ);
    if (!source.isNull())
        source->SetName(U"fixture.zip");
    if (source.isNull() || source->SetPos(7) != 7)
        return fail("archive ownership fixture could not position its source");

    LVContainerRef archive = LVOpenArchieve(source);
    lvsize_t itemCount = 0;
    const LVContainerItemInfo *item =
            archive.isNull() ? NULL : archive->GetObjectInfo(0);
    if (archive.isNull()
            || source->GetPos() != 7
            || archive->GetParentContainer() != NULL
            || archive->GetSize(&itemCount) != LVERR_OK
            || itemCount != 1
            || archive->GetObjectCount() != 1
            || !item || !item->GetName()
            || lString32(item->GetName()) != lString32(U"entry.txt")
            || item->IsContainer()
            || item->GetSize() != payload.size()) {
        return fail("archive candidate metadata or position rollback failed");
    }

    source.Clear();
    LVStreamRef decoded =
            archive->OpenStream(U"entry.txt", LVOM_READ);
    archive.Clear();
    std::vector<unsigned char> oversized(payload.size() + 8, 0xcd);
    lvsize_t oversizedRead = 0;
    if (decoded.isNull()
            || decoded->Read(
                    oversized.data(), oversized.size(), &oversizedRead)
                    != LVERR_OK
            || oversizedRead != payload.size()
            || !decoded->Eof()
            || !std::equal(
                    payload.begin(), payload.end(), oversized.begin())
            || oversized[payload.size()] != 0xcd)
        return fail("archive/entry did not retain its source ownership");
    decoded.Clear();

    const std::vector<unsigned char> fragmentSourceBytes =
            {0x10, 0x20, 0x30, 0x40, 0x50, 0x60};
    LVStreamRef fragmentSource = LVCreateMemoryStream(
            const_cast<unsigned char *>(fragmentSourceBytes.data()),
            static_cast<int>(fragmentSourceBytes.size()),
            true, LVOM_READ);
    std::unique_ptr<LVStreamFragment> fragmentOwner(
            new LVStreamFragment(fragmentSource, 2, 3, 7));
    LVStreamRef fragment(fragmentOwner.release());
    unsigned char fragmentBytes[6] =
            {0xee, 0xee, 0xee, 0xee, 0xee, 0xee};
    lvsize_t fragmentRead = 99;
    if (fragment->GetMode() != LVOM_READ
            || fragment->GetContainerDepth() != 7
            || fragment->Read(
                    fragmentBytes, sizeof(fragmentBytes), &fragmentRead)
                    != LVERR_OK
            || fragmentRead != 3
            || fragmentBytes[0] != 0x30
            || fragmentBytes[2] != 0x50
            || fragmentBytes[3] != 0xee
            || fragment->Seek(-1, LVSEEK_END, NULL) != LVERR_OK
            || fragment->GetPos() != 2
            || fragment->Seek(1, LVSEEK_END, NULL) != LVERR_FAIL
            || fragment->GetPos() != 2
            || fragment->Seek(-4, LVSEEK_END, NULL) != LVERR_FAIL
            || fragment->GetPos() != 2
            || fragment->Seek(0, LVSEEK_SET, NULL) != LVERR_OK) {
        return fail("stream fragment escaped its bounded seek/read region");
    }
    fragmentRead = 99;
    if (fragment->Read(NULL, 1, &fragmentRead) != LVERR_FAIL
            || fragmentRead != 0
            || fragment->GetPos() != 0)
        return fail("stream fragment failure changed position or byte count");
    std::unique_ptr<LVStreamFragment> overflowFragmentOwner(
            new LVStreamFragment(
                    fragmentSource, LV_INVALID_SIZE, 2));
    LVStreamRef overflowFragment(overflowFragmentOwner.release());
    fragmentRead = 99;
    if (overflowFragment->Read(
                fragmentBytes, 1, &fragmentRead) != LVERR_FAIL
            || fragmentRead != 0
            || overflowFragment->Seek(
                    0, LVSEEK_SET, NULL) != LVERR_FAIL)
        return fail("stream fragment accepted an overflowing source region");

    unsigned char nonArchiveBytes[] =
            {'N', 'O', 'T', '!', 'd', 'a', 't', 'a'};
    LVStreamRef nonArchive = LVCreateMemoryStream(
            nonArchiveBytes, sizeof(nonArchiveBytes), true, LVOM_READ);
    if (nonArchive->SetPos(3) != 3
            || !LVOpenArchieve(nonArchive).isNull()
            || nonArchive->GetPos() != 3)
        return fail("failed archive probe did not restore caller position");

    std::vector<unsigned char> corrupt(64, 0);
    corrupt[0] = 'P';
    corrupt[1] = 'K';
    corrupt[2] = 3;
    corrupt[3] = 4;
    LVStreamRef corruptSource = LVCreateMemoryStream(
            corrupt.data(), static_cast<int>(corrupt.size()),
            true, LVOM_READ);
    for (int attempt = 0; attempt < 3; ++attempt) {
        if (corruptSource->SetPos(5) != 5
                || !LVOpenArchieve(corruptSource).isNull()
                || corruptSource->GetPos() != 5)
            return fail("corrupt archive candidate leaked partial state");
    }

    std::vector<unsigned char> malformedZip64 =
            buildStoredZip("entry.txt", payload);
    const std::size_t localNameEnd = 30 + std::strlen("entry.txt");
    malformedZip64[22] = 0xff;
    malformedZip64[23] = 0xff;
    malformedZip64[24] = 0xff;
    malformedZip64[25] = 0xff;
    malformedZip64[28] = 4;
    malformedZip64[29] = 0;
    malformedZip64.insert(
            malformedZip64.begin() + localNameEnd,
            {0x01, 0x00, 0x1c, 0x00});
    const std::size_t eocdOffset = malformedZip64.size() - 22;
    const std::uint32_t originalCentralOffset =
            static_cast<std::uint32_t>(
                    30 + std::strlen("entry.txt") + payload.size());
    const std::uint32_t shiftedCentralOffset = originalCentralOffset + 4;
    malformedZip64[eocdOffset + 16] =
            static_cast<unsigned char>(shiftedCentralOffset);
    malformedZip64[eocdOffset + 17] =
            static_cast<unsigned char>(shiftedCentralOffset >> 8);
    malformedZip64[eocdOffset + 18] =
            static_cast<unsigned char>(shiftedCentralOffset >> 16);
    malformedZip64[eocdOffset + 19] =
            static_cast<unsigned char>(shiftedCentralOffset >> 24);
    LVContainerRef malformedArchive =
            LVOpenArchieve(LVCreateMemoryStream(
                    malformedZip64.data(),
                    static_cast<int>(malformedZip64.size()),
                    true, LVOM_READ));
    if (malformedArchive.isNull()
            || !malformedArchive->OpenStream(
                    U"entry.txt", LVOM_READ).isNull())
        return fail("truncated ZIP64 local extra data was published");

    const std::vector<unsigned char> deflatedPayload(256, 0x5a);
    const std::string failingName("broken.bin");
    std::vector<unsigned char> failingZip =
            buildDeflatedZip(failingName, deflatedPayload, false);
    if (failingZip.size() < 22)
        return fail("failing ZIP decoder fixture was not built");
    const std::size_t failingEocd = failingZip.size() - 22;
    const lvpos_t failingCentralOffset =
            static_cast<lvpos_t>(failingZip[failingEocd + 16])
            | (static_cast<lvpos_t>(failingZip[failingEocd + 17]) << 8)
            | (static_cast<lvpos_t>(failingZip[failingEocd + 18]) << 16)
            | (static_cast<lvpos_t>(failingZip[failingEocd + 19]) << 24);
    const lvpos_t failingPayloadStart =
            30 + static_cast<lvpos_t>(failingName.size());
    std::unique_ptr<PayloadFailingMemoryStream> failingSourceOwner(
            new PayloadFailingMemoryStream(
                    failingPayloadStart, failingCentralOffset));
    if (failingSourceOwner->CreateCopy(
                failingZip.data(), failingZip.size(), LVOM_READ) != LVERR_OK)
        return fail("failing ZIP decoder source could not initialize");
    LVStreamRef failingSource(failingSourceOwner.release());
    LVContainerRef failingArchive = LVOpenArchieve(failingSource);
    if (failingArchive.isNull())
        return fail("failing ZIP decoder central directory was rejected");
    for (int attempt = 0; attempt < 3; ++attempt) {
        if (!failingArchive->OpenStream(
                    Utf8ToUnicode(lString8(failingName.c_str())).c_str(),
                    LVOM_READ).isNull())
            return fail("failed inflater input published a decoder candidate");
    }
    return 0;
}

class FailingBufferStream : public LVStream {
private:
    lvpos_t _pos;
    int &_writeCalls;

public:
    explicit FailingBufferStream(int &writeCalls)
        : _pos(0), _writeCalls(writeCalls)
    {
    }

    lvopen_mode_t GetMode() override { return LVOM_READWRITE; }

    lverror_t Seek(lvoffset_t offset, lvseek_origin_t origin,
            lvpos_t *newPos) override
    {
        lvoffset_t target = offset;
        if (origin == LVSEEK_CUR)
            target += static_cast<lvoffset_t>(_pos);
        else if (origin == LVSEEK_END)
            target += 8;
        if (target < 0 || target > 8)
            return LVERR_FAIL;
        _pos = static_cast<lvpos_t>(target);
        if (newPos)
            *newPos = _pos;
        return LVERR_OK;
    }

    lverror_t GetSize(lvsize_t *size) override
    {
        *size = 8;
        return LVERR_OK;
    }

    lverror_t SetSize(lvsize_t) override { return LVERR_FAIL; }

    lverror_t Read(void *, lvsize_t, lvsize_t *bytesRead) override
    {
        if (bytesRead)
            *bytesRead = 0;
        return LVERR_FAIL;
    }

    lverror_t Write(const void *, lvsize_t count,
            lvsize_t *bytesWritten) override
    {
        ++_writeCalls;
        if (bytesWritten)
            *bytesWritten = count;
        return LVERR_OK;
    }

    bool Eof() override { return false; }
};

static int testMemoryStreamOwnership() {
    std::vector<unsigned char> payload(12000);
    for (std::size_t i = 0; i < payload.size(); ++i)
        payload[i] = static_cast<unsigned char>((i * 29 + 5) % 251);

    LVStreamRef owned = LVCreateMemoryStream();
    lvsize_t bytesWritten = 0;
    if (owned.isNull()
            || owned->Write(payload.data(), payload.size(), &bytesWritten)
                    != LVERR_OK
            || bytesWritten != payload.size()
            || owned->GetSize() != payload.size()
            || !verifyStreamRange(owned, payload, 0, payload.size()))
        return fail("owned memory stream did not grow through RAII storage");
    bytesWritten = 99;
    if (owned->Write(
                payload.data(), LV_INVALID_SIZE, &bytesWritten) != LVERR_FAIL
            || bytesWritten != 0
            || owned->GetSize() != payload.size())
        return fail("memory stream growth overflow changed owned storage");

    unsigned char external[] = {0x10, 0x20, 0x30, 0x40};
    {
        LVStreamRef borrowed = LVCreateMemoryStream(
                external, static_cast<int>(sizeof(external)),
                false, LVOM_READWRITE);
        external[1] = 0x2a;
        unsigned char actual[sizeof(external)] = {};
        lvsize_t bytesRead = 0;
        lvsize_t rejectedWrite = 99;
        if (borrowed.isNull()
                || borrowed->GetMode() != LVOM_READ
                || borrowed->Read(actual, sizeof(actual), &bytesRead)
                        != LVERR_OK
                || bytesRead != sizeof(actual)
                || actual[1] != 0x2a
                || borrowed->SetSize(sizeof(external) + 1) != LVERR_FAIL
                || borrowed->Write(
                        payload.data(), 1, &rejectedWrite) != LVERR_FAIL
                || rejectedWrite != 0)
            return fail("borrowed memory stream lost its readonly alias");
    }
    if (external[0] != 0x10 || external[1] != 0x2a)
        return fail("borrowed memory stream modified or freed caller storage");

    std::vector<unsigned char> source = {1, 2, 3, 4};
    LVStreamRef copied = LVCreateMemoryStream(
            source.data(), static_cast<int>(source.size()),
            true, LVOM_READWRITE);
    source[0] = 0xff;
    std::vector<unsigned char> growth(9000, 0x5c);
    if (copied.isNull()
            || copied->SetPos(0) != 0)
        return fail("copied memory stream was not initialized");
    unsigned char copiedPrefix[4] = {};
    lvsize_t copiedRead = 0;
    if (copied->Read(
                copiedPrefix, sizeof(copiedPrefix), &copiedRead) != LVERR_OK
            || copiedRead != sizeof(copiedPrefix)
            || copiedPrefix[0] != 1
            || copied->SetPos(copied->GetSize()) != source.size()
            || copied->Write(growth.data(), growth.size(), &bytesWritten)
                    != LVERR_OK
            || bytesWritten != growth.size()
            || copied->GetSize() != source.size() + growth.size())
        return fail("copied memory stream did not own or grow its snapshot");

    LVMemoryStream lifecycle;
    if (lifecycle.Create() != LVERR_OK
            || lifecycle.Open(external, sizeof(external)) != LVERR_OK
            || lifecycle.Close() != LVERR_OK
            || lifecycle.Close() != LVERR_OK
            || lifecycle.GetMode() != LVOM_CLOSED)
        return fail("memory stream close/reopen lifecycle is not idempotent");

    int ignoredWrites = 0;
    LVStreamRef failingSource(new FailingBufferStream(ignoredWrites));
    if (!LVCreateMemoryStream(failingSource).isNull())
        return fail("memory stream factory published a failed short copy");
    if (!LVCreateMemoryStream(
                external, -1, true, LVOM_READ).isNull())
        return fail("memory stream factory accepted a negative buffer size");

    LVStreamRef empty = LVCreateStringStream(lString8());
    if (empty.isNull() || empty->GetSize() != 0)
        return fail("memory stream rejected an owned empty string");
    return 0;
}

class FailOnceMemoryStream : public LVMemoryStream {
private:
    bool _failNextWrite;

public:
    FailOnceMemoryStream()
        : _failNextWrite(false)
    {
    }

    void failNextWrite()
    {
        _failNextWrite = true;
    }

    lverror_t Write(const void *buf, lvsize_t count,
            lvsize_t *bytesWritten) override
    {
        if (_failNextWrite) {
            _failNextWrite = false;
            if (bytesWritten)
                *bytesWritten = 0;
            return LVERR_FAIL;
        }
        return LVMemoryStream::Write(buf, count, bytesWritten);
    }
};

static int testBlockWriteStreamOwnership() {
    std::vector<unsigned char> payload(24);
    for (std::size_t i = 0; i < payload.size(); ++i)
        payload[i] = static_cast<unsigned char>(0x30 + i);

    LVStreamRef base = LVCreateMemoryStream();
    LVStreamRef buffered = LVCreateBlockWriteStream(base, 8, 2);
    lvsize_t bytesWritten = 0;
    if (buffered.isNull()
            || buffered->Write(
                    payload.data(), payload.size(), &bytesWritten) != LVERR_OK
            || bytesWritten != payload.size())
        return fail("block write stream rejected multi-block cache growth");

    std::vector<unsigned char> cachedRead(payload.size());
    lvsize_t bytesRead = 0;
    if (buffered->SetPos(0) != 0
            || buffered->Read(
                    cachedRead.data(), cachedRead.size(), &bytesRead)
                    != LVERR_OK
            || bytesRead != cachedRead.size()
            || cachedRead != payload)
        return fail("block write stream did not merge cached and flushed data");
    if (buffered->Flush(true) != LVERR_OK
            || !verifyStreamRange(base, payload, 0, payload.size()))
        return fail("block write stream did not flush its RAII block chain");

    std::vector<unsigned char> replacement(13, 0xa7);
    std::copy(
            replacement.begin(), replacement.end(), payload.begin() + 5);
    if (buffered->SetPos(5) != 5
            || buffered->Write(
                    replacement.data(), replacement.size(), &bytesWritten)
                    != LVERR_OK
            || bytesWritten != replacement.size()
            || buffered->Flush(true) != LVERR_OK
            || !verifyStreamRange(base, payload, 0, payload.size()))
        return fail("block write stream could not reuse its cache after flush");

    std::unique_ptr<FailOnceMemoryStream> flakyOwner =
            std::make_unique<FailOnceMemoryStream>();
    FailOnceMemoryStream *flakyRaw = flakyOwner.get();
    LVStreamRef flakyBase(flakyOwner.release());
    if (flakyRaw->Create() != LVERR_OK)
        return fail("block write rollback fixture could not initialize");
    LVStreamRef rollback = LVCreateBlockWriteStream(flakyBase, 4, 1);
    static const unsigned char first[] = {1, 2, 3, 4};
    static const unsigned char second[] = {5, 6, 7, 8};
    if (rollback.isNull()
            || rollback->Write(
                    first, sizeof(first), &bytesWritten) != LVERR_OK
            || bytesWritten != sizeof(first))
        return fail("block write rollback fixture could not cache a block");
    flakyRaw->failNextWrite();
    bytesWritten = 99;
    if (rollback->Write(
                second, sizeof(second), &bytesWritten) != LVERR_FAIL
            || bytesWritten != 0
            || rollback->GetPos() != sizeof(first))
        return fail("failed block eviction reported committed bytes");
    if (rollback->Flush(true) != LVERR_OK
            || rollback->Write(
                    second, sizeof(second), &bytesWritten) != LVERR_OK
            || bytesWritten != sizeof(second)
            || rollback->Flush(true) != LVERR_OK)
        return fail("failed block eviction did not retain dirty storage");
    const std::vector<unsigned char> rollbackExpected =
            {1, 2, 3, 4, 5, 6, 7, 8};
    if (!verifyStreamRange(
            flakyBase, rollbackExpected, 0, rollbackExpected.size()))
        return fail("block eviction rollback lost dirty data");

    LVStreamRef readOnly = LVCreateMemoryStream(
            payload.data(), static_cast<int>(payload.size()),
            true, LVOM_READ);
    LVStreamRef passThrough =
            LVCreateBlockWriteStream(readOnly, 8, 2);
    if (passThrough.get() != readOnly.get())
        return fail("block write factory wrapped a readonly stream");
    if (!LVCreateBlockWriteStream(base, 0, 2).isNull()
            || !LVCreateBlockWriteStream(base, 8, 0).isNull())
        return fail("block write factory accepted invalid cache bounds");
    return 0;
}

#if defined(_LINUX) || defined(_WIN32)
static int testFileMappedStreamOwnership() {
    char path[] = "/tmp/coolreader-mmap-test-XXXXXX";
    int fd = mkstemp(path);
    if (fd < 0)
        return fail("mapped-stream fixture could not create a file");
    std::vector<unsigned char> expected(64, 0);
    for (std::size_t i = 0; i < 8; ++i)
        expected[i] = static_cast<unsigned char>(i + 1);
    if (write(fd, expected.data(), 8) != static_cast<ssize_t>(8)) {
        close(fd);
        unlink(path);
        return fail("mapped-stream fixture could not write its payload");
    }
    close(fd);

    LVStreamRef mapped = LVMapFileStream(path, LVOM_READ, 0);
    if (mapped.isNull() || mapped->GetSize() != 8) {
        unlink(path);
        return fail("readonly mapped stream rejected a valid file");
    }
    LVStreamBufferRef anchored = mapped->GetReadBuffer(2, 4);
    if (anchored.isNull()
            || anchored->getSize() != 4
            || anchored->getReadOnly()[0] != 3
            || anchored->getReadWrite() != NULL
            || !mapped->GetReadBuffer(LV_INVALID_SIZE, 2).isNull()) {
        anchored.Clear();
        mapped.Clear();
        unlink(path);
        return fail("mapped read-buffer bounds or readonly view are invalid");
    }
    mapped.Clear();
    if (anchored->getReadOnly()[3] != 6) {
        anchored.Clear();
        unlink(path);
        return fail("mapped buffer did not retain its stream owner");
    }
    anchored.Clear();

    LVStreamRef writable = LVMapFileStream(path, LVOM_APPEND, 32);
    if (writable.isNull() || writable->GetSize() != 32) {
        unlink(path);
        return fail("writable mapped stream did not grow to its minimum");
    }
    LVStreamBufferRef writeView = writable->GetWriteBuffer(8, 4);
    lUInt8 *writeData =
            writeView.isNull() ? NULL : writeView->getReadWrite();
    if (!writeData) {
        writeView.Clear();
        writable.Clear();
        unlink(path);
        return fail("writable mapped stream did not expose its mapped view");
    }
    for (int i = 0; i < 4; ++i) {
        writeData[i] = static_cast<lUInt8>(0xa1 + i);
        expected[8 + i] = writeData[i];
    }
    writeView.Clear();

    static const unsigned char directWrite[] =
            {0xb1, 0xb2, 0xb3, 0xb4};
    lvsize_t bytesWritten = 0;
    if (writable->SetPos(12) != 12
            || writable->Write(
                    directWrite, sizeof(directWrite), &bytesWritten)
                    != LVERR_OK
            || bytesWritten != sizeof(directWrite)) {
        writable.Clear();
        unlink(path);
        return fail("mapped stream direct write failed");
    }
    std::copy(
            directWrite, directWrite + sizeof(directWrite),
            expected.begin() + 12);
    if (writable->SetSize(64) != LVERR_OK
            || writable->SetSize(16) != LVERR_FAIL
            || writable->GetSize() != 64
            || writable->Seek(-1, LVSEEK_SET, NULL) != LVERR_FAIL) {
        writable.Clear();
        unlink(path);
        return fail("mapped stream remap or bounds rollback failed");
    }
    writable.Clear();

    LVStreamRef persisted = LVOpenFileStream(path, LVOM_READ);
    if (persisted.isNull()
            || !verifyStreamRange(
                    persisted, expected, 0, expected.size())) {
        persisted.Clear();
        unlink(path);
        return fail("mapped stream teardown did not persist shared writes");
    }
    persisted.Clear();

    LVFileMappedStream reusable;
    const lString32 path32 = Utf8ToUnicode(lString8(path));
    const lString32 missing =
            Utf8ToUnicode(lString8((std::string(path) + ".missing").c_str()));
    if (reusable.OpenFile(path32, LVOM_READ) != LVERR_OK
            || reusable.OpenFile(missing, LVOM_READ) != LVERR_FAIL
            || reusable.GetMode() != LVOM_ERROR
            || reusable.GetSize() != 0) {
        unlink(path);
        return fail("mapped stream reopen failure retained old resources");
    }
    if (!LVMapFileStream(path, LVOM_WRITE, 0).isNull()) {
        unlink(path);
        return fail("mapped stream factory accepted an unsupported mode");
    }
    unlink(path);

    char emptyPath[] = "/tmp/coolreader-empty-mmap-test-XXXXXX";
    int emptyFd = mkstemp(emptyPath);
    if (emptyFd < 0)
        return fail("empty mapped-stream fixture could not create a file");
    close(emptyFd);
    LVStreamRef empty = LVMapFileStream(emptyPath, LVOM_READ, 0);
    unlink(emptyPath);
    if (!empty.isNull())
        return fail("mapped stream published a failed empty-file mapping");
    return 0;
}
#endif

static int testDefaultStreamBufferOwnership() {
    std::vector<unsigned char> payload(12);
    for (std::size_t i = 0; i < payload.size(); ++i)
        payload[i] = static_cast<unsigned char>(i + 1);

    LVStreamRef readStream = LVCreateMemoryStream(
            payload.data(), static_cast<int>(payload.size()),
            true, LVOM_READ);
    LVStreamBufferRef readBuffer = readStream->GetReadBuffer(3, 4);
    if (readBuffer.isNull()
            || readBuffer->getSize() != 4
            || readBuffer->getReadWrite() != NULL)
        return fail("default read buffer rejected a nonzero region");
    const lUInt8 *readData = readBuffer->getReadOnly();
    if (!readData || readData[0] != 4 || readData[3] != 7)
        return fail("default read buffer exposed the wrong region");
    if (!readBuffer->close()
            || readBuffer->getReadOnly() != NULL
            || readBuffer->getSize() != 0
            || !readBuffer->close())
        return fail("default read buffer close is not idempotent");

    LVStreamRef writeStream = LVCreateMemoryStream(
            payload.data(), static_cast<int>(payload.size()),
            true, LVOM_READWRITE);
    LVStreamBufferRef writeBuffer = writeStream->GetWriteBuffer(4, 3);
    lUInt8 *writeData = writeBuffer.isNull()
            ? NULL : writeBuffer->getReadWrite();
    if (!writeData || writeData[0] != 5 || writeData[2] != 7)
        return fail("default write buffer did not preload its region");
    writeData[0] = 0xa1;
    writeData[1] = 0xa2;
    writeData[2] = 0xa3;
    if (!writeBuffer->close() || !writeBuffer->close())
        return fail("default write buffer did not flush exactly once");
    std::vector<unsigned char> expected = payload;
    expected[4] = 0xa1;
    expected[5] = 0xa2;
    expected[6] = 0xa3;
    if (!verifyStreamRange(writeStream, expected, 0, expected.size()))
        return fail("default write buffer flushed to the wrong offset");

    {
        LVStreamBufferRef destructorBuffer =
                writeStream->GetWriteBuffer(8, 2);
        lUInt8 *destructorData = destructorBuffer.isNull()
                ? NULL : destructorBuffer->getReadWrite();
        if (!destructorData)
            return fail("default write buffer destructor fixture failed");
        destructorData[0] = 0xb1;
        destructorData[1] = 0xb2;
    }
    expected[8] = 0xb1;
    expected[9] = 0xb2;
    if (!verifyStreamRange(writeStream, expected, 0, expected.size()))
        return fail("default write buffer destructor did not flush");

    std::vector<unsigned char> writeOnlyBytes(8, 0xcc);
    LVStreamRef writeOnlyStream = LVCreateMemoryStream(
            writeOnlyBytes.data(), static_cast<int>(writeOnlyBytes.size()),
            true, LVOM_WRITE);
    LVStreamBufferRef writeOnlyBuffer =
            writeOnlyStream->GetWriteBuffer(2, 3);
    lUInt8 *writeOnlyData = writeOnlyBuffer.isNull()
            ? NULL : writeOnlyBuffer->getReadWrite();
    if (!writeOnlyData || writeOnlyBuffer->getReadOnly() != NULL)
        return fail("write-only stream buffer attempted a read preload");
    writeOnlyData[0] = 0x31;
    writeOnlyData[1] = 0x32;
    writeOnlyData[2] = 0x33;
    if (!writeOnlyBuffer->close()
            || writeOnlyStream->SetMode(LVOM_READ) != LVERR_OK)
        return fail("write-only stream buffer did not flush");
    std::vector<unsigned char> writeOnlyExpected(8, 0xcc);
    writeOnlyExpected[2] = 0x31;
    writeOnlyExpected[3] = 0x32;
    writeOnlyExpected[4] = 0x33;
    if (!verifyStreamRange(
            writeOnlyStream, writeOnlyExpected, 0, writeOnlyExpected.size()))
        return fail("write-only stream buffer flushed the wrong bytes");

    int rollbackWrites = 0;
    LVStreamRef failingStream(new FailingBufferStream(rollbackWrites));
    LVStreamBufferRef failedBuffer =
            failingStream->GetWriteBuffer(2, 4);
    if (!failedBuffer.isNull() || rollbackWrites != 0)
        return fail("failed stream-buffer construction flushed partial data");
    if (!failingStream->GetReadBuffer(2, 0).isNull())
        return fail("default stream buffer accepted an empty region");
    return 0;
}

static std::vector<unsigned char> buildTcrFixture(
        std::vector<unsigned char> &decoded) {
    static const unsigned char signature[] = {
        '!', '!', '8', '-', 'B', 'i', 't', '!', '!'
    };
    std::vector<std::vector<unsigned char>> codes(256);
    codes[1].resize(255);
    for (std::size_t i = 0; i < codes[1].size(); ++i)
        codes[1][i] = static_cast<unsigned char>((i * 13 + 7) % 251);
    for (std::size_t i = 2; i < codes.size(); ++i)
        codes[i].push_back(static_cast<unsigned char>(i));

    std::vector<unsigned char> bytes(signature,
            signature + sizeof(signature));
    for (const std::vector<unsigned char> &code : codes) {
        bytes.push_back(static_cast<unsigned char>(code.size()));
        bytes.insert(bytes.end(), code.begin(), code.end());
    }

    std::vector<unsigned char> packed(4096, 1);
    packed.insert(packed.end(), 731, 2);
    decoded.clear();
    decoded.reserve(4096 * codes[1].size() + 731);
    for (unsigned char code : packed)
        decoded.insert(decoded.end(), codes[code].begin(), codes[code].end());
    bytes.insert(bytes.end(), packed.begin(), packed.end());
    return bytes;
}

static int testTcrStreamOwnership() {
    std::vector<unsigned char> expected;
    std::vector<unsigned char> tcrBytes = buildTcrFixture(expected);
    LVStreamRef decoded = LVCreateTCRDecoderStream(LVCreateMemoryStream(
            tcrBytes.data(), static_cast<int>(tcrBytes.size()),
            true, LVOM_READ));
    const std::size_t blockBoundary = 4096 * 255;
    if (decoded.isNull()
            || decoded->GetSize() != expected.size()
            || !verifyStreamRange(decoded, expected, 0, 12000)
            || !verifyStreamRange(
                    decoded, expected, blockBoundary - 200, 400)
            || !verifyStreamRange(
                    decoded, expected, blockBoundary - 8192, 4096))
        return fail("TCR decoder failed RAII growth or indexed block reuse");

    static const unsigned char truncatedTcr[] = {
        '!', '!', '8', '-', 'B', 'i', 't', '!', '!',
        0,
        3, 'a', 'b', 'c',
        2, 'x'
    };
    LVStreamRef invalid = LVCreateTCRDecoderStream(LVCreateMemoryStream(
            const_cast<unsigned char *>(truncatedTcr),
            static_cast<int>(sizeof(truncatedTcr)), true, LVOM_READ));
    if (!invalid.isNull())
        return fail("TCR decoder accepted a truncated dictionary");
    return 0;
}

static int testStreamBufferOwnership() {
    std::vector<unsigned char> payload(5 * 4096 + 731);
    for (std::size_t i = 0; i < payload.size(); ++i)
        payload[i] = static_cast<unsigned char>((i * 37 + 11) % 251);

    LVStreamRef source = LVCreateMemoryStream(
            payload.data(), static_cast<int>(payload.size()),
            true, LVOM_READ);
    LVStreamRef cached = LVCreateBufferedStream(source, 3 * 4096);
    if (cached.isNull()
            || !verifyStreamRange(cached, payload, 0, 5000)
            || !verifyStreamRange(cached, payload, 2 * 4096 + 17, 9000)
            || !verifyStreamRange(cached, payload, 101, 6000))
        return fail("cached stream failed block reuse across RAII slots");

    const std::string entryName("payload.bin");
    std::vector<unsigned char> zipBytes =
            buildDeflatedZip(entryName, payload, false);
    if (zipBytes.empty())
        return fail("deflated ZIP fixture could not be built");
    LVContainerRef archive = LVOpenArchieve(LVCreateMemoryStream(
            zipBytes.data(), static_cast<int>(zipBytes.size()),
            true, LVOM_READ));
    if (archive.isNull())
        return fail("deflated ZIP fixture did not open");
    LVStreamRef decoded = archive->OpenStream(
            Utf8ToUnicode(lString8(entryName.c_str())).c_str(), LVOM_READ);
    if (decoded.isNull() || !verifyStreamRange(
            decoded, payload, 7000, 321))
        return fail("ZIP decoder failed its multi-buffer fixture");

    const lvpos_t savedPosition = decoded->GetPos();
    lUInt32 checksum = 0;
    const lUInt32 expectedChecksum = lStr_crc32(
            0, payload.data(), static_cast<int>(payload.size()));
    if (decoded->getcrc32(checksum) != LVERR_OK
            || checksum != expectedChecksum
            || decoded->GetPos() != savedPosition)
        return fail("ZIP CRC fallback did not restore stream position");
    if (!verifyStreamRange(decoded, payload, savedPosition, 4096))
        return fail("ZIP decoder could not continue after CRC fallback");
    lvsize_t rejectedRead = 99;
    if (decoded->Read(NULL, LV_INVALID_SIZE, &rejectedRead) != LVERR_FAIL
            || rejectedRead != 0)
        return fail("ZIP decoder accepted an unbounded read request");
    return 0;
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
#ifdef _DEBUG
    if (testCompareStreamScratchOwnership() != 0)
        return 1;
#endif
    if (testRectangleClipValue() != 0)
        return 1;
    if (testT9EncodingCurrentWidth() != 0)
        return 1;
    if (testMutex() != 0)
        return 1;
    if (testConcurrentRenderBaseWeight() != 0)
        return 1;
    if (testConcurrentRenderDPISettings() != 0)
        return 1;
    if (testConcurrentHyphenationSettings() != 0)
        return 1;
    if (testHyphenationRegistryOwnership() != 0)
        return 1;
    if (testHyphenationPatternOwnership() != 0)
        return 1;
    if (testConcurrentHyphenationMethodCache() != 0)
        return 1;
    if (testConcurrentTextLangRuntimeOptions() != 0)
        return 1;
    if (testConcurrentTextLangConfigCache() != 0)
        return 1;
    if (testFontManagerLifecycleOwnership() != 0)
        return 1;
    if (testConcurrentFontGammaSettings() != 0)
        return 1;
    if (testConcurrentFontRenderSettings() != 0)
        return 1;
    if (testBoundedObservableGlyphCache() != 0)
        return 1;
    if (testFontCacheOwnership() != 0)
        return 1;
#if (USE_FREETYPE==1)
    if (testFreeTypeMetricCacheOwnership() != 0)
        return 1;
    if (testFreeTypeColorGlyphScaleOwnership() != 0)
        return 1;
#endif
    if (testBitmapFontFileOwnership() != 0)
        return 1;
    if (testWin32GlyphCacheOwnership() != 0)
        return 1;
    if (testBoundedObservableDecodedImageCache() != 0)
        return 1;
    if (testSkinOwnership() != 0)
        return 1;
    if (testGuiScreenOwnership() != 0)
        return 1;
    if (testGuiRuntimeOwnership() != 0)
        return 1;
#if CR_ENABLE_PAGE_IMAGE_CACHE==1
    if (testBoundedObservablePageImageCache() != 0)
        return 1;
#endif
    if (testLogRedactor() != 0)
        return 1;
    if (testOwnedDescriptor() != 0)
        return 1;
    if (testBorrowedDescriptor() != 0)
        return 1;
    if (testFileStreamOwnership() != 0)
        return 1;
    if (testDirectoryContainerOwnership() != 0)
        return 1;
    if (testIniTranslatorOwnership() != 0)
        return 1;
    if (testTranslatorOwnerLifecycle() != 0)
        return 1;
    if (testParserOwnedBuffers() != 0)
        return 1;
    if (testTextLineQueueOwnership() != 0)
        return 1;
    if (testHistoryOwnership() != 0)
        return 1;
    if (testPropertyStreamOwnership() != 0)
        return 1;
    if (testStringCollectionOwnership() != 0)
        return 1;
    if (testStyleRecordSerializationOwnership() != 0)
        return 1;
    if (testStyleIndexRestoreOwnership() != 0)
        return 1;
    if (testDocumentCacheIndexRestoreOwnership() != 0)
        return 1;
    if (testDocumentHeaderRestoreOwnership() != 0)
        return 1;
    if (testStringBufferOwnership() != 0)
        return 1;
#if (LDOM_USE_OWN_MEM_MAN == 1)
    if (testStringChunkStorageOwnership() != 0)
        return 1;
    if (testDomBlockStorageOwnership() != 0)
        return 1;
#endif
    if (testNameIdMapOwnership() != 0)
        return 1;
    if (testHashTableOwnership() != 0)
        return 1;
    if (testValueArrayOwnership() != 0)
        return 1;
    if (testReferenceAdoptionOwnership() != 0)
        return 1;
    if (testReferenceVectorOwnership() != 0)
        return 1;
    if (testPointerVectorOwnership() != 0)
        return 1;
    if (testMatrixOwnership() != 0)
        return 1;
    if (testPaginationAuxiliaryOwnership() != 0)
        return 1;
    if (testImporterTransientOwnership() != 0)
        return 1;
    if (testReferenceCacheOwnership() != 0)
        return 1;
    if (testSerialBufOwnership() != 0)
        return 1;
    if (testEmbeddedFontOwnership() != 0)
        return 1;
    if (testCacheFileCodecOwnership() != 0)
        return 1;
    if (testCacheFileIndexOwnership() != 0)
        return 1;
    if (testDomBlobOwnership() != 0)
        return 1;
    if (testDomChunkStorageOwnership() != 0)
        return 1;
    if (testDomNodePartOwnership() != 0)
        return 1;
    if (testDomMutableNodeOwnership() != 0)
        return 1;
    if (testXPointerStateOwnership() != 0)
        return 1;
    if (testDoubleCharStatOwnership() != 0)
        return 1;
    if (testEncodingStatsFileOwnership() != 0)
        return 1;
    if (testWolBufferOwnership() != 0)
        return 1;
    if (testFormatterWorkspaceOwnership() != 0)
        return 1;
    if (testRtfTextBufferOwnership() != 0)
        return 1;
    if (testParserFormatDetectionBuffers() != 0)
        return 1;
#if (USE_LIBJPEG==1)
    if (testJpegDecoderOwnership() != 0)
        return 1;
#endif
#if (USE_LIBPNG==1)
    if (testPngDecoderOwnership() != 0)
        return 1;
#endif
#if (USE_GIF==1)
    if (testGifLzwBoundedReads() != 0)
        return 1;
    if (testGifDecoderOwnership() != 0)
        return 1;
#endif
#if (USE_NANOSVG==1)
    if (testSvgDecoderOwnership() != 0)
        return 1;
#endif
    if (testDrawBufferStorageOwnership() != 0)
        return 1;
    if (testImageSourceOwnership() != 0)
        return 1;
    if (testMemoryStreamOwnership() != 0)
        return 1;
    if (testBlockWriteStreamOwnership() != 0)
        return 1;
#if defined(_LINUX) || defined(_WIN32)
    if (testFileMappedStreamOwnership() != 0)
        return 1;
#endif
    if (testDefaultStreamBufferOwnership() != 0)
        return 1;
    if (testTcrStreamOwnership() != 0)
        return 1;
    if (testArchiveContainerOwnership() != 0)
        return 1;
    if (testOdtOwnership() != 0)
        return 1;
    if (testOpcFb3Ownership() != 0)
        return 1;
    if (testChmOwnership() != 0)
        return 1;
    if (testStreamBufferOwnership() != 0)
        return 1;
    if (testZipArchiveBudgets() != 0)
        return 1;
    if (testParseBudgetCodes() != 0)
        return 1;
    if (testXmlParseBudgetIntegration() != 0)
        return 1;
    return testRecursiveContainerBudget();
}
