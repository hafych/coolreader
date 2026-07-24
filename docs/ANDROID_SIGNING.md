# Android release signing

The repository contains release-signing wiring, but it intentionally contains
no production key or password. Finalize the permanent app name and
`applicationId` before creating the owner-controlled key.

## Create and store the production key

Create the keystore outside every source checkout. Let `keytool` prompt for the
passwords instead of placing them in shell history:

```sh
keytool -genkeypair -v \
  -keystore /absolute/private/path/coolreader-release.jks \
  -alias coolreader-release \
  -keyalg RSA -keysize 4096 -validity 10000
```

Record the certificate fingerprint separately:

```sh
keytool -list -v \
  -keystore /absolute/private/path/coolreader-release.jks \
  -alias coolreader-release
```

Keep two encrypted backups in separate owner-controlled locations. Record who
can retrieve them, how recovery is approved and when a restore was last tested.
Do not store the keystore, passwords, recovery material or exported private key
in Git, CI artifacts, issue trackers or chat.

For Google Play, decide explicitly whether this file is the upload key used
with Play App Signing or an app-signing key for another distribution channel.
The recovery and rotation paths differ; follow the current
[official Android signing guidance](https://developer.android.com/studio/publish/app-signing).

## Configure a local or CI build

For local use, copy `android/release-signing.properties.example` to
`android/release-signing.properties`, restrict it to the current user and fill
in the four values. The keystore path must be absolute and outside the
repository.

CI should inject these environment variables from its protected secret store:

- `COOLREADER_RELEASE_STORE_FILE`
- `COOLREADER_RELEASE_STORE_PASSWORD`
- `COOLREADER_RELEASE_KEY_ALIAS`
- `COOLREADER_RELEASE_KEY_PASSWORD`

The equivalent Gradle `-P` properties are:

- `coolreader.release.storeFile`
- `coolreader.release.storePassword`
- `coolreader.release.keyAlias`
- `coolreader.release.keyPassword`

Build the signed bundle with:

```sh
cd android
./gradlew :app:bundleSignedRelease
jarsigner -verify app/build/outputs/bundle/release/app-release.aab
```

`bundleSignedRelease` fails when any value is missing, the keystore does not
exist, its path is relative or it resolves inside the repository.
Android application certificates are self-signed, so `jarsigner -strict`
incorrectly treats their lack of a public PKIX trust chain as an error; use its
ordinary integrity verification here.

## Installation and recovery checks

An AAB is a publishing artifact, not an installable APK. Before release, use
`bundletool` to generate and install an APK set on a clean supported device, as
described in the
[official app-bundle testing guide](https://developer.android.com/guide/app-bundle/test).
Verify an update over the previous signed version as well as a clean install.

Before the first upload:

1. Restore the keystore from each backup into an isolated temporary location.
2. Confirm its SHA-256 certificate fingerprint against the owner record.
3. Build the same release from the restored copy.
4. Confirm that the generated APK set installs and can update the prior build.
5. Remove the temporary restored copy.

If an upload key is lost or compromised, use the Play Console reset procedure;
do not silently create a replacement and assume existing installs will accept
it. App-signing key changes require a separately reviewed rollout and
compatibility plan.
