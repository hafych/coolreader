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

Known follow-up groups include render configuration globals, hyphenation
registries, document instance tracking and font/cache singletons. Each group
must be migrated separately with an impact check and focused regression tests.
