# Release, rollback and hotfix checklist

The tag workflow creates a **draft** GitHub Release only after every expected
artifact is present and checksummed. Publishing the draft, uploading to Play or
distributing desktop archives remains an explicit owner action.

## Before tagging

- [ ] Permanent name/application ID, publisher contacts and asset rights are
      approved in `IDENTITY_AND_ASSETS.md`.
- [ ] Owner signing key recovery drill and certificate fingerprint are current.
- [ ] `release-version.json` is incremented and
      `python3 tools/verify_release_version.py` passes.
- [ ] `CHANGELOG.md`, `FORK_DELTA.md`, privacy/Data safety and listing text match
      the exact release candidate.
- [ ] Full CI, instrumentation, license/SBOM and strict performance evidence are
      green.
- [ ] `telemetry-policy.json` remains disabled and the no-telemetry gate passes.
- [ ] Both clean-runner rebuilds pass `REPRODUCIBLE_BUILDS.md`; investigate any
      mismatch rather than adding an artifact-specific exception.
- [ ] Clean install, previous-version upgrade, SAF root, database migration,
      OPDS, consumption-only LitRes and TTS checks pass.
- [ ] Linux and macOS archives start on their declared clean reference systems.
      Record that the macOS archive is not notarized unless a separate
      owner-controlled signing/notarization step is added.

## Tag and draft artifacts

1. Create the reviewed tag `v<versionName>` on the reviewed commit and push only
   that tag.
2. The release workflow validates tag/version equality and protected signing
   secrets.
3. Confirm the draft contains exactly:
   - signed AAB and universal APK;
   - Android native debug symbols and signing-certificate evidence;
   - Android SPDX 2.3 SBOM;
   - Linux x86_64 and macOS arm64 desktop archives;
   - `SHA256SUMS`.
4. Independently recompute checksums after downloading the complete artifact.
5. Inspect generated release notes and keep the GitHub Release as a draft until
   the P0 Play/privacy/identity requirements are complete.

The CI secret store needs `COOLREADER_RELEASE_STORE_BASE64`,
`COOLREADER_RELEASE_STORE_PASSWORD`, `COOLREADER_RELEASE_KEY_ALIAS` and
`COOLREADER_RELEASE_KEY_PASSWORD`. Restrict them to the protected `release`
environment. The base64 value is transport encoding, not encryption.

## Android release

- [ ] Verify AAB/APK signer SHA-256 against the owner record.
- [ ] Generate an APK set with the current bundletool and test clean install and
      update from the previous production certificate/version.
- [ ] Complete internal/closed/pre-launch gates in `PLAY_RELEASE.md`.
- [ ] Confirm Play artifact version, permissions, Data safety, content rating,
      privacy URL and consumption-only behavior before production.

## Desktop release

- [ ] Verify archive root, executable, resources, translations, hyphenation
      data, license/notice files and clean-system startup.
- [ ] Record dynamically required system libraries for Linux.
- [ ] Verify macOS architecture, Gatekeeper behavior and any owner-applied
      signature/notarization.
- [ ] Publish source/notice obligations alongside binary archives.

## Rollback

Do not delete or overwrite an immutable published tag/artifact.

- Play: halt the rollout. Users already updated are not downgraded; prepare a
  higher-version hotfix from the last known-good source and the same signing
  lineage.
- GitHub/desktop: mark the release and affected assets as withdrawn, retain
  forensic evidence, and publish a higher-version replacement. Do not silently
  replace a file under the same filename/checksum.
- Preserve the failing artifact, logs, SBOM, symbols, reproduction steps and
  impact assessment. If privacy/data integrity is involved, stop distribution
  before normal bug triage.

## Hotfix

1. Branch from the last known-good release commit.
2. Apply only the minimal fix and its regression test.
3. Increment fork release, Android code and tag; update changelog/delta.
4. Run the full release gates—hotfix urgency does not waive signing, migration,
   privacy or artifact verification.
5. Publish clear impact/upgrade notes and monitor the rollout using the same
   stop criteria.
