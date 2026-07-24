#include "fb2def.h"
#include "lvtinydom.h"
#include "lvstreamutils.h"

#include <cstdio>
#include <memory>
#include <string>

#define XS_IMPLEMENT_SCHEME 1
#include "fb2def.h"

static int fail(const char *message) {
    std::fprintf(stderr, "%s\n", message);
    return 1;
}

static std::unique_ptr<ldomDocument> parseFixture() {
    static const char fixture[] =
            "<?xml version=\"1.0\" encoding=\"UTF-8\"?>"
            "<FictionBook>"
            "<body><section><title><p>Regression chapter</p></title>"
            "<p>Alpha Needle middle needle omega.</p>"
            "</section></body>"
            "</FictionBook>";
    LVStreamRef stream = LVCreateMemoryStream(
            const_cast<char *>(fixture),
            static_cast<int>(sizeof(fixture) - 1),
            true,
            LVOM_READ);
    return std::unique_ptr<ldomDocument>(LVParseXMLStream(
            stream, fb2_elem_table, fb2_attr_table, fb2_ns_table));
}

struct DocumentSnapshot {
    lString32 firstPosition;
    lString32 secondPosition;
    lString32 selectionText;
};

static int snapshotDocument(
        ldomDocument *document, DocumentSnapshot &snapshot) {
    if (document == NULL)
        return fail("document fixture did not parse");

    ldomXPointer textPointer = document->createXPointer(
            U"/FictionBook/body[1]/section[1]/p[1]/text()[1]");
    if (textPointer.isNull() || !textPointer.isText())
        return fail("fixture text XPointer did not resolve");

    lString32 text = textPointer.getNode()->getText();
    ldomXRange textRange(
            ldomXPointer(textPointer.getNode(), 0),
            ldomXPointer(textPointer.getNode(), text.length()));

    LVArray<ldomWord> forward;
    if (!textRange.findText(
                U"needle", true, false, forward, 10, 0)
            || forward.length() != 2) {
        return fail("case-insensitive forward search changed");
    }
    if (forward[0].getText() != U"Needle"
            || forward[1].getText() != U"needle") {
        return fail("forward search result order changed");
    }

    ldomXRange reverseRange(
            ldomXPointer(textPointer.getNode(), 0),
            ldomXPointer(textPointer.getNode(), text.length()));
    LVArray<ldomWord> reverse;
    if (!reverseRange.findText(
                U"needle", true, true, reverse, 10, 0)
            || reverse.length() != 2
            || reverse[0].getText() != U"needle"
            || reverse[1].getText() != U"Needle") {
        return fail("reverse search result order changed");
    }

    ldomXRange caseSensitiveRange(
            ldomXPointer(textPointer.getNode(), 0),
            ldomXPointer(textPointer.getNode(), text.length()));
    LVArray<ldomWord> caseSensitive;
    if (!caseSensitiveRange.findText(
                U"needle", false, false, caseSensitive, 10, 0)
            || caseSensitive.length() != 1
            || caseSensitive[0].getText() != U"needle") {
        return fail("case-sensitive search changed");
    }

    ldomXRange missingRange(
            ldomXPointer(textPointer.getNode(), 0),
            ldomXPointer(textPointer.getNode(), text.length()));
    LVArray<ldomWord> missing;
    if (missingRange.findText(
                U"absent", true, false, missing, 10, 0)
            || !missing.empty()) {
        return fail("missing search term produced a result");
    }

    snapshot.firstPosition =
            forward[0].getStartXPointer().toString();
    snapshot.secondPosition =
            forward[1].getStartXPointer().toString();
    ldomXPointer restored =
            document->createXPointer(snapshot.firstPosition);
    if (restored.isNull()
            || restored != forward[0].getStartXPointer()) {
        return fail("serialized reading position did not round-trip");
    }

    ldomXRange selection(
            forward[0].getStartXPointer(),
            forward[1].getEndXPointer());
    snapshot.selectionText = selection.getRangeText(' ', 1000);
    if (snapshot.selectionText != U"Needle middle needle")
        return fail("selection range text changed");
    return 0;
}

int main() {
    std::unique_ptr<ldomDocument> first = parseFixture();
    DocumentSnapshot firstSnapshot;
    if (snapshotDocument(first.get(), firstSnapshot) != 0)
        return 1;

    std::unique_ptr<ldomDocument> second = parseFixture();
    DocumentSnapshot secondSnapshot;
    if (snapshotDocument(second.get(), secondSnapshot) != 0)
        return 1;

    if (firstSnapshot.firstPosition != secondSnapshot.firstPosition
            || firstSnapshot.secondPosition
                    != secondSnapshot.secondPosition
            || firstSnapshot.selectionText
                    != secondSnapshot.selectionText) {
        return fail("document results changed between equivalent parses");
    }
    return 0;
}
