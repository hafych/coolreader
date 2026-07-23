# Changelog

This file records user-visible changes in the downstream CoolReader Next fork.
The historical Debian changelog remains in [`changelog`](changelog).

## Unreleased

### Added

- Linux, macOS and Android build verification, native smoke tests and
  ASan/UBSan CI.
- Release AAB and native debug-symbol generation in Android CI.
- Stable, checksum-equivalent zlib release retrieval from the official GitHub
  release asset.
- Storage Access Framework loading through file descriptors with a bounded
  private-cache fallback for non-seekable providers.
- Unit and native regression tests for XML, OPDS, stream and hostile document
  resource limits.
- Weekly read-only compatibility check against `buggins/coolreader`.

### Changed

- Android baseline is API 21+, compile/target API 35, JDK 17 and the pinned NDK
  declared in the Gradle build.
- Notification actions are immutable, package-scoped and registered as
  non-exported.
- Temporary document cache is capped at 512 MiB and 32 files with
  deterministic oldest-first eviction.
- Database schema 35 rejects unknown future schemas and repairs early
  downstream schema 34 installations.
- Database schema 36 preserves OPDS catalogs while physically removing legacy
  plaintext username and password columns; the catalog editor no longer
  collects credentials that cannot be stored safely.
- Database upgrades now create a SHA-256-verified, `fsync`ed backup through a
  same-directory temporary file, retain four generations and restore without
  deleting the current database before the replacement is ready.
- Main and cover database upgrades now run as named transactional steps with
  mandatory schema and data postconditions instead of suppressing migration
  errors.

### Security

- Restored platform TLS and hostname validation; LitRes uses HTTPS.
- Hardened redirect handling, XML parsing, OPDS response limits, ZIP/image
  limits and sensitive URL logging. ZIP archives now also enforce entry-count,
  aggregate-size and path-depth limits and reject traversal or duplicate names.
- Purged legacy plaintext OPDS/LitRes credentials and stopped accepting new
  plaintext credential persistence.
- Disabled Android cloud backup and device transfer until a safe explicit
  export model is implemented.
- Removed phone-state permission and the exported telephony receiver; TTS
  interruption is handled through Android Audio Focus.
