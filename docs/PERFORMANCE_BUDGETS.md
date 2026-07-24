# Performance budgets

`performance-budgets.json` is the release contract for startup, large-book
opening, page turns, search, scanning and memory. Targets are the desired
ceiling; hard limits block a public release. A target miss below the hard limit
requires investigation or a written owner waiver in the release evidence.
These are initial engineering thresholds, not previously measured claims; the
first signed release candidate must establish and retain the raw baseline.

## Reference environment

The comparable Android baseline is a Pixel 5 or equivalent running API 35 and
the release build, with animations disabled, nominal thermal state, at least
50% battery and 5 GiB free internal storage. Record the exact model, OS build,
CPU governor/thermal state, app version, commit, artifact checksum and signing
certificate with results.

Use one warm-up followed by at least five measured runs. Report the configured
statistic, every raw value and peak PSS. Process-cold startup means force-stop
before each run; it does not claim a filesystem-cold boot. Do not compare debug
or emulator numbers with the release-device baseline.

E-Ink page-turn results require approved physical E-Ink hardware. Record its
waveform/refresh mode and firmware; emulator or LCD results cannot waive that
scenario.

## Deterministic fixtures

Generate synthetic fixtures outside the repository:

```bash
python3 tools/generate_performance_fixtures.py \
  --output android/app/build/performance-fixtures
```

This creates a 50 MiB valid FB2 with a known search needle and a 10,000-book
synthetic library. It contains no copyrighted or private book text. For the
scan scenario, expose the generated library through a test document provider or
copy it to a dedicated test location and select it with SAF.

## Measurement definitions

- Cold start: force-stop to interactive library, measured with Android startup
  timing or a trace whose end marker is the first enabled library interaction.
- Large-book open: selection/launch to first interactive rendered page.
- Page turn: input event to presented frame, 100 forward turns after warm-up.
- Search: request from the beginning to the known needle near the file end.
- Scan: new private database to completed metadata persistence for all 10,000
  SAF documents.
- Memory: maximum PSS sampled during open, search and page-turn sequence; save
  `dumpsys meminfo` or Perfetto evidence.

The same source fixture hash must be used for baseline and candidate. A changed
scenario, device or OS establishes a new named baseline; it must not silently
replace historical measurements.

## Automated scanner corpus

API 35 CI also runs a deterministic 20,000-book FB2 scanner corpus:

```bash
./tools/run_android_scanner_corpus.sh
```

The instrumentation report records `initial_scan_elapsed_ms`,
`unchanged_scan_elapsed_ms`, `peak_pss_kib` and `peak_java_heap_bytes` under
`android/app/build/outputs/androidTest-results/`. Its debug-emulator numbers are
regression evidence and enforce broad safety ceilings; they are not comparable
to, and never replace, the signed release-device baseline defined above.

## Results format and gate

Store release evidence as JSON:

```json
{
  "environmentId": "android-pixel5-api35-release",
  "commit": "<full commit>",
  "runs": {
    "cold_start": [1200, 1250, 1300, 1280, 1260],
    "open_large_book": [4100, 4200, 4300, 4250, 4150],
    "page_turn_lcd": [31, 34, 33, 35, 32],
    "page_turn_eink": [180, 190, 200, 195, 185],
    "search_large_book": [1500, 1520, 1490, 1510, 1530],
    "scan_10000_books": [120000, 125000, 123000, 121000, 124000],
    "peak_pss_large_book": [300, 305, 302, 308, 304]
  }
}
```

Validate hard limits:

```bash
python3 tools/verify_performance_budgets.py --results results.json
```

Use `--strict-targets` for a release candidate. Compare the configured
statistic with the last accepted release as well; a regression over 15% requires
investigation even when it remains below the absolute target.
