# Android 14–16 platform compatibility

This document records the release contract and runtime evidence for Android 14
through Android 16. Static checks are necessary but do not replace the device
matrix below.

## Release contract

The production app currently targets API 35 and supports API 21 and newer.
`tools/verify_android_platform_policy.py` keeps these invariants:

- `TTSControlService` is non-exported and declares the `mediaPlayback`
  foreground-service type together with `FOREGROUND_SERVICE` and
  `FOREGROUND_SERVICE_MEDIA_PLAYBACK`.
- TTS starts only from the visible reader toolbar and immediately posts a
  `MediaStyle` notification backed by a `MediaSession`.
- TTS returns `START_NOT_STICKY`; Android must not resurrect it without another
  user-initiated prepare action.
- notification and media-control `PendingIntent` objects are immutable and
  control broadcasts are package-scoped;
- every dynamically registered production receiver uses
  `ContextCompat.RECEIVER_NOT_EXPORTED`;
- no service is exported and no `BOOT_COMPLETED` path starts foreground work.

The app does not request `POST_NOTIFICATIONS`. Android does not require this
permission to launch a foreground service, and media-session notifications are
exempt from the notification-permission behavior. TTS must nevertheless remain
visible in the system's foreground-service task manager when notification
permission is denied.

The active sync integration is disabled. `SyncService` is non-exported and is
not a foreground service; it must be redesigned against the current background
work rules before sync can be enabled.

References:

- [Foreground-service changes](https://developer.android.com/develop/background-work/services/fgs/changes)
- [Declare foreground services and request permissions](https://developer.android.com/develop/background-work/services/fgs/declare)
- [Restrictions on background foreground-service starts](https://developer.android.com/develop/background-work/services/fgs/restrictions-bg-start)
- [Notification runtime permission](https://developer.android.com/develop/ui/views/notifications/notification-permission)
- [Android 16 changes for all apps](https://developer.android.com/about/versions/16/behavior-changes-all)

## Runtime evidence

Run `tools/run_android_instrumentation.sh` against a clean emulator for each
row. The suite covers app startup, private-storage policy, ordinary and
archive-entry metadata, bounded multi-batch library scanning, provider-backed
document opening, persisted SAF recovery, TTS foreground notification actions,
and clean reinstall.

Evidence recorded on July 24, 2026:

| Platform | Profile | Evidence |
| --- | --- | --- |
| Android 14 / API 34 | `coolreader_api34_atd`, clean phone data | 7/7 instrumentation phases passed; background TTS controls passed |
| Android 15 / API 35 | `coolreader_api35_atd`, clean phone data | 9/9 instrumentation phases passed, including locator-free archive metadata and a three-level 133-book scan with entry/depth stops; background TTS controls passed |
| Android 16 / API 36 | `coolreader_api36_phone`, clean phone data | 7/7 instrumentation phases passed; background TTS controls passed |
| Android 16 / API 36 | `coolreader_api36_tablet`, 2560×1600 | cold start, background TTS controls and portrait/landscape visual checks passed |

The TTS test starts the service while the activity is in the `TOP` state,
confirms the media notification and its four actions, moves the task to the
background, exercises every action, stops the service, and verifies the
notification disappears. Android's ActivityManager recorded the start as
allowed from `PROC_STATE_TOP` on each API. The app does not declare
`POST_NOTIFICATIONS`, so these runs also exercise the media-session exemption.

The initial API 36 tablet check exposed a clipped first-run notice. The notice
body is now inset-aware and scrollable with fixed action buttons; cold-start
screenshots in both orientations confirmed that the full message and actions
remain reachable.

Use a distinct result directory when retaining more than one local run:

```bash
ANDROID_TEST_REPORT_PROFILE=api34 ./tools/run_android_instrumentation.sh
ANDROID_TEST_REPORT_PROFILE=api35 ./tools/run_android_instrumentation.sh
ANDROID_TEST_REPORT_PROFILE=api36-phone ./tools/run_android_instrumentation.sh
```

For release-candidate reruns, inspect `logcat` for
`ForegroundServiceStartNotAllowedException`,
`MissingForegroundServiceTypeException`, app-side `SecurityException`,
notification channel failures, leaked receivers, and background execution
errors. None were emitted by CoolReader in this matrix.

Raising `targetSdkVersion` to 36 remains a separate platform migration because
it enables additional predictive-back and large-screen behavior changes beyond
this foreground-service contract.
