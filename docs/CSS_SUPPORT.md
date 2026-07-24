# Supported CSS subset

CoolReader implements a reading-oriented CSS subset. The contract below
describes behavior that the engine parses and applies; unsupported declarations
are ignored instead of being approximated as browser CSS.

## Selectors and cascade

Supported selectors include:

- universal, type, class and ID selectors, including comma-separated groups;
- descendant, child, adjacent-sibling and general-sibling combinators;
- attribute presence and `=`, `~=`, `|=`, `^=`, `$=` and `*=` comparisons,
  with the optional ASCII case-insensitive `i` flag;
- `:root`, `:dir()`, child/type position pseudo-classes and `:empty`;
- `::before` and `::after`.

The cascade honors selector specificity, source order and `!important`.
Inline and document styles are combined with the engine's reader stylesheet;
reader-important declarations retain the higher origin.

## Properties

The stable reading subset covers:

- layout: `display`, `visibility`, `float`, `clear`, `width`, `height`,
  min/max sizes, margins, padding, borders and table border spacing/collapse;
- text: color/background, alignment, decoration, transform, indentation,
  white-space, line height, letter spacing, direction, line/word breaking,
  widows and orphans;
- fonts: family, size, style, weight, OpenType feature settings and supported
  `font-variant-*` forms;
- lists: type, position and image;
- pagination: legacy `page-break-before`, `page-break-after` and
  `page-break-inside`, plus their `break-*` aliases;
- generated content for the supported `::before` and `::after` forms;
- EPUB hyphenation property aliases.

EPUB loading additionally discovers packaged `@font-face` declarations and
leading `@import` rules. General browser features such as scripting, CSS Grid,
transitions, animations and arbitrary media queries are outside this contract.

## Values and units

Lengths support `px`, `in`, `cm`, `mm`, `pt`, `pc`, `em`, `ex`, `ch`, `rem`,
`vw`, `vh`, `vmin`, `vmax` and percentages where the property accepts them.
Relative font sizes are resolved against the computed parent size. Colors
support named colors and `#rgb`/`#rrggbb`; functional `rgb()`/`rgba()` syntax is
not part of the subset.

Pagination accepts `auto`, `avoid`, `always`, `left`, `right`, `page`, `recto`
and `verso` where applicable. `break-inside` is limited to `auto`, `avoid` and
`inherit`.

The native `css_regression` test locks the declaration-unit matrix, modern and
legacy page-break aliases, source-order cascade, selector specificity and
computed inheritance through the production HTML rendering path.
