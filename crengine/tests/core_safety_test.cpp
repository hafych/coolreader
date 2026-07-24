#include "lvstreamutils.h"
#include "lvthread.h"
#include "hyphman.h"
#include "lvfntman.h"
#include "lvgraydrawbuf.h"
#include "lvrend.h"
#include "lvxmlparser.h"
#include "lvxmlparsercallback.h"
#include "cri18n.h"
#include "logredactor.h"
#include "parsebudget.h"
#include "textlang.h"
#include "../src/lvfont/lvfontglyphcache.h"

#include <atomic>
#include <cerrno>
#include <cstdlib>
#include <cstdio>
#include <cstring>
#include <cstdint>
#include <fcntl.h>
#include <string>
#include <thread>
#include <unistd.h>
#include <vector>

static int fail(const char *message) {
    std::fprintf(stderr, "%s\n", message);
    return 1;
}

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

static LVFontGlyphCacheItem *newGlyphCacheTestItem(
        LVFontLocalGlyphCache *localCache, lUInt32 key) {
    static const int width = 4;
    static const int height = 4;
    static const int pitch = 4;
    return LVFontGlyphCacheItem::newItem(
            localCache, key, width, height, pitch, pitch * height);
}

static int testBoundedObservableGlyphCache() {
    LVFontGlyphCacheItem *sizeProbe = newGlyphCacheTestItem(NULL, 0);
    if (!sizeProbe)
        return fail("glyph cache size probe allocation failed");
    const int itemSize = sizeProbe->getSize();
    LVFontGlyphCacheItem::freeItem(sizeProbe);

    LVFontGlobalGlyphCache globalCache(itemSize * 2);
    LVFontLocalGlyphCache localCache(&globalCache);
    if (localCache.get(1) != NULL)
        return fail("empty glyph cache unexpectedly returned an item");

    LVFontGlyphCacheItem *first = newGlyphCacheTestItem(&localCache, 1);
    LVFontGlyphCacheItem *second = newGlyphCacheTestItem(&localCache, 2);
    LVFontGlyphCacheItem *third = newGlyphCacheTestItem(&localCache, 3);
    if (!first || !second || !third) {
        LVFontGlyphCacheItem::freeItem(first);
        LVFontGlyphCacheItem::freeItem(second);
        LVFontGlyphCacheItem::freeItem(third);
        return fail("glyph cache fixture allocation failed");
    }
    localCache.put(first);
    localCache.put(second);
    if (localCache.get(1) != first)
        return fail("glyph cache did not return the first item");
    localCache.put(third);

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
    if (testConcurrentRenderBaseWeight() != 0)
        return 1;
    if (testConcurrentRenderDPISettings() != 0)
        return 1;
    if (testConcurrentHyphenationSettings() != 0)
        return 1;
    if (testHyphenationRegistryOwnership() != 0)
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
    if (testIniTranslatorOwnership() != 0)
        return 1;
    if (testZipArchiveBudgets() != 0)
        return 1;
    if (testParseBudgetCodes() != 0)
        return 1;
    if (testXmlParseBudgetIntegration() != 0)
        return 1;
    return testRecursiveContainerBudget();
}
