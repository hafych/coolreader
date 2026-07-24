# Privacy policy draft

Status: **not publishable yet**. Before release, replace
`[DEVELOPER LEGAL NAME]`, `[PRIVACY CONTACT EMAIL]` and
`[PUBLIC POLICY URL]`, obtain owner/legal approval, publish the final text at
the public URL, add an in-app link, and make the same URL available in Google
Play.

Effective date: 24 July 2026 (draft)

This privacy policy describes how `[DEVELOPER LEGAL NAME]` handles information
in the Android application currently identified as CoolReader Next
("the app").

## Summary

The app is an ebook reader for files selected by the user. It has no
advertising, analytics, telemetry or third-party crash-reporting SDK. Reading
data is kept on the device by default. Network access occurs when the user opens
an online catalog, downloads content, or uses the optional consumption-only
LitRes integration.

## Information stored on the device

The app stores its settings, library index, book metadata, reading history,
reading positions, bookmarks, cached covers and temporary document imports in
app-private storage. It also stores references to folders selected through
Android's Storage Access Framework and the associated persisted access grants.
Source books remain in the location chosen by the user.

Android backup and device-to-device transfer are disabled for app-private data.
Legacy LitRes and OPDS credentials written by older builds are removed. The
current app does not persist LitRes passwords or sessions; they remain in
process memory only for the active app process.

## Network features and third parties

The app uses the network only for features initiated by the user:

- OPDS: the app sends requests to the catalog operator selected by the user and
  downloads catalog metadata, covers or books returned by that operator. A
  catalog operator receives normal network information such as the user's IP
  address and request metadata under its own privacy terms.
- LitRes: if the user chooses to sign in, the app sends the supplied identifier
  and password to LitRes over HTTPS. It receives a short-lived session and may
  browse the catalog, download trials, or download books already owned by that
  user. The app does not create LitRes accounts, refill balances or offer
  purchases. LitRes processes this information under its own privacy terms.
- External apps: when the user explicitly invokes a dictionary, translation,
  web search, email or share action, Android sends the selected word, text or
  diagnostic export to the app chosen by the user. The receiving app's policy
  applies after that transfer.

The app developer does not operate an application backend for these features
and does not receive their contents.

## Android permissions

- Internet: online catalogs, user-requested downloads and optional LitRes
  access.
- Wake lock: keep an active reading or text-to-speech operation from being
  interrupted unexpectedly.
- Foreground service and media playback: expose user-visible text-to-speech
  playback controls.
- Storage Access Framework grants: access only documents or folders selected by
  the user; the app does not request broad shared-storage permission.

## Retention and deletion

Settings, history, bookmarks, indexes and caches remain until the user removes
them through app controls, clears the app's Android storage, or uninstalls the
app. Removing a library root from the app removes the saved grant/reference but
does not delete source books. Clearing app storage or uninstalling removes the
app-private data and stored grants.

Data held by LitRes, an OPDS operator, or a receiving external app is controlled
by that service. Requests concerning an external account must be sent to that
operator. The app does not create accounts of its own.

## Children

The app is a general-purpose reader and is not directed specifically to
children. The release owner must complete the Google Play target-audience and
content-rating declarations before publication.

## Changes

Material changes to this policy will be published at `[PUBLIC POLICY URL]` with
an updated effective date.

## Contact

Privacy questions can be sent to `[PRIVACY CONTACT EMAIL]`.

Google Play requires the final policy to identify the developer, accurately
describe data access/collection/use/sharing and retention/deletion, be publicly
accessible, and also be linked inside the app:
<https://support.google.com/googleplay/android-developer/answer/10144311>.
