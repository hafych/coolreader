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
| Android API 21 minimum, pinned NDK, blocking lint and CI | fork-only for now | Establishes the downstream support baseline | Reclassify when upstream adopts the same support matrix |
| OPDS/LitRes TLS, redirect, XML and response limits | upstreamable | Removes stop-ship network vulnerabilities | Keep until an audited upstream equivalent lands |
| Plaintext OPDS/LitRes credential purge | temporary-delta | Credentials require re-entry until secure storage exists | Replace with a Keystore-backed credential reference |
| SAF file-descriptor loading and bounded cache fallback | upstreamable | Avoids loading large `content://` documents into Java heap | Remove after upstream accepts an equivalent implementation |
| Database schema 35 compatibility repair | fork-only | Repairs early downstream schema 34 databases and purges secrets | Retain while schema 34 can still be upgraded |
| ZIP/image/document resource budgets | upstreamable | Bounds hostile input resource use | Replace only with stricter shared parse budgets |

## Upstream sync procedure

1. Fetch `upstream` without rebasing or rewriting shared history.
2. Merge upstream into a dedicated sync branch.
3. Reconcile every conflict against the active delta table above.
4. Run native CTest, Android assemble/lint/unit tests, and sanitizer CI.
5. Update the base commit and delta classifications in this file.
6. Merge the sync through a reviewed pull request.
