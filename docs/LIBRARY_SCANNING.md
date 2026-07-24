# Library scanning

Library scans use a bounded, cancellable pipeline:

1. Discovery runs off the UI thread with iterative depth-first traversal.
2. One directory at a time enters metadata extraction.
3. Files are loaded, parsed and persisted in batches of at most 64.
4. Completed batches update the in-memory directory incrementally.

Only one database/metadata batch is in flight, so producers cannot outrun the
database or parser. User cancellation is checked between filesystem entries,
archive entries, database lookups and parsed files.

## Discovery budgets

Full-tree discovery stops at either:

- 100,000 filesystem/archive entries; or
- 256 directory levels.

These are safe-failure limits, not silent filters. `ScanControl` preserves the
first stop reason (`USER_REQUEST`, `ENTRY_LIMIT` or `DEPTH_LIMIT`) and the
observed entry count. FileBrowser presents a specific message for either safety
limit and leaves the directory unscanned so a smaller root can be selected.

Discovery owns the first 30% of determinate progress; metadata extraction owns
the remaining 70%. Recursive scans use one discovered tree and one FIFO of
directories rather than rediscovering descendants.

## Regression coverage

- `IterativeScanTraversalTest` traverses 10,000 nested nodes without recursion
  and verifies cancellation/depth-limit behavior.
- `ScanBatchCursorTest` processes a 10,000-item corpus with a hard 64-item
  maximum batch.
- `LibraryScanStateTest` checks entry budgets, stable stop reasons and monotonic
  progress.
- Android instrumentation scans 133 books across three directory levels,
  verifies metadata, then exercises user, entry and depth stops.
