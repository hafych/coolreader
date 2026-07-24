#!/usr/bin/env python3
"""Enforce SAF-only shared storage and app-private persistence on Android."""

from __future__ import annotations

import re
from pathlib import Path


ROOT = Path(__file__).resolve().parents[1]
ANDROID_SOURCE = ROOT / "android" / "src"
MANIFESTS = (
    ROOT / "android" / "AndroidManifest.xml",
    ROOT / "android" / "app" / "src" / "main" / "AndroidManifest.xml",
)
FORBIDDEN_MANIFEST = (
    "READ_EXTERNAL_STORAGE",
    "WRITE_EXTERNAL_STORAGE",
    "MANAGE_EXTERNAL_STORAGE",
    "requestLegacyExternalStorage",
    "preserveLegacyExternalStorage",
)
FORBIDDEN_SOURCE = {
    r"Manifest\.permission\.(?:READ|WRITE|MANAGE)_EXTERNAL_STORAGE":
        "requests broad external-storage access",
    r"Environment\.getExternalStorageDirectory\s*\(":
        "uses the legacy shared-storage pathname",
    r'new\s+File\s*\(\s*"/storage"\s*\)':
        "enumerates shared-storage mount points",
    r'command\s*\(\s*"mount"\s*\)':
        "probes the process mount table",
    r'"/proc/mounts"':
        "reads the kernel mount table",
    r'System\.getenv\s*\(\s*"SECONDARY_STORAGE"\s*\)':
        "uses the legacy secondary-storage environment",
}


def relative(path: Path) -> str:
    return str(path.relative_to(ROOT))


def main() -> None:
    violations: list[str] = []
    for manifest in MANIFESTS:
        text = manifest.read_text(encoding="utf-8")
        for token in FORBIDDEN_MANIFEST:
            if token in text:
                violations.append(f"{relative(manifest)} contains {token}")

    for path in sorted(ANDROID_SOURCE.rglob("*.java")):
        text = path.read_text(encoding="utf-8")
        for pattern, reason in FORBIDDEN_SOURCE.items():
            for match in re.finditer(pattern, text):
                line = text.count("\n", 0, match.start()) + 1
                violations.append(f"{relative(path)}:{line}: {reason}")

    engine = (
        ANDROID_SOURCE / "org" / "coolreader" / "crengine" / "Engine.java"
    ).read_text(encoding="utf-8")
    if "mountedRootsMap = Collections.emptyMap();" not in engine:
        violations.append("Engine does not disable legacy filesystem roots")
    if 'new File(instance.mActivity.getCacheDir(), "engine")' not in engine:
        violations.append("native engine cache is not app-private")

    database = (
        ANDROID_SOURCE / "org" / "coolreader" / "db" / "CRDBService.java"
    ).read_text(encoding="utf-8")
    if "File cr3dir = getFilesDir();" not in database:
        violations.append("database directory is not app-private")

    workflow = (
        ROOT / ".github" / "workflows" / "build.yml"
    ).read_text(encoding="utf-8")
    if "python3 tools/verify_android_storage_policy.py" not in workflow:
        violations.append("GitHub Actions does not run the storage policy gate")

    if violations:
        raise RuntimeError(
            "Android storage policy violations found:\n"
            + "\n".join(violations)
        )
    print("Android storage policy OK: shared files use SAF; persistence is private")


if __name__ == "__main__":
    main()
