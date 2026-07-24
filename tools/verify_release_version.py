#!/usr/bin/env python3
"""Enforce the fork version scheme across Android and desktop sources."""

from __future__ import annotations

import argparse
import datetime as dt
import json
import re
import xml.etree.ElementTree as ET
from pathlib import Path
from typing import Any


ROOT = Path(__file__).resolve().parents[1]
VERSION_FILE = ROOT / "release-version.json"
ANDROID_GRADLE = ROOT / "android" / "app" / "build.gradle"
LEGACY_MANIFEST = ROOT / "android" / "AndroidManifest.xml"
ENGINE_HEADER = ROOT / "crengine" / "include" / "cr3version.h"
FORK_DELTA = ROOT / "FORK_DELTA.md"
ANDROID_NS = "http://schemas.android.com/apk/res/android"


def parse_args() -> argparse.Namespace:
    parser = argparse.ArgumentParser()
    parser.add_argument(
        "--tag",
        help="Optional release tag; must equal v<versionName>",
    )
    return parser.parse_args()


def read_json(path: Path) -> Any:
    with path.open(encoding="utf-8") as stream:
        return json.load(stream)


def main() -> None:
    args = parse_args()
    version = read_json(VERSION_FILE)
    violations: list[str] = []
    if version.get("schemaVersion") != 1:
        violations.append("unsupported release-version schema")

    upstream = version.get("upstreamVersion")
    upstream_commit = version.get("upstreamCommit")
    fork_release = version.get("forkRelease")
    version_name = version.get("versionName")
    version_code = version.get("androidVersionCode")
    if not isinstance(upstream, str) or not re.fullmatch(
        r"\d+\.\d+\.\d+", upstream
    ):
        violations.append("upstreamVersion must be numeric major.minor.patch")
    if not isinstance(upstream_commit, str) or not re.fullmatch(
        r"[0-9a-f]{40}", upstream_commit
    ):
        violations.append("upstreamCommit must be a full Git object ID")
    else:
        delta = FORK_DELTA.read_text(encoding="utf-8")
        if upstream_commit not in delta or f"Base version: `{upstream}`" not in delta:
            violations.append(
                "release-version upstream base differs from FORK_DELTA.md"
            )
    if not isinstance(fork_release, int) or fork_release < 1:
        violations.append("forkRelease must be a positive integer")
    expected_name = (
        f"{upstream}-next.{fork_release}"
        if isinstance(upstream, str) and isinstance(fork_release, int)
        else None
    )
    if version_name != expected_name:
        violations.append(
            f"versionName must be {expected_name}, got {version_name}"
        )

    code_text = str(version_code)
    if not isinstance(version_code, int) or not re.fullmatch(
        r"\d{9}", code_text
    ):
        violations.append("androidVersionCode must use YYMMDDRR0")
    else:
        try:
            dt.date(
                2000 + int(code_text[0:2]),
                int(code_text[2:4]),
                int(code_text[4:6]),
            )
        except ValueError:
            violations.append("androidVersionCode contains an invalid date")
        sequence = int(code_text[6:8])
        if sequence < 1:
            violations.append(
                "androidVersionCode release sequence must be 01..99"
            )
        if not code_text.endswith("0"):
            violations.append(
                "androidVersionCode must reserve final digit for ABI splits"
            )
        if version_code > 2_100_000_000:
            violations.append("androidVersionCode exceeds Google Play limit")

    gradle = ANDROID_GRADLE.read_text(encoding="utf-8")
    for required in (
        "release-version.json",
        "releaseVersion.androidVersionCode",
        "releaseVersion.versionName",
    ):
        if required not in gradle:
            violations.append(f"Android Gradle does not use {required}")
    if re.search(r"^\s*version(?:Code|Name)\s+[\"0-9]", gradle, re.MULTILINE):
        violations.append("Android Gradle contains a duplicate literal version")

    manifest = ET.parse(LEGACY_MANIFEST).getroot()
    manifest_name = manifest.attrib.get(f"{{{ANDROID_NS}}}versionName")
    manifest_code = manifest.attrib.get(f"{{{ANDROID_NS}}}versionCode")
    if manifest_name != version_name:
        violations.append(
            f"legacy manifest versionName is {manifest_name}, "
            f"expected {version_name}"
        )
    if manifest_code != code_text:
        violations.append(
            f"legacy manifest versionCode is {manifest_code}, "
            f"expected {code_text}"
        )

    header = ENGINE_HEADER.read_text(encoding="utf-8")
    match = re.search(
        r'#define\s+CR_ENGINE_VERSION\s+"([^"]+)"', header
    )
    header_version = match.group(1) if match else None
    if header_version != version_name:
        violations.append(
            f"desktop engine version is {header_version}, "
            f"expected {version_name}"
        )

    if args.tag and args.tag != f"v{version_name}":
        violations.append(
            f"release tag must be v{version_name}, got {args.tag}"
        )

    if violations:
        raise RuntimeError(
            "Release version violations found:\n" + "\n".join(violations)
        )
    print(
        f"Release version OK: {version_name}, Android code {version_code}"
    )


if __name__ == "__main__":
    main()
