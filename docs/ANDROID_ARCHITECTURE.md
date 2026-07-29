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

Database and TTS service connectors use application context for their
process/service lifetime. UI callbacks and Activity references remain
generation-scoped and detachable.

The native Engine's SAF-era process snapshot contains only immutable empty
legacy-root collections, a one-time font initialization candidate and the
final native DOM version. Each Engine generation owns its own
`MountPathCorrector`; no potentially mutable path graph is shared between
Activities.

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
Surface or native document.

## Migration rule

New Android components should:

1. take dependencies in a constructor or explicit factory method;
2. take `ServiceLifecycle` when work can outlive its caller;
3. avoid static mutable Activity, View or Service references;
4. make teardown idempotent and scoped to the owning generation;
5. expose narrow interfaces so lifecycle and background behavior can be tested
   without launching the complete reader.
