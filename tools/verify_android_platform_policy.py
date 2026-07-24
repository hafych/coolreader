#!/usr/bin/env python3
"""Enforce the Android 14-16 service, receiver and notification contract."""

from __future__ import annotations

import re
import xml.etree.ElementTree as ET
from pathlib import Path


ROOT = Path(__file__).resolve().parents[1]
ANDROID = ROOT / "android"
SOURCE = ANDROID / "src" / "org" / "coolreader"
MANIFESTS = (
    ANDROID / "AndroidManifest.xml",
    ANDROID / "app" / "src" / "main" / "AndroidManifest.xml",
)
APP_GRADLE = ANDROID / "app" / "build.gradle"
COOL_READER = SOURCE / "CoolReader.java"
TTS_SERVICE = SOURCE / "tts" / "TTSControlService.java"
TTS_TOOLBAR = SOURCE / "crengine" / "TTSToolbarDlg.java"
ANDROID_NS = "http://schemas.android.com/apk/res/android"
ANDROID_NAME = f"{{{ANDROID_NS}}}name"
ANDROID_EXPORTED = f"{{{ANDROID_NS}}}exported"
ANDROID_FGS_TYPE = f"{{{ANDROID_NS}}}foregroundServiceType"
REQUIRED_FGS_PERMISSIONS = {
    "android.permission.FOREGROUND_SERVICE",
    "android.permission.FOREGROUND_SERVICE_MEDIA_PLAYBACK",
}


def relative(path: Path) -> str:
    return str(path.relative_to(ROOT))


def require_marker(
        path: Path,
        text: str,
        marker: str,
        reason: str,
        violations: list[str]) -> None:
    if marker not in text:
        violations.append(f"{relative(path)}: {reason}: {marker}")


def main() -> None:
    violations: list[str] = []

    gradle_text = APP_GRADLE.read_text(encoding="utf-8")
    for setting in ("compileSdkVersion", "targetSdkVersion"):
        match = re.search(rf"\b{setting}\s+(\d+)", gradle_text)
        if match is None or int(match.group(1)) < 35:
            violations.append(
                f"{relative(APP_GRADLE)} must keep {setting} at API 35 or newer"
            )

    for manifest in MANIFESTS:
        root = ET.parse(manifest).getroot()
        permissions = {
            element.attrib[ANDROID_NAME]
            for element in root.findall("uses-permission")
        }
        missing = sorted(REQUIRED_FGS_PERMISSIONS - permissions)
        if missing:
            violations.append(
                f"{relative(manifest)} misses FGS permissions: {missing}"
            )

        application = root.find("application")
        if application is None:
            violations.append(f"{relative(manifest)} has no application")
            continue

        tts_service = None
        for service in application.findall("service"):
            name = service.attrib.get(ANDROID_NAME, "")
            if service.attrib.get(ANDROID_EXPORTED) != "false":
                violations.append(
                    f"{relative(manifest)} service {name} must be "
                    "explicitly non-exported"
                )
            if name.endswith("TTSControlService"):
                tts_service = service

        if tts_service is None:
            violations.append(
                f"{relative(manifest)} does not declare TTSControlService"
            )
        elif tts_service.attrib.get(ANDROID_FGS_TYPE) != "mediaPlayback":
            violations.append(
                f"{relative(manifest)} must declare TTS as mediaPlayback"
            )

        for action in root.findall(".//action"):
            if action.attrib.get(ANDROID_NAME) == "android.intent.action.BOOT_COMPLETED":
                violations.append(
                    f"{relative(manifest)} must not start TTS from boot"
                )

    for path in sorted(SOURCE.rglob("*.java")):
        text = path.read_text(encoding="utf-8")
        for match in re.finditer(r"\bregisterReceiver\s*\(", text):
            line = text.count("\n", 0, match.start()) + 1
            prefix = text[max(0, match.start() - 32):match.start()]
            if not prefix.endswith("ContextCompat."):
                violations.append(
                    f"{relative(path)}:{line}: receiver registration must "
                    "use ContextCompat with an explicit export policy"
                )
                continue
            call_end = text.find(");", match.end())
            call = text[match.start():call_end + 2]
            if (call_end < 0
                    or "ContextCompat.RECEIVER_NOT_EXPORTED" not in call):
                violations.append(
                    f"{relative(path)}:{line}: dynamic receiver must be "
                    "non-exported"
                )

        for match in re.finditer(r"\bstartForegroundService\s*\(", text):
            if path != TTS_TOOLBAR:
                line = text.count("\n", 0, match.start()) + 1
                violations.append(
                    f"{relative(path)}:{line}: foreground service start is "
                    "outside the visible TTS UI flow"
                )

    cool_reader_text = COOL_READER.read_text(encoding="utf-8")
    require_marker(
        COOL_READER, cool_reader_text,
        "ContextCompat.RECEIVER_NOT_EXPORTED",
        "activity receivers lack a non-exported policy", violations)

    toolbar_text = TTS_TOOLBAR.read_text(encoding="utf-8")
    for marker in (
        "mWindow.showAtLocation(",
        "coolReader.startForegroundService(intent)",
        "TTSControlService.TTS_CONTROL_ACTION_PREPARE",
    ):
        require_marker(
            TTS_TOOLBAR, toolbar_text, marker,
            "visible user-initiated TTS start marker is missing", violations)

    instrumentation_test = (
        ANDROID / "app" / "src" / "androidTest" / "java" / "org"
        / "coolreader" / "AndroidSmokeInstrumentedTest.java"
    )
    instrumentation_text = instrumentation_test.read_text(encoding="utf-8")
    require_marker(
        instrumentation_test, instrumentation_text,
        "activity.moveTaskToBack(true)",
        "TTS controls are not exercised after backgrounding the task",
        violations)

    tts_text = TTS_SERVICE.read_text(encoding="utf-8")
    for marker in (
        "ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK",
        "PendingIntent.FLAG_IMMUTABLE",
        "Notification.MediaStyle()",
        "setMediaSession(mMediaSession.getSessionToken())",
        "new Intent(action).setPackage(getPackageName())",
        "return START_NOT_STICKY;",
    ):
        require_marker(
            TTS_SERVICE, tts_text, marker,
            "TTS foreground notification contract is incomplete", violations)
    if "return START_STICKY;" in tts_text:
        violations.append(
            f"{relative(TTS_SERVICE)} may resurrect TTS without user action"
        )

    command = "python3 tools/verify_android_platform_policy.py"
    for workflow in (
        ROOT / ".github" / "workflows" / "build.yml",
        ROOT / ".github" / "workflows" / "release.yml",
    ):
        if command not in workflow.read_text(encoding="utf-8"):
            violations.append(
                f"{relative(workflow)} does not run the Android platform gate"
            )

    if violations:
        raise RuntimeError(
            "Android platform policy violations found:\n"
            + "\n".join(violations)
        )

    print(
        "Android platform policy OK: typed user-initiated TTS FGS, "
        "media notification, and non-exported receivers"
    )


if __name__ == "__main__":
    main()
