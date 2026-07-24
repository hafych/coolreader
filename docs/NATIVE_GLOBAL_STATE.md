# Native mutable-state policy

Native parser, rendering and cache code must not use unguarded process-wide
mutable state. State should be owned by a document, view or operation whenever
possible.

## Allowed lifetime and synchronization

- Immutable lookup tables may remain process-wide and must be declared
  `const`.
- One-time library initialization must use a platform once primitive.
- Shared caches must have an explicit process lifetime, a documented lock and
  bounded capacity.
- Scratch buffers and recursion counters may be `thread_local` when ownership
  by an operation would add disproportionate allocation cost. Their contents
  must never escape the calling thread.
- Mutable rendering options belong to a document or view unless they are
  explicitly immutable after engine startup.

## Migration inventory

The skin inheritance recursion counter is thread-local, so an unrelated
renderer cannot consume another thread's recursion budget. Formatter, render
measurement and bitmap glyph scratch buffers are also thread-local, while
libunibreak table initialization uses `std::call_once`. Native concurrency and
source-policy tests protect these boundaries.

The fixed-size DOM document registry now uses atomic slots and atomic
round-robin allocation. Concurrent construction and destruction are covered by
the document regression test; callers must still ensure no node outlives its
owning document.

The legacy process-wide base font-weight API is retained for compatibility,
but its value is atomically published and constrained to the documented
`1..999` range. Concurrent setter/getter coverage protects this boundary.

Render DPI and font-DPI scaling also retain process lifetime for compatibility.
Their storage is private and atomic, access is limited to explicit setter/getter
functions, and multi-value scaling sites take one DPI snapshot where their
calculations must agree. The unused `gRootFontSize` declaration was removed.

Process-wide hyphenation minima and the soft-hyphen policy are atomically
published, range-checked where applicable and covered by concurrent regression
tests.

The hyphenation dictionary list and data loader now have explicit RAII
ownership. Their initialization and teardown remain process lifecycle
operations and must not race readers.

The loaded hyphenation-method cache owns values through `std::unique_ptr` and
serializes lookup/load/publication. Concurrent requests for one dictionary are
covered by a regression that requires one load and one shared method instance.

Known follow-up groups include text-language configuration and font/cache
singletons. Each group must be migrated separately with an impact check and
focused regression tests.
