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
