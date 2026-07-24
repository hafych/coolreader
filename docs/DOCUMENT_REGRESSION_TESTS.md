# Document behavior regression tests

Document behavior tests use deterministic synthetic fixtures kept in source.
They do not depend on private books, network services or platform UI.

The native `document_regression` test currently fixes these invariants:

- forward, reverse and case-sensitive search result order;
- a missing query returning no result;
- serialized reading positions round-tripping through XPointer;
- selected range text boundaries;
- identical parse runs producing identical positions and selection text.

Pagination, page-level bookmark restoration and rendered selection geometry
remain follow-up coverage because they require a deterministic font and render
fixture. Those checks must compare both the page sequence and restored logical
position, rather than only a page count.
