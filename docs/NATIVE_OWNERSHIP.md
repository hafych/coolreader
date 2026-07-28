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

Cache-file serialization buffers, temporary draw mark lists and SVG decoder
input/output buffers also use standard RAII containers. These operation-scoped
resources cannot outlive their call and no longer rely on matching cleanup at
each return.

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
archives whose header omits the checksum. `LVCachedStream` owns cache slots as
`std::unique_ptr<BufItem>` values inside a vector while its LRU links remain
non-owning views. Slot eviction transfers ownership instead of deleting and
reallocating nodes.

The TCR decoder owns dictionary entries, its packed-block index and the
reusable decoded block through `std::vector`. Its factory keeps a candidate
decoder in `std::unique_ptr` until ownership crosses into `LVStreamRef`.
Regression coverage expands a decoded block beyond its initial reserve, seeks
across packed-block boundaries and rejects a truncated dictionary after
partially initialized entries have been released automatically.

`SerialBuf` owns writable fixed-size and auto-growing storage through
`std::vector`; its raw `_buf` member is only a compatibility view. The
deserialization constructor remains explicitly borrowed, while the legacy
`set(lUInt8 *, int)` boundary takes a malloc-allocated buffer into a scoped
owner, copies it into container storage and releases it automatically. Shallow
copying is disabled so the view cannot detach from its owner. Regression
coverage exercises CRC round-tripping, autogrowth, legacy adoption, swapping
and fixed/borrowed overflow rejection.
