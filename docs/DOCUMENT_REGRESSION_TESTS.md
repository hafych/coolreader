# Document behavior regression tests

Document behavior tests use deterministic synthetic fixtures kept in source.
They do not depend on private books, network services or platform UI.

The native `document_regression` test currently fixes these invariants:

- forward, reverse and case-sensitive search result order;
- a missing query returning no result;
- serialized reading positions round-tripping through XPointer;
- selected range text boundaries;
- identical parse runs producing identical positions and selection text;
- strictly ordered pagination for a fixed 320x240 single-page viewport;
- every rendered page bookmark mapping back to the same page;
- exact logical bookmark and page restoration through the production
  `savePosition`/`restorePosition` history path;
- multi-line selection rectangles with positive, ordered geometry;
- identical page starts, heights, bookmarks, restored position and selection
  geometry across two equivalent renders.

Rendered checks register the vendored HarfBuzz Roboto fixture explicitly and
select it by family name. This keeps pagination independent of the host's
default font while reusing an asset already present in the source tree.
