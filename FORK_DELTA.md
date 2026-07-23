# CoolReader downstream fork delta

This file tracks deliberate differences from
[`buggins/coolreader`](https://github.com/buggins/coolreader). Update it in every
release and upstream-sync pull request.

## Upstream base

- Repository: `https://github.com/buggins/coolreader.git`
- Base commit: `109e2a22fa7b38c7e388155cca0fe1fbdbbf6144`
- Base version: `3.2.59`
- Last reviewed: 2026-07-23

Local remotes use `https://github.com/hafych/coolreader.git` as `origin` and the
original repository above as `upstream`.

## Active deltas

| Area | Classification | Reason | Removal/upstream condition |
| --- | --- | --- | --- |
| CMake C++17 repair and `fb2props` smoke test | upstreamable | Restores reproducible native configuration and testing | Remove after an equivalent upstream change lands |
| Android API 21 minimum, pinned NDK, least-privilege manifest, blocking lint and CI | fork-only for now | Establishes the downstream support and privacy baseline | Reclassify when upstream adopts the same support matrix and permission policy |
| Shared OPDS/LitRes HTTP policy, TLS and response limits | upstreamable | Enforces platform TLS, fixed timeouts/deadlines, explicit redirects, origin-scoped secrets and bounded decompressed responses with regression and source-policy tests | Keep until an audited upstream equivalent lands |
| Plaintext OPDS/LitRes credential purge | temporary-delta | Authenticated OPDS catalogs stay disabled until secure storage exists | Replace with a Keystore-backed credential reference and an explicit migration |
| SAF file-descriptor loading and bounded, evicting cache fallback | upstreamable | Avoids loading large `content://` documents into Java heap without unbounded disk growth | Remove after upstream accepts an equivalent implementation |
| Shared, fixture-tested database migrations and schema 36 compatibility repair | fork-only | Enforces migration postconditions, repairs early downstream schema 34 databases and physically removes legacy OPDS secret columns | Retain while legacy schemas can still be upgraded |
| Verified pre-migration database backups | upstreamable | Makes schema upgrades and recovery atomic while retaining bounded history | Remove after an equivalent upstream backup policy lands |
| Shared Java/native document `ParseBudget` | upstreamable | Applies stable safe-failure codes and common limits to ingress, XML/HTML, ZIP, decoded text, images and recursive containers | Replace only with an audited upstream budget of equal or stricter coverage |
| Native sanitizer CI and atomic thread completion | upstreamable | Runs ASan/UBSan/TSan smoke coverage, exercises the real thread abstraction and removes its completion-state data race | Remove after equivalent upstream tests and synchronization land |

## Upstream sync procedure

1. Fetch `upstream` without rebasing or rewriting shared history.
2. Merge upstream into a dedicated sync branch.
3. Reconcile every conflict against the active delta table above.
4. Run native CTest, Android assemble/lint/unit tests, and sanitizer CI.
5. Update the base commit and delta classifications in this file.
6. Merge the sync through a reviewed pull request.

The read-only compatibility workflow runs weekly. If upstream has not advanced,
no sync branch or pull request is created. When it advances, open one dedicated
sync pull request within 14 days; prioritize security fixes without waiting for
that window. Never mix product work into an upstream-sync pull request.

## OPDS credential lifecycle

- Upstream schemas through version 34 could store OPDS usernames and passwords
  as plaintext database columns.
- Schema 35 cleared every stored value and stopped writing new credentials.
- Schema 36 rebuilds `opds_catalog` inside the schema transaction, preserves
  catalog IDs, names, URLs and usage order, then verifies that the plaintext
  columns no longer exist.
- The OPDS catalog editor does not collect credentials while authenticated OPDS
  has no Keystore-backed reference model. Plaintext values are never restored
  from an older database or backup.
