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
- Nook E-Ink vendor reflection bindings are now immutable and owned by one
  screen-controller generation; disabled devices skip proprietary class
  loading, while legacy and Nook 1.2 invocation paths have local JVM coverage.
- Reader bitmap pools and the optional legacy VM memory tracker are now scoped
  to one reader generation. Tracking is synchronized and uses real row strides
  plus widened byte counts, without shared or overflow-prone static totals.
- Reader actions are immutable command metadata; themed icon overrides and
  device input defaults now belong to one Activity/settings generation.
  Nook key conflicts override generated defaults while preserving explicit
  user mappings, and action catalogs no longer expose mutable arrays.
- Gesture acceleration now uses an immutable curve owned by each reader and
  widened interpolation across the full integer range. Document style codes
  and labels now share one typed immutable catalog instead of parallel arrays.
- Interface theme definitions and their E-Ink visual choices now belong to an
  immutable catalog owned by each Activity; the legacy public theme array has
  been removed.
- External dictionary definitions, including intent data keys, now live in one
  immutable catalog; legacy array callers receive independent snapshots.
- Audio sibling selection and filename transliteration now use private
  immutable catalogs; extension and replacement arrays no longer escape
  process scope or depend on OPDS helper types.
- BaseActivity preference/debug configuration is now immutable, while system
  locale resolution uses a snapshot owned by the current Activity generation.
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
- Parser selection, encoding and format detection, cache-file serialization,
  draw mark collection and SVG/GIF/XPM image decoding now use scoped RAII
  ownership instead of manual temporary-buffer cleanup; encoding probes also
  restore stream position on every exit.
- PNG pixel and row-view storage now survives libpng `longjmp` errors through
  member RAII containers; repeated truncated-IDAT failures release buffers and
  preserve a balanced decode callback lifecycle.
- JPEG source, input, scanline and converted-row storage now uses libjpeg
  lifecycle pools behind a pre-creation `setjmp` boundary; fatal refill errors
  and successful finishes both release state with balanced callbacks.
- Unpacked image snapshots now own grayscale, RGB565 and 32-bit pixels through
  RAII containers, eliminating the legacy 16-bit teardown leak; repeat-decode
  regressions cover every storage depth, while failed predecode rolls back to
  the original image source.
- Scaled-image drawing now owns coordinate maps, decoded RGBA snapshots and
  allocator-specific smooth-scale results through RAII; failed smooth decodes
  no longer render partially initialized image data.
- Default stream-region buffers now use RAII storage and rollback-safe factory
  ownership. Nonzero read/write offsets, write-only initialization and
  idempotent flush are covered without writing partial factory state.
- Memory streams now keep owned and growing buffers in `std::vector` while
  preserving readonly borrowed aliases. Their factories publish only
  successfully initialized streams, growth arithmetic is checked, and reopen
  plus repeated close are rollback-safe.
- Block write-cache payloads and LRU links now use RAII containers and
  `unique_ptr` ownership. Failed eviction or flush retains dirty blocks for
  retry, repeated flush keeps the bounded count coherent, and invalid cache
  dimensions are rejected by the factory.
- File-mapped streams now scope mapping regions, POSIX descriptors and Windows
  handles with RAII wrappers. Open/remap failure cannot publish stale stream
  state, mapped buffer views retain their stream owner, and size/buffer
  arithmetic rejects overflow and shrinking safely.
- File streams now scope `FILE`, Windows handles and owned POSIX descriptors
  while keeping borrowed descriptors explicitly non-owning. Factory/reopen
  rollback is automatic, close is idempotent, append honors sync flags and
  POSIX resize now changes the underlying file before restoring position.
- Directory containers now scope POSIX/Windows scan handles, factory candidates
  and not-yet-adopted item metadata. Enumeration errors roll back partial
  containers, failed metadata reads are skipped safely, and scan handles are
  closed before a successfully populated container is returned.
- ZIP and optional RAR container factories now keep parser candidates and ZIP
  entry metadata under scoped ownership until reference/list adoption. Failed
  normal and fallback parsing releases partial entries automatically, archive
  probes restore the caller stream position, and opened entries retain their
  source independently of the container.
- ZIP inflate/CRC buffers and cached-stream slots now use explicit RAII
  ownership; missing-checksum CRC fallback restores the caller's stream
  position, and cache eviction transfers slot ownership safely.
- ZIP entry construction now scopes stored fragments, deflated source
  fragments and decoder candidates until inflater initialization and
  `LVStreamRef` adoption succeed. Fragment reads/seeks cannot escape entry
  bounds, and malformed local-header offsets or truncated ZIP64 extras are
  rejected before a decoder is published.
- Shared parser read windows and charset conversion tables now use explicit
  RAII ownership; short reads near end of file expose only valid bytes instead
  of stale retained buffer capacity.
- Serialization buffers now own writable storage through `std::vector`, retain
  an explicit non-owning deserialization view and reject shallow copies.
  Autogrowth, legacy buffer adoption and fixed/borrowed overflow paths have
  native regression coverage.
- String collections now own reference-counted slots and hashed collision
  buckets through standard vectors. Copy/assignment no longer share raw
  storage, clearing a hashed collection removes stale indices, and custom
  sorting no longer relies on a process-global comparator pointer.
- DOM name/id maps now keep one `unique_ptr` owner per item and a non-owning
  sorted name index. Deep copies rebuild their views, growth is atomic through
  vectors, and malformed or CRC-failing deserialization preserves the
  previously committed map.
- Native hash tables now own bucket roots and collision chains through
  `vector<unique_ptr>`. Rehash transfers nodes without copying stored values,
  copies rebuild independent chains, and final-bucket collisions are no longer
  skipped by iteration.
- Generic native value arrays now own contiguous storage through
  `unique_ptr<T[]>`; reserve/copy failures preserve committed contents,
  self-appends remain valid across growth, sparse slots are initialized and
  erased reference values are released without discarding reserved capacity.
- Native reference vectors now own constructed `LVRef` slots through
  `unique_ptr<LVRef<T>[]>`. Copy, growth, self-append, sparse set, trim and
  erase no longer depend on raw arrays, mixed allocation families or stale
  inactive references.
- Native pointer vectors now keep slots in `std::vector`, scope owning item
  disposal with `unique_ptr`, roll back partial deep copies and shallow-copy
  borrowed views. Remove/pop transfers clear inactive slots, null gaps survive
  copies, and typed sorting replaces the erased `qsort` bridge.
- Native generic matrices now own contiguous `std::vector` cell storage.
  Bounded transactional resize preserves the old/new overlap across growth and
  shrink, deep copy and reset-source moves replace shallow raw-row aliases, and
  constructed cells are released automatically.
- Pagination compact arrays now combine lazy `unique_ptr` allocation with
  bounded vector payloads, alias-safe batch append and deep copy semantics.
  Rendered lines also scope optional footnote-link lists with `unique_ptr`
  while preserving the lists' non-owning note references and ordering.
- Word/PDB import paths now keep image, encoding-detection and multi-chunk zlib
  buffers in scoped vectors. PDB inflate publishes only complete output, zlib
  teardown is guarded, and PDB stream/container candidates transfer from
  `unique_ptr` only at their reference-counted ownership boundaries.
- WOL export now scopes TOC, page, cover and LZSS buffers in containers,
  rejects oversized images and reports output overflow before publishing.
  WOL reader image/cover results return explicit `unique_ptr` ownership,
  validate exact stream ranges and no longer write an implicit `test.dat`;
  round-trip regression covers overflow rollback and decoded page bytes.
- Compiled CSS declarations now publish vector storage only after a complete
  closing brace. Selector/rule chains own links with `unique_ptr`, copy and
  teardown iteratively, and stylesheet push/pop moves one coherent snapshot
  instead of maintaining parallel raw-owner stacks. Regressions cover
  truncated-declaration rollback, independent copies and 4096 selectors.
- Cache-file ZSTD/zlib contexts and reusable codec chunks now have explicit
  RAII ownership. Pack, unpack, validation and block I/O use bounded
  transactional vectors; corrupt frames preserve prior results, codec errors
  do not invalidate reusable state, and completed blocks move directly into
  serialization buffers or persistent DOM owners without a malloc-return read
  boundary.
- Cache-file index reads and writes now use bounded typed snapshots instead of
  owning raw arrays. Loads validate every record and reject duplicate live
  keys before atomically swapping the owning index, free-list views and lookup
  map; a late corrupt record leaves the previous cache state unchanged.
- DOM blob caches now own payloads and items through vectors and
  `unique_ptr`. Bounded index loads publish transactionally, failed cache
  writes do not create phantom items, and short blobs no longer feed an
  unchecked four-byte diagnostic preview. Native regressions cover RAM/cache
  transfer, index reopen and truncated-index rollback.
- DOM text-storage chunks now own resident bytes through vectors while their
  manager and LRU links remain non-owning. Cache restore validates the indexed
  size before publishing, eviction preserves the serialized position, and
  resident-byte accounting changes exactly once per transition. Raw data views
  are bounds-checked in every build; native regressions cover fixed and dynamic
  chunks plus mismatched-index rollback.
- Persistent DOM element/text node-part catalogs now use fixed arrays of
  `unique_ptr` owners with a malloc-compatible deleter. Cache loads stage both
  catalogs, validate address-space bounds and every part, then clean and swap
  only after complete success; truncated later parts preserve the old DOM.
- Native reference caches now own bucket roots and collision chains through
  vectors of `unique_ptr`, keep indexed metadata in vector storage and export
  serialized indexes with explicit ownership. Index restoration is
  rollback-safe, long collision chains are released iteratively, and bounded
  cache-map slots use vector-backed storage.
- The RTF importer now owns its text accumulator through RAII, flushes
  recoverable truncated input before teardown and emits no document callbacks
  after `OnStop()`. Nested destinations now transfer `unique_ptr` ownership
  through a LIFO owner stack, including malformed-group unwinding.
- The TCR decoder now owns dictionary, block-index and decoded-block storage
  through RAII containers; factory failure is rollback-safe, and regressions
  cover buffer growth, indexed block reuse and truncated dictionaries.
- The Antiword bridge now keeps writer, layout and stream callback state in a
  scoped per-import context while serializing the third-party library; real
  Word fixture imports are covered concurrently, and sequential lists reset
  their state correctly.
- Legacy engine guard mutexes now have built-in recursive fallbacks and
  private RAII owners. Provider setup is all-or-nothing, partial failure rolls
  back safely, and quiescent shutdown clears non-owning views before teardown.
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

### Fixed

- Scroll-view animation frames now use fractional progress instead of truncated
  integer division, and autoscroll duration arithmetic widens before multiplying
  character counts. Draw-time averaging is isolated per reader and validates
  restored samples.
- Reading-time queries no longer increment persisted duration while tracking is
  paused. Lifecycle signals are idempotent, clock regressions cannot subtract
  time, and accumulation saturates instead of overflowing.
- Last-position reading durations now retain long precision for multi-day and
  saturated values instead of narrowing milliseconds to an `int` before
  formatting.
- Natural page-flip table lookups now clamp widened indices through a tested
  geometry component instead of using overflow-prone inline arithmetic and an
  out-of-bounds `length` fallback.
- Android heap diagnostics now allocate one snapshot per invocation, and the
  unused process-wide `ReaderView` date formatter has been removed.
- Android GUI/background tasks queued before handler startup now retain their
  delay and drain exactly once; concurrent handler attachment can no longer
  strand or replay tasks across Activity generations.
- Android Engine mount/font/DOM initialization is now an immutable process
  snapshot, while each Activity service generation owns its path corrector.
- Android hyphenation definitions now publish one frozen JNI snapshot; Java
  enumeration returns copies and late definitions cannot diverge from native
  registry state.
- Natural page-curl lookup curves now live behind one immutable, test-covered
  owner instead of mutable static arrays in `ReaderView`.
- Android synchronous dispatcher handoffs no longer depend on a nested
  `ReaderView` type, release every waiter, preserve interrupts, and complete
  GUI failure paths instead of leaving callers blocked.
- Android option dialogs now keep resource and document-format state per
  Activity generation; backlight values return copies and localized labels no
  longer mutate process-wide arrays.
- Android backlight inactivity timestamps and timer tasks are now owned by one
  Activity generation, with overflow-safe dim/expiry thresholds covered by
  pure JVM tests.
- OPDS timestamps now parse with strict method-scoped formatters, explicit UTC
  handling, and working compact or colon timezone offsets under concurrent use.
- All first-party Clang warnings eliminated across 47 files (~700 diagnostics);
  `sign-compare` and `unused-but-set-variable` promoted to the full
  `-Werror` gate alongside the existing high-confidence classes.
- Cacheable-object ID counter and MathML stylesheet lazy init now use
  `std::atomic` and `std::call_once` instead of unsynchronized statics.
- String-literal interning tables (`cs8`/`cs32`) are mutex-guarded and covered
  by a real-thread regression test.
- The FB2/FB3 first-body flag is now per-document instead of a file-scope
  static shared across all parser instances.
- Approximately 15 read-only globals in `lvdocview`, `textlang` and `props`
  moved to `const` (rodata).

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
