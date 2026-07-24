#include "hyphman.h"
#include "lvfnt.h"
#include "lvstring.h"

#include <cstdio>
#include <vector>

static lString32 hyphenateWord(
        HyphMethod *method, const lString32 &word)
{
    std::vector<lUInt16> widths(word.length());
    std::vector<lUInt8> flags(word.length(), 0);
    for (int index = 0; index < word.length(); index++)
        widths[index] = static_cast<lUInt16>((index + 1) * 10);
    method->hyphenate(
            word.c_str(),
            word.length(),
            widths.data(),
            flags.data(),
            1,
            0xFFFF);

    lString32 result;
    for (int index = 0; index < word.length(); index++) {
        result.append(1, word[index]);
        if ((flags[index] & LCHAR_ALLOW_HYPH_WRAP_AFTER) != 0)
            result.append(1, U'-');
    }
    return result;
}

struct GoldenCase {
    const lChar32 *language;
    const lChar32 *word;
    const lChar32 *expected;
};

static int validatePackagedDictionaries()
{
    HyphDictionaryList *dictionaries = HyphMan::getDictList();
    if (!dictionaries || dictionaries->length() != 30) {
        std::fprintf(stderr, "expected 27 packaged and 3 built-in dictionaries\n");
        return 1;
    }
    int packagedCount = 0;
    for (int index = 0; index < dictionaries->length(); index++) {
        HyphDictionary *dictionary = dictionaries->get(index);
        if (!dictionary || dictionary->getType() != HDT_DICT_TEX)
            continue;
        packagedCount++;
        HyphMethod *method = HyphMan::getHyphMethodForDictionary(
                dictionary->getId());
        if (!method || method->getId() != dictionary->getId()) {
            std::fprintf(
                    stderr,
                    "packaged dictionary failed to load: %s\n",
                    UnicodeToUtf8(dictionary->getId()).c_str());
            return 1;
        }
    }
    if (packagedCount != 27) {
        std::fprintf(stderr, "packaged dictionary count changed\n");
        return 1;
    }
    return 0;
}

int main()
{
    const lString32 dictionaryDirectory(
            COOLREADER_SOURCE_DIR "/cr3gui/data/hyph/");
    if (!HyphMan::initDictionaries(dictionaryDirectory, true)) {
        std::fprintf(stderr, "hyphenation dictionary directory did not load\n");
        return 1;
    }
    if (validatePackagedDictionaries() != 0) {
        HyphMan::uninit();
        return 1;
    }

    static const GoldenCase cases[] = {
        {U"en-US", U"representation", U"representa-tion"},
        {U"de-1996", U"Silbentrennung", U"Sil-ben-tren-nung"},
        {U"pt", U"extraordinário", U"ex-tra-or-di-ná-rio"},
        {U"uk", U"український", U"укра-їн-ський"},
        {U"ru-RU", U"библиотека", U"биб-лио-те-ка"},
        {U"pl", U"czytelnictwo", U"czy-tel-nic-two"},
        {U"fi", U"kirjasto", U"kir-jas-to"},
    };

    int failures = 0;
    for (const GoldenCase &testCase : cases) {
        HyphMethod *method = HyphMan::getHyphMethodForLang(
                lString32(testCase.language));
        const lString32 actual = hyphenateWord(
                method, lString32(testCase.word));
        if (actual != testCase.expected) {
            std::fprintf(
                    stderr,
                    "%s: expected '%s', got '%s'\n",
                    UnicodeToUtf8(lString32(testCase.language)).c_str(),
                    UnicodeToUtf8(lString32(testCase.expected)).c_str(),
                    UnicodeToUtf8(actual).c_str());
            failures++;
        }
    }

    HyphMan::uninit();
    return failures == 0 ? 0 : 1;
}
