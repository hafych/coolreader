# Reproducible release builds

The release workflow builds Android, Linux and macOS twice in separate,
ephemeral runners. A candidate set is accepted only when every file from the
first build is byte-identical to the corresponding file from the second build,
with the single cryptographically required exception below.

## Normalized inputs

- Both builds use the same reviewed commit, release version, protected Android
  signing key and pinned GitHub runner image.
- `SOURCE_DATE_EPOCH` is the reviewed commit time.
- Desktop archive paths, owners, modes, ordering and timestamps are normalized.
- The Android dependency SBOM uses `SOURCE_DATE_EPOCH` for SPDX creation time.
- The native-symbol ZIP drops source timestamps and uses reproducible ordering.

The gate compares signed AAB/APK files, native symbols, signing evidence, SPDX
JSON and the Linux/macOS archives. The first complete set is then checksummed
and validated before it can become a draft release.

## Allowed differences

No differences are allowed inside canonical AAB, symbol, SPDX or desktop
files. A signed APK may have different RSA-PSS entropy in its v2 APK Signing
Block. For that case the comparator validates the block framing, removes only
that block, restores the original ZIP central-directory offset and requires all
remaining APK bytes to be identical. Both APKs must also independently pass
`apksigner` verification and produce identical signer evidence.

Runner logs, temporary paths, Gradle/CMake intermediates, GitHub
artifact-container metadata, workflow IDs and upload timestamps are evidence
rather than release artifacts and are outside the byte comparison.

Apple Developer ID signing and notarization are not configured. If they are
added, their tickets and timestamped signatures must be verified separately
and the unsigned deterministic archive must remain available as provenance.
Toolchain or dependency updates may intentionally change artifacts between
different commits; that is not a failure when each commit is internally
reproducible and the change is reviewed.

## Local check

`tools/compare_release_builds.py` accepts two flat artifact directories:

```sh
python3 tools/compare_release_builds.py \
  --left /path/to/first \
  --right /path/to/second \
  --label android
```

Use clean build directories and isolated dependency caches when producing the
two inputs. A mismatch is a release blocker; do not bless a filename-specific
exception without first identifying and documenting the nondeterministic
producer.
