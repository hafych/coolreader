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

## Process-scoped infrastructure

`BackgroundThread` remains a process-scoped dispatcher. Activity teardown does
not quit it because a newer Activity generation may already use the same
dispatcher. Activity-owned work must use `ServiceLifecycle` for cancellation
instead.

Database and TTS service connectors use application context for their
process/service lifetime. UI callbacks and Activity references remain
generation-scoped and detachable.

## Migration rule

New Android components should:

1. take dependencies in a constructor or explicit factory method;
2. take `ServiceLifecycle` when work can outlive its caller;
3. avoid static mutable Activity, View or Service references;
4. make teardown idempotent and scoped to the owning generation;
5. expose narrow interfaces so lifecycle and background behavior can be tested
   without launching the complete reader.
