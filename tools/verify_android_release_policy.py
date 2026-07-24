#!/usr/bin/env python3
"""Enforce the Android release permission and monetization baseline."""

from __future__ import annotations

import re
import xml.etree.ElementTree as ET
from pathlib import Path


ROOT = Path(__file__).resolve().parents[1]
ANDROID = ROOT / "android"
MANIFESTS = (
    ANDROID / "AndroidManifest.xml",
    ANDROID / "app" / "src" / "main" / "AndroidManifest.xml",
)
ANDROID_NAME = "{http://schemas.android.com/apk/res/android}name"
EXPECTED_PERMISSIONS = {
    "android.permission.FOREGROUND_SERVICE",
    "android.permission.FOREGROUND_SERVICE_MEDIA_PLAYBACK",
    "android.permission.INTERNET",
    "android.permission.WAKE_LOCK",
}
REMOVED_BILLING_PATHS = (
    ANDROID / "src" / "com" / "android" / "vending" / "billing"
    / "IInAppBillingService.aidl",
    ANDROID / "src" / "org" / "coolreader" / "donations"
    / "CRDonationService.java",
    ANDROID / "res" / "layout" / "about_dialog_donation.xml",
    ANDROID / "res" / "layout" / "about_dialog_donation2.xml",
    ANDROID / "res" / "drawable" / "ic_menu_emoticons.png",
)
FORBIDDEN_LIVE_SOURCE = (
    "com.android.vending.BILLING",
    "IInAppBillingService",
    "CRDonationService",
    "makeDonation(",
    "isDonationSupported(",
    "dlg_about_donation",
    "mi_donation",
)


def relative(path: Path) -> str:
    return str(path.relative_to(ROOT))


def main() -> None:
    violations: list[str] = []

    for manifest in MANIFESTS:
        root = ET.parse(manifest).getroot()
        permissions = {
            element.attrib[ANDROID_NAME]
            for element in root.findall("uses-permission")
        }
        if permissions != EXPECTED_PERMISSIONS:
            missing = sorted(EXPECTED_PERMISSIONS - permissions)
            extra = sorted(permissions - EXPECTED_PERMISSIONS)
            violations.append(
                f"{relative(manifest)} permission drift; "
                f"missing={missing}, extra={extra}"
            )

    for path in REMOVED_BILLING_PATHS:
        if path.exists():
            violations.append(f"obsolete billing surface exists: {relative(path)}")

    live_files = [
        *sorted((ANDROID / "src").rglob("*.java")),
        *sorted((ANDROID / "src").rglob("*.aidl")),
        *sorted((ANDROID / "res").rglob("*.xml")),
        *MANIFESTS,
        ANDROID / "app" / "build.gradle",
    ]
    for path in live_files:
        text = path.read_text(encoding="utf-8")
        for token in FORBIDDEN_LIVE_SOURCE:
            match = re.search(re.escape(token), text)
            if match:
                line = text.count("\n", 0, match.start()) + 1
                violations.append(
                    f"{relative(path)}:{line}: obsolete billing token {token}"
                )

    workflow = (
        ROOT / ".github" / "workflows" / "build.yml"
    ).read_text(encoding="utf-8")
    command = "python3 tools/verify_android_release_policy.py"
    if command not in workflow:
        violations.append("GitHub Actions does not run the release policy gate")

    if violations:
        raise RuntimeError(
            "Android release policy violations found:\n"
            + "\n".join(violations)
        )

    print(
        "Android release policy OK: least privilege; "
        "no billing or purchase UI"
    )


if __name__ == "__main__":
    main()
