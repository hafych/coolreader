#!/usr/bin/env python3
"""Collect the signed universal Android artifacts using Gradle metadata."""

from __future__ import annotations

import argparse
import json
import shutil
from pathlib import Path


ROOT = Path(__file__).resolve().parents[1]
ANDROID_OUTPUTS = ROOT / "android" / "app" / "build" / "outputs"


def parse_args() -> argparse.Namespace:
    parser = argparse.ArgumentParser()
    parser.add_argument("--output-dir", type=Path, required=True)
    return parser.parse_args()


def main() -> None:
    args = parse_args()
    release = json.loads(
        (ROOT / "release-version.json").read_text(encoding="utf-8")
    )
    version = release["versionName"]
    version_code = release["androidVersionCode"]
    apk_dir = ANDROID_OUTPUTS / "apk" / "release"
    metadata = json.loads(
        (apk_dir / "output-metadata.json").read_text(encoding="utf-8")
    )
    universal = [
        element
        for element in metadata.get("elements", [])
        if element.get("type") == "UNIVERSAL"
    ]
    if len(universal) != 1:
        raise RuntimeError(
            f"expected one universal release APK, found {len(universal)}"
        )
    element = universal[0]
    if element.get("versionName") != version:
        raise RuntimeError("release APK versionName drift")
    if element.get("versionCode") != version_code:
        raise RuntimeError("release APK versionCode drift")
    apk_name = element.get("outputFile", "")
    if "unsigned" in apk_name.casefold():
        raise RuntimeError("refusing to collect an unsigned release APK")
    sources = {
        f"coolreader-{version}-android.aab": (
            ANDROID_OUTPUTS / "bundle" / "release" / "app-release.aab"
        ),
        f"coolreader-{version}-android-universal.apk": apk_dir / apk_name,
        f"coolreader-{version}-native-debug-symbols.zip": (
            ANDROID_OUTPUTS
            / "native-debug-symbols"
            / "release"
            / "native-debug-symbols.zip"
        ),
    }
    args.output_dir.mkdir(parents=True, exist_ok=True)
    for name, source in sources.items():
        if not source.is_file() or source.stat().st_size == 0:
            raise RuntimeError(f"missing Android release artifact: {source}")
        shutil.copy2(source, args.output_dir / name)
    print(f"Collected {len(sources)} signed Android artifacts")


if __name__ == "__main__":
    main()
