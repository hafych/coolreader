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

The active and fallback `CRI18NTranslator` slots are exclusive `unique_ptr`
owners. Their raw setters remain explicit adoption boundaries, accept `NULL`
as teardown, and move an already published translator between slots rather
than duplicating ownership. Every GUI producer keeps a `.mo` translator in a
scoped candidate until loading succeeds and publication releases it to the
selected slot; failed candidates now clean up without a manual `delete`.

Property-file loading owns its bounded input through `std::vector`, requires an
exact stream read and applies decoded values to a cloned candidate before
publishing the complete merged snapshot. Short reads, oversized input and
invalid stream modes preserve the prior properties. Escape decoding bounds a
trailing backslash and comment lines cannot become settings. Saving stages the
complete UTF-8 representation in an owned `lString8` and reports both target
errors and short successful writes instead of hiding them behind an unchecked
stream pump. Binary property restore bounds its entry count by the remaining
payload and CRC, decodes into an isolated container, and replaces live values
only after the trailing CRC succeeds. Container and sub-container clones,
factories and newly inserted items remain in `unique_ptr` until the intrusive
reference or pre-reserved owning list accepts them. A sub-container snapshot
regression verifies stripped names, independent values and later insertion
without shared item ownership.

Serialized CSS style records validate their input magic without mutating the
buffer, decode into isolated record state, and publish only after the stored
style hash matches. Publication preserves the live intrusive reference count,
runtime flags and exclusive before/after pseudo-style owners because these
fields are intentionally absent from the cache format. The surrounding sparse
style index is capped by its `lUInt16` consumers, rejects duplicate slots, owns
each decoded record before intrusive adoption, and reaches the live reference
cache only after its terminator and trailing magic have both been validated.

The document-cache directory index similarly bounds entry count by the
remaining bytes and CRC footer, fills an isolated owning vector, and swaps it
into live MRU state only after CRC validation. Both restore and new-MRU paths
stage `FileItem` values in `unique_ptr` before transferring them to the owning
pointer vector.

The cached document render header stages all six scalar fields and publishes
them only after magic and CRC validation, so a late checksum failure cannot
leave dimensions, flags or style hashes partially replaced.

`HyphMan` now owns its dictionary list and data loader with `std::unique_ptr`.
The legacy `setDataLoader(HyphDataLoader *)` call is an explicit ownership
transfer boundary, while `getDictList()` returns a non-owning pointer valid only
between initialization and process-shutdown teardown. Loaded dictionary methods
are owned by a private vector of `std::unique_ptr`; the legacy hash table is now
only a non-owning lookup index. Each TeX dictionary owns its fixed hash buckets
and sorted collision links with nested `unique_ptr`; parser candidates remain
scoped until insertion and oversized patterns disappear without manual
deletion. Dictionary destruction unlinks long chains iteratively before the
bounded bucket array is released. Native coverage forces 128 patterns into one
bucket, rejects an oversized candidate, validates a tail match and repeats the
whole lifecycle under sanitizers.

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

Encoding autodetection accumulates double-character frequencies in a fixed
array of sparse `unique_ptr<lUInt16[]>` row owners and builds its sortable
output in a scoped vector. The obsolete parallel tree implementation and its
recursive manual deletion are gone. The live collector cannot be copied, and
reset releases every allocated row while restoring both counters so the same
object can run another collection cycle. Native regression coverage verifies
scaled output, complete row teardown and successful reuse after reset.
The offline statistic generator likewise owns its input `FILE` with a scoped
closer and its complete input bytes with a vector. Seek/read failures unwind
both resources uniformly, and files larger than the downstream `int` size
contract are rejected before allocation. Native coverage generates both table
output and the matching registry entry from a temporary HTML fixture.

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
error paths no longer call `deflateEnd()` or `inflateEnd()`. `SerialBuf`, DOM
text-storage chunks and persistent node-part loading accept completed cache
blocks through vector storage; no malloc-return CacheFile read boundary
remains. Regression coverage exercises multi-chunk ZSTD/zlib round trips,
reuse, corruption recovery, cleanup/recreation and the direct-vector and
serialization read boundaries.

Cache-file index serialization uses bounded `vector<CacheFileItem>` snapshots.
On load, every record and live block key is validated before candidate owning
items, free-list views and the lookup map are assembled. The three structures
publish through no-throw swaps only after the complete index succeeds, so a
short read, allocation failure, duplicate key or late invalid record releases
all candidates and preserves the previous cache state. Index writes likewise
release their snapshot automatically on every failed write or header update.
New live block records also remain in a `unique_ptr` while the owning index
reserves capacity and the lookup map accepts its borrowed view; ownership
transfers only after both publication steps are non-throwing. Regression
coverage reopens a valid multi-block index, verifies rollback after a late
record is corrupted while its aggregate CRC remains valid, and exercises new
record publication plus free-list reuse.

DOM blob payloads live in `vector` storage, while the blob cache owns items as
`vector<unique_ptr>`. Index loading builds a bounded candidate list and swaps
it into place only after every name, size and 16-bit block index validates, so
a truncated index cannot expose a partial cache. New items reserve their list
slot before external cache writes and enter the list only after the payload or
cache block succeeds. Small diagnostic blobs no longer trigger a four-byte
out-of-bounds preview. Regression coverage follows RAM blobs into CacheFile,
reopens them through the persisted index and rejects a deliberately truncated
index without publishing its valid prefix.

DOM text-storage chunks own resident uncompressed bytes in `vector` storage.
Their manager pointer and doubly linked LRU neighbors are non-owning views, and
copying is disabled because it would duplicate both those links and resident
byte accounting. Eviction releases vector capacity but deliberately preserves
the serialized write position; restoration reads into a scoped candidate,
checks that size against the chunk index and publishes it only after accounting
can be updated. Fixed raw-data access and node-item views enforce their byte
ranges in every build. Regression coverage follows both fixed and dynamically
allocated chunks through eviction and restore, verifies exact accounting and
rejects a mismatched indexed size without publishing bytes.

Persistent DOM element/text node parts retain their required calloc-compatible
layout but each block is owned by `unique_ptr` with one `free` deleter, and the
4096-slot catalogs are fixed `std::array` owners. Cache restoration reads each
part into scoped vector bytes, adopts only a full-size validated block and
stages both catalogs before touching the live DOM. Once both candidates
succeed, existing node payloads follow `onCollectionDestroy()` and the
catalogs swap without relocating any node. Count bounds match the encoded
28-bit address space. Regression coverage verifies that a truncated later part
preserves the prior catalog and that a corrected cache replaces both catalogs.

`tinyNodeCollection` exclusively owns its persistent `CacheFile` in a
`unique_ptr`. Opening or creating a cache keeps the candidate scoped until it
is complete, then one adoption boundary moves it into the collection and
publishes borrowed views to the four data-storage managers and blob cache.
Those dependants are declared after the owner, so their destructors run first
and cannot observe a released cache. The node-part regression also verifies
that adoption empties the candidate, preserves one owner/view identity and
uses that cache through failed rollback and a successful repeated load.

Temporary draw mark lists and SVG decoder input/output buffers also use
standard RAII containers. Parsed NanoSVG images and rasterizers have
`unique_ptr` owners with library deleters, while bounded RGBA vectors cover
decode and PNG-conversion scratch. These operation-scoped resources cannot
outlive their call, including when a decode callback throws. The PNG byte
result remains the public raw ownership-transfer boundary for callers.

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

`LVImageSource` owns cached nine-patch metadata through `std::unique_ptr`; its
public raw pointers are non-owning views into that cache. Detection decodes
into a scoped candidate and publishes it only after the source reports success
and the complete frame validates, so failed or invalid probes retain no
partial metadata. `LVColorTransformImgSource` likewise owns its full-image
workspace with `std::unique_ptr`, while its downstream callback is borrowed
only during synchronous `Decode()`. Decoder errors discard buffered rows, and
a source that omits `OnEndDecode()` is closed with an error callback before the
workspace and borrowed callback view are cleared.

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

`LVColorDrawBuf` and `LVGrayDrawBuf` keep their public raw scanline API as a
non-owning view, while directly allocated pixel backing lives in a private
`std::vector`. Windows DIB backing retains its handle representation but is
replaced transactionally under scoped candidate-handle guards. External-buffer
constructors explicitly mark borrowed storage, and copying or moving either
wrapper is disabled. A resize, rotation or bitmap conversion first builds a
checked, bounded candidate and publishes it only after allocation succeeds;
operations that detach from borrowed memory never release the caller's buffer.
The color buffer retains its legacy borrowed-resize no-op contract. Gray
buffers include the diagnostic guard byte inside their owned vector, including
every converted bitmap row.

WOL writing stages TOC records, page pixels, cover pixels and LZSS output in
scoped vectors. Image-size arithmetic is checked against a 64 MiB decoded
limit, the compressor reports output overflow, and its required trailing byte
has reserved capacity before publication. Reader-side compressed/decoded
buffers are equally bounded, require exact reads and return cover/image
objects through `unique_ptr`; decoding no longer creates an implicit
`test.dat` file. The native round trip verifies byte-identical page recovery,
undersized-output failure and oversized-input rollback.

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
indices, while deserialization validates magic, count and CRC in a complete
candidate and swaps string owners and lookup buckets together.

The desktop custom string-chunk allocator preserves its fixed-size freelist
ABI, while a bounded array of `unique_ptr` now owns every allocated slice.
Each slice likewise owns its malloc-backed chunk array through a matching
deleter; `pChunks`, `pEnd` and `pFree` are internal borrowed cursors. Storage is
a function-local lifecycle object created by the first dependent string, so it
outlives that string's static teardown. A local regression instance forces
slice growth, verifies freelist reuse, performs repeated clear, and reinitializes
8-, 16- and 32-bit chunk paths under sanitizers.

The desktop DOM block allocator likewise owns each malloc-backed slice through
an allocator-matched `unique_ptr`, and its bounded slice and size-class tables
hold exclusive owners rather than raw process-lifetime pointers. Free-list
cursors remain borrows. Block sizes are rounded up to pointer alignment before
links are stored, requests from 1 through 64 bytes map to all 16 local classes,
and larger allocations retain the original requested byte count when falling
back to `malloc`. The reference-count and class-specific pools use
first-dependent-use storage; the direct size-class and reference-count pools
can be cleared and reinitialized at a quiescent lifecycle boundary. Native
coverage forces slice growth and reuse, repeated clear, every size-class
boundary, and the 64-to-65-byte fallback transition under sanitizers.

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

Generic `LVRef` raw-pointer adoption keeps the candidate object in a local
`unique_ptr` until its reference record has been allocated. Raw assignment
stages both the candidate object and replacement record before releasing the
committed reference, so allocation failure deletes the rejected candidate and
leaves the old value intact. `clone()` returns an owning value and copies the
referenced object rather than the control record. A fail-once record-allocation
regression covers constructor cleanup, assignment rollback, clone independence
and final teardown.

`LVRefVec` owns its contiguous slots through `unique_ptr<LVRef<T>[]>`.
Copy/assignment and reserve publish only complete candidate arrays, while
append snapshots aliased ranges so self-add remains finite across growth.
Sparse `set()` allocates the addressed slot, trim constructs real reference
objects instead of writing into `malloc` bytes, and erase clears inactive slots
immediately. Its public icon/battery-facing API remains unchanged.

`LVPtrVector` keeps its contiguous compatibility slots in `vector<T*>`.
`insert()` and `set()` create a compile-time-selected adoption guard before
validating indices or growing slot storage: the owning specialization holds a
`unique_ptr`, while the borrowed specialization keeps a non-owning view. A
rejected owning operation therefore deletes its unpublished candidate; a
rejected borrowed operation leaves the viewed object with its caller.
Replacement, erase and clear move each owned slot through a local `unique_ptr`
before destruction, while `remove()` and `pop()` clear the inactive slot and
transfer the raw pointer to their caller. Owning copies build all clones under
temporary `unique_ptr` guards before publishing them and preserve null gaps.
Typed `std::sort` replaces the erased `qsort` bridge while `get()` retains the
legacy contiguous view.

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
and `clear()` releases the list without manual deletion. Rendered page
deserialization stages scalar fields and compact footnotes before publication.
Page-list counts are bounded by the bytes remaining ahead of the magic/CRC
footer, each page enters a `unique_ptr`, and the complete candidate list swaps
into place only after nested decoding and the late CRC succeed. Failed or
oversized snapshots preserve both the committed pages and nonlinear-flow
state; a successful linear snapshot clears stale derived state. The page
splitter also retains every newly assembled page and virtual line in an
operation-scoped `unique_ptr` until the owning page/line vector accepts it,
including overflow slicing and cropped forward-line paths. Its indexing,
serialization and link-order contracts remain unchanged.

Compiled CSS declarations own instruction words in `std::vector` and swap a
candidate into place only after the complete declaration and closing brace
have parsed, so malformed replacement text preserves the committed rules.
Selector and selector-rule links use `unique_ptr`; deep copies and hash walks
are iterative, and explicit destructors unlink before deleting so long chains
cannot recurse through teardown. `LVStyleSheet` buckets own their chain heads
directly, while each push snapshot combines the selector count and deep-copied
buckets in one move-only object. Regression coverage includes declaration
rollback, independent clear/copy, snapshot restore and a 4096-selector chain.

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

`LVFormatter` keeps dynamically sized text, flag, source, index, width and
optional bidi workspaces in vectors; its raw members are non-owning active
views. Small paragraphs still borrow per-thread static arrays, but an explicit
storage state now owns that lease and releases it idempotently from normal
cleanup or the destructor. Nested formatters fall back to dynamic storage,
while growth constructs every candidate array before publishing any new view.
The public `formatted_text_fragment_t` C layout and render-facing views remain
unchanged.

Formatted source, line, word and float graphs use private C++ owner extensions
whose base subobjects are the unchanged public C structs. Source records live
in a vector with copied text held by parallel `unique_ptr` slots; formatted
lines and embedded floats live in vectors of `unique_ptr`, with separate
contiguous raw-pointer views for existing draw and debug consumers. Word arrays
and float link collections are likewise container-owned. Growth reserves every
owner/view pair before publishing a new element, formatted-data clear destroys
the owned graph while retaining sources, and the public alloc/free functions
remain the explicit raw ownership boundary. `LFormattedText` now holds that
boundary in a `unique_ptr`, disables shallow copies and swaps a complete
replacement during `Clear()`.

History XML parsing keeps the current file and bookmark in `unique_ptr`
candidates until the owning pointer vectors adopt them. A complete history is
parsed into a temporary `CRFileHist` and swapped into the live object only after
the parser succeeds, so over-depth or otherwise rejected input releases its
valid prefix and preserves the prior snapshot. `ChangeInfo` owns its bookmark
copy directly, deep-copies that state, and keeps factory candidates scoped until
the legacy raw-return boundary transfers ownership. Regression coverage also
round-trips escaped synchronization text, ensuring decoding reads the encoded
input rather than uninitialized output storage.

Interactive bookmark mutation follows the same ownership boundary. Range,
page and shortcut bookmark candidates stay in `unique_ptr` until the owning
record has reserved their slot. Shortcut replacement uses the pointer
vector's owning `set()` operation, releasing the previous bookmark exactly
once instead of overwriting its raw slot. Whole-list replacement clones a
bounded candidate list and publishes it with `swap`, while removal retains the
transferred owner through highlight-range rebuilding. Highlight
`ldomXRange` candidates are likewise scoped until the owning range list adopts
them, and newly created history records remain scoped until reserved list
publication. The rendered document regression covers range/page creation,
shortcut replacement without list growth, independent list cloning and
removal with rebuilt highlights.

The common XML and HTML document factories keep both the candidate
`ldomDocument` and parser scope-owned, releasing the document only through the
legacy raw-return API after format detection and parsing succeed. OPC packages
own relation tables in a vector of `unique_ptr`; the hash table contains only
stable non-owning views and is published together with the complete owner
candidate. Content-type and core-property parse documents are scoped in the
same way. An FB3 import context borrows its enclosing package, owns its cached
description DOM and returns only a non-owning view, while the body parser uses
automatic storage. The end-to-end in-memory FB3 archive regression covers
content types, relationships, metadata, cached description, successful import,
and XML-depth rejection without publishing partial documents.

CHM container and entry streams share a small intrusive owner that keeps the
external `chmFile` handle and its source stream alive until the final consumer
is released; `chm_close()` is paired in that owner's destructor. Container,
entry and enumerated-item candidates remain in `unique_ptr` until their legacy
reference or owning-list boundary accepts them. The `#SYSTEM`, `#URLTBL` and
`#URLSTR` metadata chain uses nested `unique_ptr` owners and move-returning
factories, while CHM HTML and TOC documents keep both DOM and parser candidates
scoped until successful publication. Regression coverage reads a real CHM
entry after releasing its container and source, repeats the metadata owner
chain, rejects malformed metadata and HTML, and verifies failed container
probing does not publish a candidate.

EPUB encryption metadata is parsed into a vector of scoped item candidates and
published only after the complete XML document succeeds; a failed parse
therefore releases its valid prefix without changing the decrypting container.
Encrypted-item lookup normalizes leading separators before comparing the
requested path with the stored URI. Each font-demangling stream owns a snapshot
of the Adobe key and XORs only the bytes actually returned by its base stream,
so the stream remains valid after its container is released. Decryptor,
manifest-item and OPF/nav/NCX/page-map document candidates remain in
`unique_ptr` until their explicit `LVContainerRef`, owning-list or legacy
factory boundary accepts them. The in-memory EPUB regression covers a valid
obfuscated font, malformed encryption rollback and cover/font streams that
outlive their source container.

`LVDocView` is the sole owner of its primary `ldomDocument`, held in a
`unique_ptr`; the public `getDocument()` API remains a borrowed compatibility
view. Importers, writers, bookmark conversion and metadata extractors receive
that view explicitly through `get()`. `Clear()` releases the owner
idempotently, document recreation enters the owner immediately, and copying or
assigning a view is disabled so ownership cannot be duplicated. The document
regression replaces a populated document, verifies that no nodes from the old
DOM survive, repeats `Clear()`, and recreates a usable document afterward.

ODT metadata parsing adopts the temporary `meta.xml` DOM immediately into a
`unique_ptr`. Style and list-style handlers keep candidates only in their
intrusive reference owners; they no longer maintain parallel raw aliases, and
release their handler-held reference after the import context adopts a
candidate. Handler/context links are non-null borrowed references, while
paragraph and list property pointers remain short-lived views bounded by their
active style element. The in-memory ODT regression verifies metadata, language,
paragraph/list style publication, cleanup after a depth-rejected style document
with a valid prefix, and a successful repeated import in the same view.

Skin loaders adopt each parsed XML document immediately into a `unique_ptr` and
move it into the skin only after parsing succeeds. Simple/container skin
factories and skin-list items remain scope-owned until their legacy intrusive or
owning-list publication boundary accepts them. Background-icon and toolbar-
button candidates enter intrusive refs before any property lookup, so both the
successful item and the sentinel candidate that ends each list have automatic
teardown. Regression coverage verifies parsed menu/icon/button publication,
depth-rejected DOM cleanup, a clean repeated load after rejection, and the
directory plus skin-list factory boundaries.

`LVQueue` delegates node ownership to `std::list`, supports move-only values,
and cannot be copied, so its linked storage can no longer be shallow-copied or
manually torn down. `CRThreadExecutor` owns its monitor and worker with
`unique_ptr`, uses an atomic stop flag, and queues `unique_ptr<CRRunnable>`
values. The legacy raw `execute()` boundary transfers ownership immediately:
accepted tasks move through queue and running scopes, while stopped/rejected
and queued-at-stop tasks are destroyed automatically; stop/join is idempotent.
The thread regression uses a real worker and condition-variable monitor to
verify running-task, queued-stop, repeated-stop, post-stop and monitor/thread
teardown independently.

`LVFontCache` owns every registered definition and live instance in vectors of
`unique_ptr<LVFontCacheItem>` and cannot be copied. New candidates enter scoped
ownership before vector publication, so allocation failure cannot strand a raw
entry. Lookup APIs continue to return borrowed item pointers, while
`getInstances()` exposes only a const borrowed collection view to FreeType
settings updates. Typeface and document removal use erase/remove and therefore
destroy every matching owner without skipping adjacent entries; `clear()` and
garbage collection release entries through the same container ownership. The
native regression covers document-scoped removal, adjacent same-typeface
removal, surviving borrowed views and idempotent empty teardown.

`LVEmbeddedFontList` owns definitions in a vector of `unique_ptr` and exposes
only borrowed `get()`/lookup pointers. The legacy raw `add()` boundary adopts
its argument immediately, while internal construction keeps every candidate
scoped until vector publication. The copy constructor clones every owner,
while `set()` builds its deduplicated replacement completely before swap.
Deserialization retains the existing append contract and wire format, but
each definition stages its fields until complete decoding, the list count is
bounded by the minimum wire size, and all incoming definitions enter a
temporary owner-list before the destination reserves and moves any item.
Malformed input therefore cannot publish a valid prefix or partially replace a
definition. The native regression covers independent copy/set owners, a
successful round trip, append compatibility, oversized counts and
truncated-input rollback at both levels.

`LVTextLineQueue` owns decoded `LVTextFileLine` objects in a private vector of
`unique_ptr`; indexed access returns only borrowed line views. Its source
`LVTextFileBase` is a non-null borrowed reference bounded by the stack-local
queue in `LVTextParser::Parse()`. Each newly decoded line enters scoped
ownership before alignment detection and vector publication, while head-range
erasure destroys removed owners automatically and preserves the file-relative
index of retained lines. The native regression checks append/removal/index
invariants directly and parses a 2400-line document across repeated queue
batches.

The process-wide `CRLog` instance is owned by a function-local `unique_ptr`;
the legacy static pointer is only a borrowed compatibility view. Logger
replacement adopts its candidate before releasing the previous owner, treats
publication of the active pointer as an idempotent operation, and clears the
borrowed view from the base destructor. Lifecycle, level access and message
dispatch share a function-local recursive mutex. Recursion is required because
the file logger emits its final message from its destructor while replacement
still holds the lifecycle lock; function-local initialization order also keeps
that mutex alive through owner teardown at process exit. The thread regression
serializes six concurrent writers and verifies exact destruction counts for
same-pointer publication, replacement and repeated clear.

`CRGUIWindowManager` keeps an optional exclusive `unique_ptr` screen owner and
exposes the legacy raw `_screen` only as a borrowed compatibility view. The
owner is declared before windows and events, so dependent GUI state is
destroyed first. Protected owned/borrowed transition helpers replace the
manual ownership flag, and copying the manager is disabled. Qt, Win32, Jinke,
XCB and PocketBook managers adopt every screen they create; NanoX does the
same for a new screen but explicitly borrows an already existing Jinke
singleton. The native lifecycle regression verifies borrowed teardown, owned
replacement, owned-to-borrowed transition and final destruction.

The GUI window stack and event queue are vectors of exclusive `unique_ptr`
owners. The legacy raw `activateWindow()` and `postEvent()` arguments are
adopted before publication; stack reordering moves an existing owner, closing
keeps the window alive through its lifecycle callback, and duplicate event
removal releases the replaced owner automatically. Dispatch holds each event
in a scoped owner, while `getEvent()` remains an explicit transfer boundary for
legacy callers. Manager teardown releases queued events and every remaining
window even when the process font manager is already unavailable.
`CRDocViewWindow` likewise owns its `LVDocView` directly and exposes only a
borrowed getter. Native lifecycle coverage verifies reactivation without
duplication, managed and unmanaged close paths, queued/replaced/transferred
events, borrowed-screen preservation and document-view teardown.

Render-flow shifts and floats enter scoped candidates before publication in
their owning vectors; removal and final teardown are delegated to those
containers instead of paired `remove()`/`delete` loops. Single-column table
page contexts are owned by their rows, while private cell contexts and
draw-time bookmark range filters are operation-scoped `unique_ptr` values.
The rendered-document regression exercises a floating single-column table,
pagination and an actual highlighted-page draw twice; the source policy keeps
all three ownership boundaries from returning to manual teardown.

`CCRTable` owns every row group, row, column and cell through its owning
pointer vectors; the cross-links from groups to rows and from cells to rows
and columns are borrowed views. A common publication helper keeps each new
graph node in a `unique_ptr` until the destination has reserved its slot, and
grid trimming uses owning-container erasure. MathML table expansion uses the
same boundary for generated rows/cells and temporarily owns cells while
moving them between rows. The rendered-document fixture verifies both the
floating table path and a nested `msubsup` table graph under normal and
sanitized execution.

`LVFreeTypeFontManager` stores language-compatibility tables as shared owner
values because the legacy hash-table contract copies mapped values. Table
construction begins in `make_shared`, cache replacement/clear releases owners
automatically, and manager teardown clears them before closing FreeType.
Each `LVFreeTypeFace` load candidate is exclusively owned until a successful
load transfers it into the intrusive `LVFontRef`; failed loads now unwind
without a manual delete. The rendered lifecycle queries the same language
twice to cover population, cached lookup and manager teardown.

Selection-range splitting copies its input before mutation, constructs the
complete replacement set in a vector of `unique_ptr`, and reserves the final
owning-list capacity before erasing or publishing anything. This also keeps a
self-aliased split argument valid throughout the operation. Draw-range
conversion builds a complete candidate owner list and swaps it into place only
after successful geometry conversion, while text-range fragments enter their
owning list from scoped candidates. The rendered-document regression validates
the exact five segments of three overlapping selections, repeated isolated
draw-owner replacement and all six marked/unmarked text fragments.

Range-derived owner-list factories publish in complete batches. Word-to-range
conversion, filtered range clones, cropped draw ranges and extended selectable
words first construct every item in `vector<unique_ptr>`, then check the final
list size, reserve it once and release the batch without further allocation.
The append contracts used for a two-page word selector remain intact. Rendered
coverage verifies independent clones, stable existing owners, translated crop
coordinates and repeated extended-word publication.

TOC and page-map nodes enter their owning child vectors through explicit
`unique_ptr` transfer boundaries. Deserialization reads scalar state and a
complete temporary owner graph before swapping it into the live object, so a
repeated load replaces rather than appends and truncated input preserves the
previous graph. TOC recursion is capped by the parser depth budget, and both
child counts are bounded by the minimum serialized item size before reserve.
Native coverage exercises nested parent links, independent replacement,
truncated-input rollback and oversized-count rejection for both graph types.

The complete DOM map cache block is also transactional. Element, attribute and
namespace name maps, hashed attribute values, next-ID counters and the
ID-to-node index all deserialize into independent candidates. Serialized entry
counts are bounded by the remaining bytes, duplicate ID keys are rejected, and
the live snapshot changes only after both nested and outer CRCs succeed.
ID-to-node serialization uses a typed vector and deterministic `std::sort`
instead of a manual scratch array. Native regressions verify byte-stable output,
successful whole-snapshot replacement, retained ID-node lookup, corrupted-input
rollback and oversized-count rejection.

The legacy pre-20200824 HTML autoclose table is an array of owned rule vectors.
Construction stages each rule in a local vector and swaps duplicate tag entries,
while modern DOM versions simply retain an empty table with automatic teardown.
The legacy parser regression processes two malformed HTML lifecycles and verifies
that unclosed paragraphs and list items still become the same sibling nodes.

FreeType glyph metrics use 360 lazily allocated pages owned by a bounded array
of `unique_ptr<array<...>>`. Each page is fully initialized in a scoped
candidate before publication; `clear()` and cache destruction release pages
without manual array teardown. Direct unsigned and signed cache coverage checks
page boundaries, repeated clear, the last supported codepoint, and verifies
that unsupported high codepoints cannot alias a lower page.

The shared glyph bitmap cache retains its compact single-allocation item ABI,
but every malloc-backed candidate now starts in a `unique_ptr` with a matching
deleter. FreeType and bold-transform producers fill that scoped candidate and
move it across the local-cache publication boundary. The global LRU owns every
published item in one owner map; its intrusive LRU links and the local hash or
list indices are borrowed views. Eviction first detaches those views and then
erases the sole owner, while local clear delegates destruction to the same
owner set. Capacity accounting records the exact allocated bitmap byte count
instead of deriving it from signed pitch and logical height. Native coverage
includes negative-pitch accounting, candidate rollback, LRU eviction, repeated
clear and reuse under sanitizers.

Fixed-size FreeType color glyphs keep the temporary output from smooth scaling
in an exclusive owner with the scaler's platform-specific deleter:
`_aligned_free` for MinGW and `free` elsewhere. The glyph slot borrows that
buffer only while copying the smaller BGRA image back into its existing
storage. Native coverage repeats a synthetic 4-by-4 to 2-by-2 scale and checks
both the copied solid-color pixels and every adjusted metric under sanitizers.

The legacy bitmap-font loader treats its file and complete malloc-backed image
as scoped candidates. It reads bytes with a byte-count contract, validates the
header and performs any endian fixups before publishing the handle through the
C API; every failure leaves the output null. The matching `lvfontClose()` call
remains the documented caller-side transfer boundary. Native coverage opens
the same minimal valid LFNT image twice and rejects a corrupt-header candidate
without publishing or leaking it.

The Win32 glyph cache keeps its hash buckets and bounded three-entry chains as
`unique_ptr` owner links; lookups return borrowed entries whose addresses stay
stable while unrelated entries are inserted. Each entry owns decoded glyph
bytes in a vector, and the GDI packed bitmap remains a scoped vector until a
complete decode moves into that entry. Third-entry eviction releases through
the owner link before a scoped candidate becomes the new head. A portable
native regression exercises this WinAPI-independent graph directly, including
surviving borrows, bounded eviction, repeated clear and reuse.

`ldomXPointer` stores its shared mutable state in `shared_ptr<XPointerData>`.
Ordinary copies intentionally retain alias semantics, while `clear()` publishes
a fresh null state and leaves aliases alive. The protected clone boundary,
`clone()` and `ldomXPointerEx` copies create independent state before replacing
their current owner. Native coverage preserves shared copy/assignment,
clear-detach, alias teardown and extended-pointer deep-copy behavior.

Computed CSS styles own their transient `::before` and `::after` accumulator
styles with `unique_ptr`. Selector application borrows those scoped owners;
`setNodeStyle()` records whether generated nodes are required and resets both
temporary slots before publishing the computed style to the shared cache.
Rendered CSS coverage verifies one before/after node, clean cached styles and
stable ownership across a repeated render.

Mutable DOM elements store attributes in `vector<lxmlAttribute>`. Lookup and
replacement borrow entries from that owner, while append stages a complete
attribute before vector publication; collection teardown no longer pairs
`realloc` with `free`. Document coverage verifies parsed values, replacement
without duplication, append, and a mutable-to-persistent-to-mutable round trip.

`ldomDocumentWriter` centrally owns every live `ldomElementWriter` frame in a
`vector<unique_ptr<...>>`; current, parent, last-paragraph and foster-parent
links are non-owning views. Normal pop paths erase through that owner set, and
EOF/destruction additionally drains frames temporarily detached from the
current chain by HTML foster parenting; filter shutdown then clears its foster
and last-paragraph borrows. A modern malformed-HTML regression leaves both
branches open at EOF and verifies finalization, foster order and repeatable
teardown under sanitizers.

DOM-backed base64 streams enter `LVStreamRef` ownership at construction.
The stream borrows its source element only for the document-bounded read
lifecycle; a zero-length decode returns an empty reference and releases the
candidate automatically, while a successful candidate is returned through the
same intrusive owner. Document coverage reads the decoded payload and repeats
both successful publication and empty-candidate rollback across two documents.

Extended DOM serialization keeps its per-text-node hyphenation flags in a
scoped vector. The selected `HyphMethod` borrows only the active word slice;
soft-hyphen emission reads the same container, and every exit releases it
automatically. Document coverage exercises algorithmic hyphenation repeatedly
on two equivalent DOMs and verifies that removing emitted soft hyphens restores
the exact ordinary serialization.
