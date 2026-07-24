#include "crsetup.h"
#include "lvstring.h"

#include <graphemebreak.h>
#include <linebreak.h>

#if USE_FRIBIDI == 1
#if BUNDLED_FRIBIDI == 1
#include "fribidi.h"
#else
#include <fribidi/fribidi.h>
#endif
#endif

#include <cstdio>

static int fail(const char *message)
{
    std::fprintf(stderr, "%s\n", message);
    return 1;
}

static int testUtf16Surrogates()
{
    const lChar32 scalars[] = {
        U'A', 0x1F600, 0x10437, U'Z'
    };
    lString16 utf16 = UnicodeToUtf16(scalars, 4);
    const lChar16 expectedUtf16[] = {
        0x0041, 0xD83D, 0xDE00, 0xD801, 0xDC37, 0x005A
    };
    if (utf16.length() != 6)
        return fail("UTF-16 supplementary scalar length is incorrect");
    for (int index = 0; index < utf16.length(); index++) {
        if (utf16[index] != expectedUtf16[index])
            return fail("UTF-16 supplementary scalar encoding is incorrect");
    }
    if (Utf16ToUnicode(utf16) != lString32(scalars, 4))
        return fail("UTF-16 supplementary scalar round trip failed");

    const lChar16 explicitInput[] = {0xD83D, 0xDE00, U'A'};
    lChar32 explicitOutput[3] = {};
    int sourceLength = 3;
    int destinationLength = 3;
    Utf16ToUnicode(explicitInput, sourceLength,
            explicitOutput, destinationLength);
    if (sourceLength != 3 || destinationLength != 2
            || explicitOutput[0] != 0x1F600
            || explicitOutput[1] != U'A')
        return fail("bounded UTF-16 surrogate decoding failed");

    const lChar16 malformedHigh[] = {0xD83D, U'A', 0};
    lString32 decodedHigh = Utf16ToUnicode(malformedHigh);
    if (decodedHigh.length() != 2
            || decodedHigh[0] != U'?'
            || decodedHigh[1] != U'A')
        return fail("unpaired high surrogate consumed the following scalar");

    const lChar16 malformedLow[] = {0xDE00, U'B'};
    lString32 decodedLow = Utf16ToUnicode(malformedLow, 2);
    if (decodedLow.length() != 2
            || decodedLow[0] != U'?'
            || decodedLow[1] != U'B')
        return fail("unpaired low surrogate was not replaced");

    const lChar16 trailingHigh[] = {0xD83D};
    lString32 decodedTrailing = Utf16ToUnicode(trailingHigh, 1);
    if (decodedTrailing.length() != 1 || decodedTrailing[0] != U'?')
        return fail("trailing high surrogate was not bounded and replaced");

    const lChar32 invalidScalars[] = {0xD800, 0x110000};
    lString8 invalidUtf8 = UnicodeToUtf8(invalidScalars, 2);
    lString16 invalidUtf16 = UnicodeToUtf16(invalidScalars, 2);
    if (invalidUtf8.length() != 2
            || invalidUtf8[0] != '?' || invalidUtf8[1] != '?')
        return fail("invalid Unicode scalars escaped UTF-8 validation");
    if (invalidUtf16.length() != 2
            || invalidUtf16[0] != U'?' || invalidUtf16[1] != U'?')
        return fail("invalid Unicode scalars escaped UTF-16 validation");
    return 0;
}

static int testGraphemeBoundaries()
{
    const utf32_t combining[] = {U'e', 0x0301, U'x'};
    char combiningBreaks[3] = {};
    set_graphemebreaks_utf32(
            combining, 3, "en", combiningBreaks);
    if (combiningBreaks[0] != GRAPHEMEBREAK_NOBREAK
            || combiningBreaks[1] != GRAPHEMEBREAK_BREAK
            || combiningBreaks[2] != GRAPHEMEBREAK_BREAK)
        return fail("combining-mark grapheme boundaries are incorrect");

    const utf32_t emojiZwj[] = {
        0x1F469, 0x200D, 0x1F4BB, U'x'
    };
    char emojiBreaks[4] = {};
    set_graphemebreaks_utf32(emojiZwj, 4, nullptr, emojiBreaks);
    if (emojiBreaks[0] != GRAPHEMEBREAK_NOBREAK
            || emojiBreaks[1] != GRAPHEMEBREAK_NOBREAK
            || emojiBreaks[2] != GRAPHEMEBREAK_BREAK
            || emojiBreaks[3] != GRAPHEMEBREAK_BREAK)
        return fail("emoji ZWJ grapheme boundaries are incorrect");
    return 0;
}

static int testCjkLineBreaking()
{
    if (is_line_breakable(0x4E00, 0x4E8C, "zh")
            != LINEBREAK_ALLOWBREAK)
        return fail("CJK ideographs did not expose a line-break opportunity");
    if (is_line_breakable(0x4E00, 0x3001, "zh")
            != LINEBREAK_NOBREAK)
        return fail("CJK punctuation allowed a prohibited leading break");
    return 0;
}

#if USE_FRIBIDI == 1
static int testBidirectionalText()
{
    const FriBidiChar logical[] = {
        U'A', U' ', 0x05D0, 0x05D1
    };
    const FriBidiChar expectedVisual[] = {
        U'A', U' ', 0x05D1, 0x05D0
    };
    FriBidiChar visual[4] = {};
    FriBidiLevel levels[4] = {};
    FriBidiParType direction = FRIBIDI_PAR_LTR;
    FriBidiLevel maximumLevel = fribidi_log2vis(
            logical, 4, &direction, visual, nullptr, nullptr, levels);
    if (maximumLevel < 2 || direction != FRIBIDI_PAR_LTR)
        return fail("mixed-direction paragraph levels are incorrect");
    for (int index = 0; index < 4; index++) {
        if (visual[index] != expectedVisual[index])
            return fail("mixed-direction visual ordering is incorrect");
    }
    return 0;
}
#endif

static int testMixedScriptRoundTrips()
{
    const lChar32 mixed[] = {
        U'A', 0x0301, 0x05D0, 0x0628, 0x4E2D, 0x1F642
    };
    const lString32 expected(mixed, 6);
    if (Utf8ToUnicode(UnicodeToUtf8(expected)) != expected)
        return fail("mixed-script UTF-8 round trip failed");
    if (Utf16ToUnicode(UnicodeToUtf16(expected)) != expected)
        return fail("mixed-script UTF-16 round trip failed");
    return 0;
}

static int testAsciiInterop()
{
    const lString32 text(U"prefix-value");
    if (text.pos("value") != 7
            || text.pos("fix", 1) != 3
            || !text.startsWith("prefix"))
        return fail("ASCII and Unicode string matching diverged");
    if (!lString16(u"prefix-value").startsWith("prefix"))
        return fail("ASCII and UTF-16 prefix matching diverged");

    double value = 0;
    if (!lString32(U"12,5").atod(value, ',')
            || value < 12.49 || value > 12.51)
        return fail("Unicode decimal parsing lost its ASCII separator");
    return 0;
}

int main()
{
    if (testUtf16Surrogates() != 0
            || testGraphemeBoundaries() != 0
            || testCjkLineBreaking() != 0)
        return 1;
#if USE_FRIBIDI == 1
    if (testBidirectionalText() != 0)
        return 1;
#endif
    if (testMixedScriptRoundTrips() != 0)
        return 1;
    return testAsciiInterop();
}
