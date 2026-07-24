# Native cache management

Native caches are migrated independently so their limits and telemetry can be
validated without changing unrelated rendering behavior.

| Cache | Bound | Counters | Status |
| --- | --- | --- | --- |
| Glyph bitmap | Compile-time byte capacity (`GLYPH_CACHE_SIZE`) with LRU eviction | hits, misses, evictions, current bytes, capacity | implemented |
| Rendered page image | 2 entries | hits, misses, evictions, current bytes/items, item capacity | implemented |
| Cover bytes | 512 KiB and 256 entries | hits, misses, evictions, current bytes/items, capacities | implemented |
| Parsed document | pending | pending | follow-up |

Glyph counters are aggregated by the process-wide font manager and exposed
through `GetGlyphCacheStats()`. `ResetGlyphCacheStats()` resets counters without
flushing cached glyphs. Explicit removal and cache clearing do not count as
capacity evictions.

The glyph cache admits at most the configured byte capacity for normal glyphs.
An individual glyph larger than the configured capacity is retained as the sole
entry so callers keep the existing ownership contract; the reported byte size
makes this exceptional state visible.

The Android cover-byte cache uses strict byte and entry bounds. Null and
oversized entries are not retained. Explicit removal and clearing update size
accounting but do not count as capacity evictions.

The desktop rendered-page cache retains two buffers. Cache lookups use scoped
locking on misses and probes, while a returned image holder keeps the mutex only
for the lifetime of the borrowed buffer.
