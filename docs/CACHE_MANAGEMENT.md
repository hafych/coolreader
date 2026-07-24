# Native cache management

Native caches are migrated independently so their limits and telemetry can be
validated without changing unrelated rendering behavior.

| Cache | Bound | Counters | Status |
| --- | --- | --- | --- |
| Glyph bitmap | Compile-time byte capacity (`GLYPH_CACHE_SIZE`) with LRU eviction | hits, misses, evictions, current bytes, capacity | implemented |
| Decoded image | pending | pending | follow-up |
| Cover | pending | pending | follow-up |
| Parsed document | pending | pending | follow-up |

Glyph counters are aggregated by the process-wide font manager and exposed
through `GetGlyphCacheStats()`. `ResetGlyphCacheStats()` resets counters without
flushing cached glyphs. Explicit removal and cache clearing do not count as
capacity evictions.

The glyph cache admits at most the configured byte capacity for normal glyphs.
An individual glyph larger than the configured capacity is retained as the sole
entry so callers keep the existing ownership contract; the reported byte size
makes this exceptional state visible.
