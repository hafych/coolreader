# Hyphenation dictionaries

CoolReader ships the same 27 pattern files in:

- `cr3gui/data/hyph` for desktop builds;
- `android/res/raw` for Android, with `-` and `,` replaced by `_` in Android
  resource names.

`android/update-hyphenation.sh --check` and the native
`hyphenation_resource_sync` test require both sets to be byte-identical.

## Pinned sources

The TeX-derived files are generated with
`crengine/Tools/HyphConv/mkpattern.pl` from the primary
[`hyphenation/tex-hyphen`](https://github.com/hyphenation/tex-hyphen)
repository, pinned for this update to commit
`5684c0f51c0b81133db2efbe60a408b4155a3ff5` (2026-02-24).

At that pin, payload comparison changed three dictionaries:

- `hyph-de-1996.pattern`: upstream version 2024-02-28;
- `hyph-pt.pattern`: upstream version 1.4, 2024-07-13;
- `hyph-uk.pattern`: current generated patterns and exception list.

The other TeX-derived payloads already matched the pin. Russian
`hyph-ru-ru.pattern` is generated from
[`laboratory50/russian-spellpack`](https://github.com/laboratory50/russian-spellpack)
commit `7ff7934531a0711b5c86802ab635fdbdc974c1cc`; its payload already
matched. `hyph-ru-ru,en-us.pattern` combines that Russian set with the
pinned American English patterns.

## License audit

Every distributed `.pattern` contains its source copyright and license notice.
The notice inside each file is authoritative. The choices below are the
redistribution terms used by this GPL-2-or-later project when a source offers
multiple alternatives.

| Files | Redistribution terms |
| --- | --- |
| `ar`, `fa`, `de-1996`, `en-gb`, `es`, `fr`, `nl`, `zh-latn-pinyin` | MIT |
| `bn`, `da`, `el-monoton`, `gu`, `it`, `mr`, `pa`, `pl`, `ta`, `te`, `uk` | MIT option |
| `pt` | BSD-3-Clause |
| `bg` | permissive BSD-style notice in the file |
| `en-us` | permissive no-royalty notice with notice preservation |
| `fi` | freely distributable notice in the file |
| `cs` | GPL-2.0-or-later |
| `hu` | GPL-2.0 option |
| `ru-ru` | LGPL-licensed Russian source |
| `ru-ru,en-us` | Russian LGPL notice plus the American English permissive notice |

The American English and British English files preserve their upstream
notices. The British file contains generated patterns, not the
non-redistributable OUP training word list mentioned in its provenance notes.

## Golden behavior

The native `hyphenation_regression` test fully parses all 27 shipped
dictionaries through `HyphMan`, including the intentionally empty Arabic and
Persian no-hyphenation sets. It also locks representative English, German,
Portuguese, Ukrainian, Russian, Polish and Finnish break positions. This
catches conversion, language-tag, minimum-fragment and loading regressions
rather than only checking file presence.
