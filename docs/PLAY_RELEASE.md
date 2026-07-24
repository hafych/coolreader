# Google Play test and rollout plan

Status: executable checklist, blocked on permanent identity, owner-held signing
key, Play Console access, final listing/privacy assets and an uploaded signed
AAB.

## Release prerequisites

- Approved name, application ID, publisher identity and asset rights.
- Owner-controlled release/upload key with tested recovery and access process.
- Signed AAB built by `:app:bundleSignedRelease`; certificate fingerprint,
  source commit, version code/name and checksums recorded.
- Listing assets, content rating, Data safety and published privacy policy
  reviewed against the exact artifact.
- Unit, lint, native, instrumentation and policy gates green.

## Internal test

1. Upload the signed AAB to the internal track.
2. Install from Google Play on at least API 21 and API 35 devices.
3. Verify clean install, upgrade from the last supported build and reinstall.
4. Exercise startup, system document picker, persisted SAF root, EPUB/FB2/ZIP
   open, bookmarks/position restore, search, TTS notification actions, OPDS and
   optional LitRes login/trial/already-owned download.
5. Confirm there is no registration, purchase, balance-refill or broad-storage
   permission surface.
6. Check Play-generated split APKs, signing certificate and version identity.

## Closed test

Use a representative private tester group and keep a written tester roster.
For newly created personal Play developer accounts, verify whether Google
requires at least 12 opted-in testers continuously for 14 days before production
access. Do not shorten or simulate this requirement.

Collect only voluntary issue reports; do not add telemetry by default. Record
device/API, app version and reproduction steps without private book content.

## Pre-launch report

After each candidate upload, review:

- stability: startup failures, crashes, ANRs and native faults;
- compatibility: permission, storage, ABI and rendering failures;
- performance: startup/open latency and memory warnings;
- accessibility: labels, contrast, touch targets and navigation findings;
- security/privacy findings and manifest/dependency changes.

Every finding must be triaged as fixed, demonstrated false positive with
evidence, or an explicit release blocker. Re-run the report after a changed AAB.

## Production rollout

Google Play staged rollout applies to updates, not the first production release.
The first public release therefore requires the full internal/closed/pre-launch
gate before production.

For later updates:

| Stage | Minimum observation window | Promotion evidence |
| --- | --- | --- |
| 5% | 24 hours | No stop criterion; install/open/storage smoke confirmed |
| 20% | 24 hours | Crash/ANR and review signals remain within the prior baseline |
| 50% | 48 hours | No data-loss, compatibility or policy regression |
| 100% | — | Owner records approval and release evidence |

## Stop and rollback criteria

Halt rollout immediately for any:

- reproducible crash/ANR or meaningful regression from the previous release;
- startup, document-open or upgrade failure on a supported API/ABI;
- book, bookmark, position or database loss/corruption;
- lost SAF access not recoverable through the documented re-selection flow;
- unexpected permission, telemetry, purchase or privacy-policy mismatch;
- foreground TTS control failure that leaves playback unmanaged;
- signing, versioning or Play policy inconsistency.

Halting a rollout does not remove the update from users who already received it.
Prepare a higher-version hotfix from the last known-good source; never reuse a
version code or signing identity. Preserve the failed artifact and evidence.

Official references:

- Test tracks: <https://support.google.com/googleplay/android-developer/answer/9845334>
- New personal-account testing requirement:
  <https://support.google.com/googleplay/android-developer/answer/14151465>
- Pre-launch reports:
  <https://support.google.com/googleplay/android-developer/answer/9842757>
- Staged rollouts:
  <https://support.google.com/googleplay/android-developer/answer/6346149>
