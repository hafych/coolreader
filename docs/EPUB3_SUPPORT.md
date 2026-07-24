# EPUB 3 reading policy

CoolReader's EPUB 3 support is intentionally centered on static reading.

- A manifest item with the `nav` property supplies the primary nested table of
  contents.
- A `landmarks` navigation list is used as the reading-order fallback when a
  publication has no `toc` list.
- A `page-list` is exposed through the document page map independently of the
  selected TOC source.
- Dublin Core title, creators, language, description and subjects are retained.
  EPUB 3 `belongs-to-collection` and `group-position` refinements populate the
  series name and number.
- `epub:type="noteref"` and the equivalent `doc-noteref` ARIA role identify
  note links. Footnote asides are eligible for in-page rendering and note
  popups; endnote asides remain in normal flow and are popup targets.
- Media-overlay declarations and SMIL/audio resources are not executed. The
  XHTML spine remains the authoritative, accessible static text, and overlay
  resources are not merged into the document DOM.

The native `epub3_regression` corpus is built in memory as deterministic,
uncompressed EPUB ZIPs. One fixture covers nested `nav`, page-list, metadata,
semantic notes and a media overlay; a second omits `toc` to lock the landmarks
fallback.
