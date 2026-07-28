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

Text-language mode flags are published as one atomic bitmask. Hyphenation
method selection consumes one coherent snapshot, so it cannot combine a stale
derived override flag with newer mode values. The main-language string and the
RAII-owned language-configuration cache are protected by one mutex; concurrent
lookups for one tag publish one stable configuration. Returned pointers are
non-owning and remain valid until `HyphMan::uninit()`. Teardown is a quiescent
process-lifecycle operation and must not race readers. Main-language and cached
tag storage use deep copies at the lock boundary because desktop `lString`
reference counts are not atomic.

The process-wide font-manager lifetime is serialized and owned by a private
`std::unique_ptr`. The public `fontMan` pointer remains a non-owning compatibility
view between successful `InitFontManager()` and `ShutdownFontManager()` calls.
Those lifecycle calls are quiescent operations and must not race font users.

The font gamma setting is stored as one atomic table index, while setters
serialize index changes with glyph-cache invalidation. Glyph creation takes one
index snapshot for each bitmap correction.

The antialiasing, hinting, kerning and shaping settings use atomic storage for
readers. Their setters are serialized with cache invalidation and propagation
to live font instances.

Known follow-up groups include the font manager's internal caches. Each group
must be migrated separately with an impact check and focused regression tests.

The string-literal interning tables (`cs8`, `cs32`) are append-only,
process-lifetime hash tables. Each table is guarded by its own `std::mutex`;
the lock is held for both lookup and insert because open-addressing reads can
race with concurrent inserts. The tables are cold after startup — subsequent
calls with the same literal hit the existing entry under the lock.

The custom string-chunk, block-size and reference-count pools remain
single-threaded process-lifecycle allocators. Their initialization, runtime
allocation, teardown and possible reinitialization must all be externally
serialized. A one-time primitive must not be used for these pools while their
public teardown path permits later reinitialization. Multi-threaded Android
builds keep `LDOM_USE_OWN_MEM_MAN=0`.

The FB2/FB3 first-body flag (`IS_FIRST_BODY`) was a file-scope static shared
across all document instances. It is now a per-document boolean
(`ldomDocument::_firstBodyPending`) with public accessors, eliminating
cross-document state leakage during concurrent or re-entrant parsing.

The cacheable-object ID counter uses `std::atomic<lUInt32>` with pre-increment,
replacing an unsynchronized `static lUInt32`. MathML stylesheet lazy loading
uses `std::call_once` instead of a check-then-act boolean.
