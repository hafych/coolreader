#!/usr/bin/env python3
"""Verify the complete, checksummed release artifact set."""

from __future__ import annotations

import argparse
import hashlib
import json
import tarfile
import zipfile
from pathlib import Path


ROOT = Path(__file__).resolve().parents[1]


def parse_args() -> argparse.Namespace:
    parser = argparse.ArgumentParser()
    parser.add_argument("--directory", type=Path, required=True)
    return parser.parse_args()


def digest(path: Path) -> str:
    value = hashlib.sha256()
    with path.open("rb") as stream:
        while chunk := stream.read(1024 * 1024):
            value.update(chunk)
    return value.hexdigest()


def archive_roots(path: Path) -> set[str]:
    if path.name.endswith(".tar.gz"):
        with tarfile.open(path, "r:gz") as archive:
            names = archive.getnames()
    elif path.suffix == ".zip":
        with zipfile.ZipFile(path) as archive:
            names = archive.namelist()
    else:
        raise ValueError(f"unsupported archive: {path}")
    return {name.split("/", 1)[0] for name in names if name}


def main() -> None:
    args = parse_args()
    version = json.loads(
        (ROOT / "release-version.json").read_text(encoding="utf-8")
    )["versionName"]
    prefix = f"coolreader-{version}"
    expected = {
        f"{prefix}-android.aab",
        f"{prefix}-android-universal.apk",
        f"{prefix}-android.spdx.json",
        f"{prefix}-native-debug-symbols.zip",
        f"{prefix}-signing.txt",
        f"{prefix}-linux-x86_64.tar.gz",
        f"{prefix}-macos-arm64.zip",
        "SHA256SUMS",
    }
    actual = {
        path.name for path in args.directory.iterdir() if path.is_file()
    }
    violations: list[str] = []
    if actual != expected:
        violations.append(
            f"artifact set drift; missing={sorted(expected - actual)}, "
            f"extra={sorted(actual - expected)}"
        )
    for name in expected - {"SHA256SUMS"}:
        path = args.directory / name
        if path.is_file() and path.stat().st_size == 0:
            violations.append(f"empty release artifact: {name}")

    checksum_file = args.directory / "SHA256SUMS"
    recorded: dict[str, str] = {}
    if not checksum_file.is_file():
        violations.append("SHA256SUMS is missing")
    else:
        for line in checksum_file.read_text(encoding="utf-8").splitlines():
            parts = line.split("  ", 1)
            if len(parts) != 2:
                violations.append(f"malformed checksum line: {line}")
                continue
            recorded[parts[1]] = parts[0]
        checksum_targets = expected - {"SHA256SUMS"}
        if recorded.keys() != checksum_targets:
            violations.append("SHA256SUMS target set does not match artifacts")
        for name, expected_digest in recorded.items():
            path = args.directory / name
            if path.is_file() and digest(path) != expected_digest:
                violations.append(f"checksum mismatch: {name}")

    sbom_path = args.directory / f"{prefix}-android.spdx.json"
    if sbom_path.is_file():
        sbom = json.loads(sbom_path.read_text(encoding="utf-8"))
        if sbom.get("spdxVersion") != "SPDX-2.3":
            violations.append("release SBOM is not SPDX 2.3")
        package_versions = {
            package.get("versionInfo") for package in sbom.get("packages", [])
        }
        if version not in package_versions:
            violations.append("release SBOM omits the release version")

    for platform, suffix in (
        ("linux-x86_64", "tar.gz"),
        ("macos-arm64", "zip"),
    ):
        name = f"{prefix}-{platform}.{suffix}"
        path = args.directory / name
        if path.is_file():
            expected_root = f"{prefix}-{platform}"
            roots = archive_roots(path)
            if roots != {expected_root}:
                violations.append(
                    f"{name} has unexpected archive roots: {sorted(roots)}"
                )

    if violations:
        raise RuntimeError(
            "Release artifact violations found:\n" + "\n".join(violations)
        )
    print(f"Release artifacts OK: {len(expected) - 1} files and checksums")


if __name__ == "__main__":
    main()
