# Changelog

This file records user-visible changes in the downstream CoolReader Next fork.
The historical Debian changelog remains in [`changelog`](changelog).

## Unreleased

### Added

- Linux, macOS and Android build verification, native smoke tests and
  ASan/UBSan CI.
- A full Qt 6 Linux desktop build job with shared native smoke tests; Qt 5 is
  now a best-effort compatibility target through December 31, 2026.
- A separate Clang warning gate for high-confidence C/C++ diagnostics; its
  first run also fixed an uninitialized MathML style flag.
- CI runner labels pinned to Ubuntu 24.04 and macOS 15 instead of moving
  `*-latest` aliases.
- Guarded external-keystore release signing, a synthetic signed-AAB CI smoke
  test and native debug-symbol generation; unsigned bundles are not uploaded as
  distributable artifacts.
- A CI-enforced dependency and license allowlist plus an SPDX 2.3 SBOM for the
  resolved Android release runtime and native components.
- Explicit Android release performance budgets and deterministic synthetic
  large-book/10,000-book-library fixture generation.
- A single verified fork version source shared by Android and desktop, with a
  monotonic date/sequence Android version-code scheme.
- A tag-verified release workflow producing signed Android artifacts, native
  symbols, SPDX, deterministic desktop archives, checksums and a draft GitHub
  Release, plus rollback/hotfix gates.
- A two-clean-runner release reproducibility gate with normalized archive,
  native-symbol and SPDX timestamps.
- A trusted-base pull request governance check enforcing fork classification
  and consistent `FORK_DELTA.md` declarations.
- Two verified, brand-independent upstream patch candidates for C++17 CMake
  selection and reliable checksum-pinned third-party downloads.
- Explicit glyph-format handling and named packed ZIP headers, with the
  corresponding Clang diagnostics promoted to first-party build errors.
- Deterministic member/aggregate initialization and unambiguous parser/skin
  control flow, with misleading indentation promoted to a blocking first-party
  Clang diagnostic.
- A machine-enforced no-telemetry decision: local crash redaction only, with
  analytics/crash SDKs blocked until a separate consent and privacy decision.
- Lifecycle-aware Activity Result launchers for SAF and dictionary flows, with
  pending document-tree state restored after Activity recreation.
- Nook E-Ink controller setup now resolves the current view host on demand
  instead of retaining the Activity in a static field.
- The native Engine retains only application context plus a detachable weak UI
  host, and stale Activity shutdown can no longer tear down a newer generation.
- ReaderView receives scanner, history, document cache and a per-generation
  lifecycle token explicitly instead of consulting the static service locator.
- CoolReader captures one explicit service generation during creation and no
  longer performs ad-hoc global dependency lookups during user flows.
- BaseActivity and CoolReader now receive that generation as one immutable
  dependency snapshot instead of reading individual global service slots.
- Cover generation receives its Engine dependency explicitly and no longer
  resolves it through the global service locator on background tasks; queued
  work is discarded when its service generation closes.
- Favorite-folder assembly and recursive folder deletion now use the Scanner
  captured by their caller instead of resolving global scanner state.
- About and options dialogs receive the active Engine explicitly, including
  browser and TTS entry points.
- Book information, online-store and TTS cover UI receive cover, genre, history
  and scanner dependencies from their owning reader or browser generation.
- The optional cloud synchronizer receives its Scanner explicitly for restored
  downloads and cloud-sync storage.
- OPDS downloads receive Engine and lifecycle dependencies from FileBrowser;
  stale-generation work stops updating progress or delivering callbacks, and
  the active task is no longer retained in static Activity-owned state.
- MainDB receives its genre handbook from CRDBService instead of depending on
  the Activity-owned global service graph.
- Each BaseActivity now owns its service graph instance and immutable dependency
  snapshot; teardown closes only that Activity generation and cannot clear a
  newer graph through a static registry.
- Custom E-Ink toast queues, handlers and popup windows are now owned and
  cancelled by their Activity instead of retaining Views in static fields.
- Scanner resources, private-directory access and cached online-store plugins
  now retain application context rather than the creating Activity.
- External local-document validation is a standalone tested component, and the
  document cache accepts a generic Context instead of an Activity.
- Archive metadata lookup is a stateless Engine operation, so FileInfo no
  longer reaches through the global service locator.
- Library metadata scanning uses one in-flight 64-item batch, persists
  completed batches incrementally, honours cancellation between items and owns
  UI progress outside the database layer.
- Library discovery now runs off the UI thread with iterative post-order
  traversal, per-entry cancellation checks and coalesced initial/final UI
  updates instead of recursive calls and an unbounded stream of UI tasks.
- Recursive library scans reuse the discovered tree and feed directories
  through one bounded FIFO metadata pipeline instead of rediscovering every
  descendant once per ancestor.
- Full-tree discovery has explicit 100,000-entry and 256-level budgets, reports
  stable stop reasons, reserves a progress phase and shows an actionable
  message instead of silently truncating oversized libraries.
- Library discovery, metadata extraction, persistence and UI progress now meet
  through explicit scan-state, extractor, metadata-store and progress ports,
  keeping the database and Engine adapters at the pipeline composition
  boundary.
- Versioned document and directory source fingerprints are persisted by main
  database schema v38, allowing unchanged directory batches to reuse stored
  metadata while same-size files with a newer modification time are rescanned.
- API 35 CI now scans and rescans a deterministic 20,000-book FB2 corpus,
  enforcing debug safety ceilings and preserving initial/unchanged elapsed
  time, peak PSS and peak Java heap as instrumentation evidence.
- FileBrowser and the home/root view now receive scanner, history, cover and
  favorite-folder services explicitly.
- Database, synchronization and TTS service connectors bind through application
  context instead of retaining their Activity host.
- Android 14–16 foreground TTS, notification and receiver invariants are now
  enforced in CI, with a repeatable API 34–36 runtime matrix.
- Stable, checksum-equivalent zlib release retrieval from the official GitHub
  release asset.
- Storage Access Framework loading through file descriptors with a bounded
  private-cache fallback for non-seekable providers.
- User-managed SAF library folders with persisted access status, re-selection
  after a lost grant and removal from the app without deleting user files.
- Typed document sources and bounded content-based format detection for
  providers that report generic MIME types.
- Versioned stable book keys for files, archive entries and document-provider
  URIs, with bounded SHA-256 identity for local content.
- API 35 emulator instrumentation for startup, ordinary-file and generic
  `content://` opening, SAF library-root management, persisted position
  restore, locator-free archive-entry metadata, TTS notification actions and
  safe startup after a clean uninstall/reinstall cycle.
- Unit and native regression tests for XML, OPDS, stream and hostile document
  resource limits.
- A documented native ownership policy now requires engine reference wrappers,
  standard containers or `unique_ptr`; the INI translator factory and parsing
  buffer are the first migrated RAII path with a native regression test.
- Native mutable-state rules and a thread-local skin recursion guard prevent
  concurrent renderers from sharing one process-wide inheritance depth budget.
- Formatter, render-measurement and bitmap glyph scratch buffers are isolated
  per thread, and libunibreak initialization is synchronized exactly once.
- Deterministic native document regressions cover forward/reverse search,
  selection boundaries and serialized reading-position restoration.
- The fixed-size native DOM registry now publishes document slots atomically
  and is stress-tested with concurrent construction and destruction.
- Concurrent renderers now read and update the legacy process-wide base font
  weight without a native data race.
- Render DPI settings now use a synchronized API with coherent multi-value
  scaling snapshots instead of directly exposed mutable globals.
- Hyphenation minima and soft-hyphen policy updates are now race-free during
  concurrent rendering.
- Hyphenation dictionary-list and loader lifetimes now use explicit RAII
  ownership with regression coverage for replacement and teardown.
- Concurrent requests for a hyphenation dictionary now share one synchronized,
  RAII-owned method-cache entry.
- Text-language runtime flags now use one atomic snapshot, keeping concurrent
  hyphenation-method selection internally consistent.
- The text-language configuration cache now has synchronized lookup, RAII
  ownership and a documented process-lifecycle teardown boundary.
- The process-wide font manager now has RAII ownership, serialized lifecycle
  operations and rollback-safe initialization.
- Font gamma selection now uses one atomic index and serialized glyph-cache
  invalidation.
- Font antialiasing, hinting, kerning and shaping settings are now synchronized
  with their cache invalidation and live-font updates.
- The bounded glyph LRU cache now reports hit, miss, eviction and byte counters;
  regression coverage also preserves true least-recently-used eviction and
  exact size accounting when the final entry is removed.
- The Android cover-byte cache now enforces hard byte and item limits with
  observable hit, miss and eviction counters.
- The two-entry desktop page-image cache now exposes byte/item counters and no
  longer retains its mutex after cache misses or availability probes.
- The decoded skin-image LRU now reports hit/miss/eviction counters and
  synchronizes lookup, insertion, clearing, and telemetry access.
- The parsed-document cache now enforces its configured capacity from finalized
  file sizes, reports hit/miss/eviction counters, and correctly clears its files.
- The Clang warning gate now prevents signed-comparison and unused-set-variable
  regressions in the hyphenation engine, and signed-comparison regressions in
  Unicode string conversion and matching.
- A native Unicode corpus now covers supplementary UTF-16, combining and
  emoji-ZWJ graphemes, bidirectional text, CJK line breaking and mixed scripts.
- Deterministic native document renders now compare pagination, page bookmarks,
  saved-position restoration and multi-line selection geometry across
  equivalent runs using a vendored font fixture.
- Native typography regressions now cover variable-font shaping,
  document-scoped embedded-font precedence and ordered CJK fallback.
- Native CSS regressions now cover declaration units, pagination aliases,
  cascade specificity, source order and computed inheritance.
- An in-memory EPUB 3 corpus now covers nested navigation, landmarks fallback,
  page lists, refined metadata, semantic notes and static media-overlay policy.
- The Clang warning gate now rejects constructor initializer-order regressions.
- German, Portuguese and Ukrainian hyphenation patterns are refreshed from a
  pinned upstream source; language goldens and desktop/Android parity checks
  now cover all packaged dictionaries.
- SQLite fixture matrices for every supported main and cover database migration,
  including atomic rollback, repeat runs, damaged downstream schema repair and
  future-version rejection.
- Local HTTPS regression fixtures for self-signed, expired and hostname-mismatch
  certificates, plus redirects to an untrusted origin.
- A shared Java/native parse budget with stable safe-failure codes and
  regression coverage for oversized and deeply nested hostile documents.
- An Android source-policy gate that prevents production HTTP callers from
  bypassing the shared connection policy.
- A diagnostic privacy gate that prevents Java and native logging callers from
  bypassing redaction.
- A Clang ThreadSanitizer CI job and a real-thread native smoke test for the
  engine's thread and recursive-mutex primitives.
- Weekly read-only compatibility check against `buggins/coolreader`.

### Changed

- Android baseline is API 21+, compile/target API 35, JDK 17 and the pinned NDK
  declared in the Gradle build.
- UTF-8/UTF-16 conversion now rejects non-scalar values, decodes valid
  surrogate pairs in bounded buffers and replaces unpaired surrogates without
  consuming adjacent text.
- Document-scoped fonts now override an equally named process font only for
  their owning book, and fallback selection accepts a valid first registered
  font while discarding unknown families.
- Intentionally empty Arabic and Persian no-hyphenation dictionaries now load
  as valid packaged policies instead of falling back after a parse error.
- EPUB 3 `noteref`, `footnote` and `endnote` semantics now map to explicit
  reader note hints, including equivalent `doc-*` ARIA roles.
- Android instrumentation now uses the AndroidX test runner, and the obsolete
  Jetifier compatibility pass is disabled after removal of support-library
  dependencies.
- Android document-open flows now carry typed file, archive, temporary-import
  and `content://` sources through the reader task boundary instead of
  collapsing them back to a pathname.
- Android no longer requests broad external-storage permissions, opts into
  legacy storage, probes mount tables or scans hardcoded shared-storage paths.
  Databases and native caches now remain in app-private storage; user-selected
  library access goes through persisted SAF grants.
- The obsolete Billing v3 AIDL integration, billing permission and non-working
  donation purchase UI were removed; the release build makes no in-app purchase
  claims.
- The optional LitRes integration is now consumption-only: catalog browsing,
  trials and login/download of previously purchased books remain, while account
  creation, purchases, balance refill and direct store links were removed.
- Notification actions are immutable, package-scoped and registered as
  non-exported.
- Temporary document cache is capped at 512 MiB and 32 files with
  deterministic oldest-first eviction.
- Database schema 35 rejects unknown future schemas and repairs early
  downstream schema 34 installations.
- Database schema 36 preserves OPDS catalogs while physically removing legacy
  plaintext username and password columns; the catalog editor no longer
  collects credentials that cannot be stored safely.
- Database schema 37 adds source-aware stable book identity, upgrades legacy
  rows without changing book IDs, and therefore preserves bookmark ownership
  while paths move or strong identities replace legacy keys.
- Database upgrades now create a SHA-256-verified, `fsync`ed backup through a
  same-directory temporary file, retain four generations and restore without
  deleting the current database before the replacement is ready.
- Main and cover database upgrades now run as named transactional steps with
  mandatory schema and data postconditions instead of suppressing migration
  errors.
- The Android and JDBC test paths now share one migration runner, and current
  database schemas are verified on every open.
- OPDS and LitRes now share an explicit no-auto-redirect connection factory that
  retains the platform trust manager and hostname verifier for every origin.
- All active Android HTTP paths now share mandatory 60-second connect/read
  timeouts, a 15-minute transfer deadline and checked response-size limits.
- SAF, OPDS, LitRes and the private document cache now apply the same 512 MiB
  document ingress limit as the native parser.
- Native thread completion state is published atomically instead of racing
  between the worker and polling threads.
- Android log messages, uncaught Java exceptions and explicitly exported
  logcat files now remove credentials, URLs, query/fragment data, book names
  and local paths while preserving safe stack frame identifiers.
- Temporary `content://` grants now warn that access may be lost after restart;
  persistable grants are retained, recent entries survive the local-file
  availability filter, and reopening restores stored bookmarks and position.
- TTS is no longer a sticky service that Android can resurrect without a fresh
  user action, and all production dynamic receivers are explicitly
  non-exported.
- The first-run notice now keeps its complete message and action buttons
  reachable on Android 16 tablets in portrait and landscape.

### Security

- Restored platform TLS and hostname validation; LitRes uses HTTPS.
- Hardened redirect handling, XML parsing, OPDS response limits, ZIP/image
  limits and sensitive URL logging. ZIP archives now also enforce entry-count,
  aggregate-size and path-depth limits and reject traversal or duplicate names.
- OPDS authorization and sanitized referrers stay on their original HTTPS
  origin; authenticated LitRes POSTs stay on the pinned LitRes API origin.
- XML/HTML depth and decoded text, individual and aggregate ZIP expansion,
  compression ratio, recursive containers and image dimensions now consume one
  `ParseBudget`; stream wrappers preserve container depth.
- Purged legacy plaintext OPDS/LitRes credentials and stopped accepting new
  plaintext credential persistence.
- Disabled Android cloud backup and device transfer until a safe explicit
  export model is implemented.
- Removed phone-state permission and the exported telephony receiver; TTS
  interruption is handled through Android Audio Focus.
- Routed every production Java/NDK log call through privacy filters and removed
  direct stack-trace/stdout/stderr diagnostics.
- Generic `application/octet-stream` and ZIP intents are rejected before native
  parsing unless a bounded content probe confirms a supported document format.
