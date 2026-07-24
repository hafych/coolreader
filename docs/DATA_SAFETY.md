# Google Play Data safety worksheet

Status: audited draft for the current Android source tree. The release owner
must validate these answers against the exact signed artifact and complete the
form in Play Console. This worksheet deliberately uses conservative disclosure
for optional LitRes authentication.

Audit date: 24 July 2026

## Artifact baseline

- Application ID today: `org.coolreader` (not approved as the permanent ID).
- Android permissions: Internet, wake lock, foreground service and foreground
  media playback.
- Dependencies: AndroidX AppCompat/Core and repository-built native libraries.
- No ad, analytics, telemetry, attribution or third-party crash-reporting SDK
  was found.
- Android backup/device transfer is disabled.
- LitRes is consumption-only: no registration, purchase, refill or direct store
  link exists in the app.

## Proposed Console answers

| Question/data type | Proposed answer | Reason and handling |
| --- | --- | --- |
| Does the app collect or share required user-data categories? | Yes, conservatively, because optional LitRes authentication transmits user-provided account data to LitRes | Validate the final form wording and any user-initiated-action exception in Console |
| Personal info — User IDs | Collected and shared; optional; app functionality; processed ephemerally by the app; encrypted in transit | The login may be an identifier or email address and is sent to LitRes only after the user signs in |
| Personal info — Email address | Declare if Play treats a LitRes email-style login as email rather than only a user ID | Do not claim the app can distinguish the identifier type |
| Personal info — Other info | Declare conservatively for the LitRes password/authentication secret if the current form presents a matching type | Password/session remain in memory only and are not persisted by the app |
| Files and documents | Not collected by the developer | Local processing stays on device; user-requested OPDS/LitRes downloads flow to the device |
| App activity, web browsing, diagnostics, device IDs, location, contacts, financial info | Not collected or shared by the developer | No relevant SDK/backend or application code was found |
| Data encrypted in transit | Yes for disclosed LitRes account data | LitRes authentication and account downloads use HTTPS with platform TLS validation |
| Account creation/deletion | The app does not create accounts | LitRes accounts are external; the app only signs in to an existing account |
| Ads | No | No ad SDK or advertising surface is present |

“Collected” in the Play form means transmitting data off the user's device;
on-device-only processing is generally outside that definition. User-initiated
sharing may qualify for an exception, but the final reviewer should disclose
LitRes authentication conservatively unless Play Console guidance clearly
applies the exception.

## Feature-by-feature audit

### Local reading and SAF

Book files, metadata, bookmarks, positions, history, search queries, settings,
SAF URIs and cached covers are processed locally. They are not sent to the
release owner. Clearing app data or uninstalling removes the app-private copy;
removing a SAF root does not delete the user's source files.

### OPDS

Opening an OPDS catalog sends a normal HTTP request to the user-selected catalog
operator. Catalog URLs and last-use metadata are stored locally. Authenticated
OPDS is disabled because credentials have no approved secure-storage model.
Public catalogs can observe ordinary connection metadata such as IP address.
The app developer does not receive it.

### LitRes

The user may optionally submit an identifier and password to LitRes over HTTPS.
The app keeps credentials and the short-lived session in process memory only.
LitRes can return account metadata and books already owned by that user. No
account creation, purchase or refill flow remains.

### Android intents

Dictionary, translation, search, email and share actions transfer selected
content only after an explicit user action to an Android app chosen or
configured by the user. The receiving app is a separate data controller.

## Final release verification

Before submission:

1. Generate the signed AAB and review the merged manifest and dependency graph.
2. Run `python3 tools/verify_play_release_docs.py` and all Android policy gates.
3. Confirm that no release-only SDK, remote configuration or telemetry was
   added.
4. Compare every Play answer with this worksheet and the published privacy
   policy.
5. Save a dated PDF/screenshot export of the submitted form with the release
   evidence.

Official definitions and form requirements:
<https://support.google.com/googleplay/android-developer/answer/10787469>.
