# Android architecture

This document records the ownership and lifecycle rules for the Android
application while the remaining `CoolReader` and `ReaderView` decomposition is
in progress.

## Activity service graph

`BaseActivity` owns one `Services` instance. Starting that instance creates an
immutable `ServiceDependencies` snapshot containing the Engine, Scanner,
History, cover manager, filesystem folders, genre collection, document cache
and one `ServiceLifecycle`.

Components receive either the snapshot or the specific dependency they need.
They must not resolve Activity-owned services through static fields or a global
service locator.

Stopping an Activity closes only its `ServiceLifecycle`, clears that instance's
graph and detaches its Engine. Queued work that captures the lifecycle must
discard callbacks after the lifecycle closes. A stale Activity therefore
cannot clear or stop a graph owned by a newer Activity.

Activity UI helpers follow the same rule. For example, each `BaseActivity` owns
its custom E-Ink toast queue, main-thread handler and popup window, then cancels
and dismisses them during `onDestroy`. Diagnostic heap snapshots are created
per invocation rather than being shared by every Activity generation, and
unused formatter objects are not retained at process scope.

## Process-scoped infrastructure

`BackgroundThread` remains a process-scoped dispatcher. Activity teardown does
not quit it because a newer Activity generation may already use the same
dispatcher. Activity-owned work must use `ServiceLifecycle` for cancellation
instead. Its singleton and handler references use explicit cross-thread
publication. GUI and background tasks submitted before their handler exists
pass through one atomic deferred-queue handoff that preserves delay and removes
delivered entries before a later Activity generation can attach.
Synchronous cross-thread calls use a one-shot `BlockingResult` owned by the
dispatcher layer, not a nested `ReaderView` type. It releases every waiter,
completes failure paths, and restores an interrupted waiter's flag after
delivery.

Database and TTS service connectors use application context for their
process/service lifetime. UI callbacks and Activity references remain
generation-scoped and detachable.

The native Engine's SAF-era process snapshot contains only immutable empty
legacy-root collections, a one-time font initialization candidate and the
final native DOM version. Each Engine generation owns its own
`MountPathCorrector`; no potentially mutable path graph is shared between
Activities.

Packaged and pre-init file hyphenation definitions build through one
synchronized process registry. JNI receives a frozen array snapshot exactly
once; Java callers receive independent arrays, and definitions arriving after
native initialization are rejected instead of appearing only on the Java side.

Scanner filesystem/resource access and cached online-store plugins also retain
only application context. Cached process objects must never capture the
Activity that first requested them.

External document validation is isolated from `CoolReader`: local inputs are
probed by `ExternalDocumentValidator`, while resolver-owned content URIs remain
in the Activity's ContentResolver flow. Storage helpers such as
`DocumentFileCache` depend on `Context`, not on an Activity subtype.

## Testable view geometry

`ReaderView` delegates natural page-flip lookup indexing to the pure
`PageFlipGeometry` component. It widens multiplication before division and
clamps every lookup to the table's last valid index. Boundary and integer-limit
behavior is covered by a local JVM test, without constructing an Activity,
Surface or native document. The matching sine/arcsine page-curl curves are
built once by `PageCurveTables`; its arrays are private, final, instance-owned
storage and the legacy numeric samples are locked by a pure JVM regression.

Options UI state is scoped to each `OptionsDialog` generation. Resource-backed
motion and gesture choices, format capability flags, and icon visibility no
longer live in process-wide fields. Shared backlight values are exposed only
through the pure `BacklightOptions` owner; callers receive copies and localized
titles are built per Activity instead of mutating a global array.

Screen-backlight user-activity timestamps and scheduled timer tasks belong to
the `ScreenBacklightControl` of one `BaseActivity` generation. Threshold and
reschedule decisions are delegated to the pure, overflow-safe
`BacklightTimeoutPolicy`; activity from a replacement screen cannot extend an
older screen's WakeLock.

OPDS feed timestamps are parsed by the pure `FeedTimestampParser`. Formatter
instances are method-scoped, UTC and locale handling are explicit, parsing is
strict, and both compact and colon timezone offsets are normalized without
shared `SimpleDateFormat` state.

Each Nook screen-controller generation owns an immutable
`NookEpdControllerBindings` reflection graph. Disabled devices do not resolve
proprietary classes, legacy devices preserve their static vendor call, and
Nook 1.2 creates its controller only from the current View host. Vendor
methods, constructors, enum arrays and failure diagnostics are not retained in
mutable process-wide fields.

Each `ReaderView` also owns its bitmap pool and `VMRuntimeHack`. The optional
legacy VM reflection bindings are final, accounting is synchronized and uses a
`long`, and failed vendor calls do not corrupt the local total. Bitmap memory
uses the actual row stride; the legacy surface estimate widens before
multiplication. Replacement reader generations therefore cannot share pool or
diagnostic accounting state.

## Migration rule

New Android components should:

1. take dependencies in a constructor or explicit factory method;
2. take `ServiceLifecycle` when work can outlive its caller;
3. avoid static mutable Activity, View or Service references;
4. make teardown idempotent and scoped to the owning generation;
5. expose narrow interfaces so lifecycle and background behavior can be tested
   without launching the complete reader.
