# Native ownership policy

CoolReader's native code predates standard C++ ownership types. New code and
touched legacy paths must make ownership explicit while preserving the
reference-counted types already used by the engine.

## Rules

- Use the engine's `*Ref` types for objects whose APIs already use intrusive
  reference counting, such as `LVStreamRef` and `LVContainerRef`.
- Use automatic storage for fixed-lifetime values and standard containers for
  owned buffers.
- Use `std::unique_ptr` for exclusive heap ownership. Call `release()` only at
  an API boundary that explicitly transfers ownership to the caller.
- A raw pointer is non-owning unless the surrounding legacy API explicitly
  documents a transfer. New owning raw-pointer fields are not allowed.
- Wrap JNI acquisitions, file descriptors, locks and other paired resources in
  an existing guard or a narrow scope guard. Every early return must follow the
  same release path.
- Prefer constructor/destructor lifetime over separate `init`/`uninit` pairs.
  Where a legacy pair cannot yet be removed, document the owner and make
  teardown idempotent.
- Do not mix two ownership systems for the same object. In particular, never
  place an intrusive-reference-counted object in `std::shared_ptr`.

## Migration

Convert one bounded ownership path at a time. Each change must include a
regression test that exercises success and failure/cleanup behavior, followed
by the native test suite and sanitized CI configuration where applicable.

The first migrated path is `CRIniFileTranslator`: its temporary file buffer is
owned by `std::vector`, and its factory uses `std::unique_ptr` until the legacy
raw-pointer return transfers ownership to the caller.

`HyphMan` now owns its dictionary list and data loader with `std::unique_ptr`.
The legacy `setDataLoader(HyphDataLoader *)` call is an explicit ownership
transfer boundary, while `getDictList()` returns a non-owning pointer valid only
between initialization and process-shutdown teardown. Loaded dictionary methods
are owned by a private vector of `std::unique_ptr`; the legacy hash table is now
only a non-owning lookup index.

`TextLangMan` owns cached language configurations with
`std::vector<std::unique_ptr<TextLangCfg>>`. Lookup returns a non-owning pointer
whose address stays stable across cache reordering and growth. It is invalidated
only by the quiescent `HyphMan::uninit()` lifecycle operation.

The global font manager has a private `std::unique_ptr` owner. The legacy
`fontMan` symbol is a non-owning compatibility view and is cleared before the
owner is destroyed. Failed initialization releases the candidate manager
without leaving a dangling published pointer.

Document format selection owns each candidate parser through
`std::unique_ptr<LVFileFormatParser>` until parsing completes. XML, HTML, plain
text and bookmark format probes own their temporary decoded-character buffers
through `std::vector`, including negative detection paths. Encoding
autodetection also uses a scoped buffer and a stream-position guard, so success,
read failure and short-input exits restore the caller's position uniformly.

The shared file-parser read window and text-parser charset conversion table are
owned by `std::vector`. Parser subclasses use indexed access or temporary
non-owning `data()` views; they do not manage either allocation. A seek exposes
only the number of bytes actually read, even when the retained vector capacity
is larger than the short window available near end of file.

The RTF importer also owns its accumulated text through `std::vector`. It
flushes pending text before closing the generated document and calls
`LVXMLParserCallback::OnStop()` only after all text and structural callbacks
have completed, including recovery from a truncated final group. Nested RTF
destinations are owned in LIFO order by `std::unique_ptr`; property-stack raw
pointers are non-owning restoration views. Truncated groups are unwound before
`OnStop()`, so deferred image callbacks remain inside the parser lifecycle.

Word image callbacks now borrow bytes from an operation-scoped `vector`
instead of a manually freed blob. PDB zlib decoding likewise accumulates
multi-chunk output in a bounded vector under an inflate-state guard and swaps
it into the caller only after `Z_STREAM_END`; corrupt input leaves prior output
unchanged. PDB encoding detection uses vector scratch storage, while stream and
container factory candidates remain in `unique_ptr` until their respective
`LVStreamRef` and `LVContainerRef` ownership boundaries accept them.

Cache-file ZSTD and zlib compressor/decompressor contexts are lazy
`unique_ptr` resources whose destructors pair the matching codec teardown with
their vector-backed output chunks. Pack, unpack, validation and block I/O build
operation-scoped vectors and publish results only after a complete codec frame,
size check and CRC check succeed. A corrupt frame leaves the caller's previous
result intact, and a later reset can safely reuse the same context because
error paths no longer call `deflateEnd()` or `inflateEnd()`. `SerialBuf` accepts
completed cache blocks by moving their vector storage; the two persistent DOM
storage consumers retain an explicit malloc-compatible ownership boundary.
Regression coverage exercises multi-chunk ZSTD/zlib round trips, reuse,
corruption recovery, cleanup/recreation and both cache read boundaries.

Temporary draw mark lists and SVG decoder input/output buffers also use
standard RAII containers. These operation-scoped resources cannot outlive
their call and no longer rely on matching cleanup at each return.

The GIF decoder owns input, color-table, compressed-stream, decoded-frame and
output-row storage through `std::vector`. A decoded frame has automatic
operation scope; the unused raw frame array and its mismatched scalar deletion
were removed. Repeated decode and invalid-dimension paths are covered by the
native ownership regression.

The PNG decoder keeps pixel storage and row-pointer views in member
`std::vector` containers because libpng reports fatal decode errors with
`longjmp`. The members survive that C error boundary and are released on both
success and failure. A separate started flag ensures `OnEndDecode(errors=true)`
is emitted only after `OnStartDecode()`, including repeated truncated-IDAT
failures.

The JPEG decoder installs its `setjmp` boundary before decompressor creation.
Its source manager and input bytes use libjpeg's permanent pool, while the
scanline and converted output row use the image pool; all are released by
`jpeg_destroy_decompress()` on success or fatal `longjmp`. Member lifecycle
flags make partial decompressor teardown and `OnEndDecode(errors=true)`
explicit, and successful decoding completes with `jpeg_finish_decompress()`.

XPM parsed rows, palettes and decode rows, plus dummy and draw-buffer conversion
rows, use standard containers. `LVDrawBufImgSource` keeps a non-owning
compatibility view and, only when requested by its legacy factory, a
`std::unique_ptr` owner. Invalid XPM construction releases partially parsed
rows without depending on dimensions that were reset after the error.

`LVUnpackedImgSource` owns grayscale, RGB565 and 32-bit pixel snapshots through
separate `std::vector` buffers and uses a scoped vector for conversion rows.
Only the buffer selected by the requested bit depth is populated. Automatic
teardown also closes the legacy 16-bit leak caused by checking the unrelated
32-bit pointer before freeing RGB565 storage. The factory owns its candidate
through `std::unique_ptr`; a failed source decode releases the partial pixel
snapshot and returns the original source instead of publishing invalid data.

`LVImageScaledDrawCallback` owns nearest-neighbor and nine-patch coordinate
maps plus its full smooth-scaling RGBA snapshot through `std::vector`. The
allocator-specific result returned by `qSmoothScaleImage()` is held by a
scoped `std::unique_ptr` with the matching platform deleter. A failed decode
does not run the smooth post-processing pass over a partial snapshot.

`LVDefStreamBuffer` owns copied stream regions through `std::vector`; its
factory keeps a candidate in `std::unique_ptr` and marks it ready only after
the requested position and optional read preload succeed. Rollback therefore
cannot flush partial data. Write-only regions skip preload, nonzero offsets
validate the position returned by `SetPos()`, and `close()` flushes at most once
before releasing both storage and the stream reference.

The ZIP decoder owns persistent inflate buffers and CRC scratch storage through
`std::vector`; fallback CRC calculation uses a position guard and supports
archives whose header omits the checksum. Entry creation returns `unique_ptr`
and keeps stored fragments, deflated source fragments and decoder candidates
scoped until inflater initialization and the final `LVStreamRef` transfer
succeed. `LVStreamFragment` cannot be copied and clamps reads/seeks to its
declared region, so a stored entry cannot expose following archive bytes.
Local-header offsets and ZIP64 extra records are validated with bounded
arithmetic before a candidate is published. `LVCachedStream` owns cache slots
as `std::unique_ptr<BufItem>` values inside a vector while its LRU links remain
non-owning views. Slot eviction transfers ownership instead of deleting and
reallocating nodes.

The TCR decoder owns dictionary entries, its packed-block index and the
reusable decoded block through `std::vector`. Its factory keeps a candidate
decoder in `std::unique_ptr` until ownership crosses into `LVStreamRef`.
Regression coverage expands a decoded block beyond its initial reserve, seeks
across packed-block boundaries and rejects a truncated dictionary after
partially initialized entries have been released automatically.

The 8-bit and 32-bit string collections own their reference-counted string
slots through `std::vector`, so copy, assignment, insertion, erase and teardown
cannot share or manually shift one raw pointer array. Typed `std::sort`
replaces the process-global custom-comparator bridge. The hashed 32-bit
collection owns collision buckets as nested vectors; copies retain independent
indices, while `clear()` and deserialize discard stale bucket contents before
new strings are indexed.

`LDOMNameIdMap` owns every name/id item exactly once through a vector of
`unique_ptr`; its sorted name vector is a non-owning lookup index into those
stable items. Per-element CSS defaults are also private `unique_ptr` copies.
Map copy and assignment deep-copy the owner index and rebuild name views,
vector growth replaces the former pair of sequential reallocations, and
deserialize swaps in a candidate only after item validation and CRC succeed.

`LVHashTable` owns its bucket roots and collision links through
`vector<unique_ptr>`. Stored pointer values keep their existing non-owning
semantics; only the table nodes are owned. Rehash allocates the replacement
bucket vector first and then transfers existing nodes without copying keys or
values. Copy and assignment rebuild independent chains, removal and clear
unlink iteratively, and iteration includes collisions in the final bucket.

`LVArray` owns its contiguous backing storage through `unique_ptr<T[]>`, which
preserves the legacy raw-view API and supports specializations such as
`LVArray<bool>`. Constructors, copy/assignment, reserve and trim build scoped
candidate arrays before publishing them. Appends snapshot aliased input before
growth, sparse `set()` value-initializes gaps, and erase/reset clear inactive
slots so reference-counted values are released while retained capacity remains
reusable.

`LVRefVec` owns its contiguous slots through `unique_ptr<LVRef<T>[]>`.
Copy/assignment and reserve publish only complete candidate arrays, while
append snapshots aliased ranges so self-add remains finite across growth.
Sparse `set()` allocates the addressed slot, trim constructs real reference
objects instead of writing into `malloc` bytes, and erase clears inactive slots
immediately. Its public icon/battery-facing API remains unchanged.

`LVPtrVector` keeps its contiguous compatibility slots in `vector<T*>`.
`ownItems=true` remains an owning adoption contract: replacement, erase and
clear move each slot through a local `unique_ptr` before destruction, while
`remove()` and `pop()` clear the inactive slot and transfer the raw pointer to
their caller. Owning copies build all clones under temporary `unique_ptr`
guards before publishing them and preserve null gaps. `ownItems=false` copies
only borrowed views and never deletes them. Typed `std::sort` replaces the
erased `qsort` bridge while `get()` retains the legacy contiguous view.

`LVMatrix` owns one contiguous `vector<T>` instead of a manually allocated
row table. `SetSize()` rejects invalid or oversized dimensions, constructs and
fills candidate storage before publishing it, and copies only the old/new
overlap so both row and column shrink are well-defined. Container copy is deep,
move resets the source dimensions, `Clear()` releases constructed cells, and
the legacy row-indexing interface remains available for mutable and const
matrices.

Pagination `CompactArray` keeps its small-object lazy allocation through a
`unique_ptr`, while the allocated payload is a bounded `vector<T>`. Batch
append snapshots its input before growth so self-appends are safe, and
copy/assignment duplicate storage instead of sharing a raw owner.
`LVRendLineInfo` likewise owns its optional footnote-link list with
`unique_ptr`; copies duplicate the non-owning pointer list, moves transfer it,
and `clear()` releases the list without manual deletion. The page splitter's
existing indexing, serialization and link-order contracts remain unchanged.

`LVRefCache` and `LVIndexedRefCache` own bucket roots and collision links
through vectors of `unique_ptr`. Collision teardown unlinks iteratively, while
the indexed cache keeps only non-owning node views in vector-backed index
metadata. Index growth completes before a new node is published, `setIndex()`
swaps in a complete candidate, invalid or null entries stay outside the cache,
and `getIndex()` returns explicit `unique_ptr` ownership to DOM serialization
callers. The adjacent bounded `LVCacheMap` stores its fixed-capacity slots in a
vector and treats non-positive capacities as an empty reusable cache.

`SerialBuf` owns writable fixed-size and auto-growing storage through
`std::vector`; its raw `_buf` member is only a compatibility view. The
deserialization constructor remains explicitly borrowed, while the legacy
`set(lUInt8 *, int)` boundary takes a malloc-allocated buffer into a scoped
owner, copies it into container storage and releases it automatically. Shallow
copying is disabled so the view cannot detach from its owner. Regression
coverage exercises CRC round-tripping, autogrowth, legacy adoption, swapping
and fixed/borrowed overflow rejection.

`LVMemoryStream` keeps owned initial, copied and auto-growing buffers in
`std::vector`; `m_pBuffer` is either a view of that storage or an explicitly
borrowed readonly alias. Borrowed streams cannot resize and never release
caller storage. Reopening first releases any previous owned storage, while
`Close()` is idempotent. Factories retain candidates in `std::unique_ptr` until
creation or copying succeeds, so invalid sizes and short source reads cannot
publish partial streams. Regression coverage includes multi-page growth,
growth-overflow rollback, borrowed aliasing, deep copies, reopen/close and
failed factory construction.

`LVBlockWriteStream` owns each block payload through `std::vector` and its
bounded LRU chain through nested `std::unique_ptr` links. New blocks stay in a
local owner until preload and mutation succeed. Eviction and flush unlink a
block only after the complete base-stream write succeeds; a short or failed
write leaves dirty markers and ownership intact for retry. Full and timed
flushes update the cached-block count as nodes leave the chain, and the factory
rejects non-positive block dimensions. Regression coverage reads across
cached/flushed blocks, reuses the cache after flush and retries a failed
single-slot eviction without losing dirty bytes.

`LVFileMappedStream` owns its mapping through a length-aware `MappedRegion` and
its platform resource through `ScopedDescriptor` or `ScopedHandle`. Member
destruction order unmaps the view before closing the mapping and file handles.
`LVBuffer::m_buf` remains a non-owning zero-copy view whose `LVStreamRef`
anchors the stream lifetime. The factory keeps candidates in `std::unique_ptr`
until open and mapping succeed, while reopen failure clears the previous
mapping and publishes `LVOM_ERROR`. Regression coverage exercises anchored
readonly views, writable shared mappings, grow/remap, shrink rejection,
persisted writes, failed reopen and empty-file mapping rollback.

`LVFileStream` scopes ANSI `FILE`, Windows `HANDLE` and owned POSIX descriptor
resources. A borrowed POSIX descriptor is stored separately and is never
passed through the closing wrapper, so `autoClose=false` remains an explicit
non-owning contract. Filename and descriptor factories return `unique_ptr`
until ownership crosses into `LVStreamRef`; open candidates are published only
after metadata and seek validation. Close is idempotent, failed reopen clears
stale state, append preserves sync flags and starts at EOF, and POSIX resize
uses `ftruncate` before restoring a position clamped to the new length.

`LVDirectoryContainer` keeps each factory candidate in `unique_ptr` until it
crosses into `LVContainerRef`. Windows enumeration uses `ScopedFindHandle` and
POSIX enumeration uses a `unique_ptr<DIR>` with the matching `closedir`
deleter, so all success and rollback paths close the scan resource before the
container is returned. New item metadata also stays in `unique_ptr` until the
legacy owning `LVPtrVector` adopts it through `Add()`. Enumeration errors roll
back the complete candidate, while failed `stat()` calls skip the affected
entry instead of publishing uninitialized metadata. The parent pointer remains
an explicitly non-owning compatibility view.

Archive probing keeps ZIP and optional RAR container candidates in
`unique_ptr` until `LVContainerRef` adopts a successful parse. ZIP entry
metadata likewise remains scoped until the legacy owning item list accepts it;
failed normal/alternative parser attempts therefore release every partial
entry with the candidate. A position guard holds only a non-owning stream view
anchored by the function's `LVStreamRef` and restores the caller position after
successful and failed format probes. `LVArcContainerBase` cannot be copied, its
`LVStreamRef` retains archive bytes for later entry creation, and each opened
entry retains the source it needs after the container itself is released. The
archive parent pointer remains an explicitly non-owning compatibility view.
