#!/usr/bin/env python3
"""Validate dependency licenses and generate an SPDX 2.3 release SBOM."""

from __future__ import annotations

import argparse
import datetime as dt
import hashlib
import json
import re
from pathlib import Path
from typing import Any
from urllib.parse import quote


ROOT = Path(__file__).resolve().parents[1]
POLICY_PATH = ROOT / "dependency-policy.json"
GRADLE_BUILD = ROOT / "android" / "app" / "build.gradle"
NATIVE_CMAKE_DIR = ROOT / "android" / "app" / "thirdparty_libs"


def parse_args() -> argparse.Namespace:
    parser = argparse.ArgumentParser()
    parser.add_argument(
        "--gradle-report",
        type=Path,
        required=True,
        help="JSON produced by :app:writeReleaseDependencyInventory",
    )
    parser.add_argument("--output", type=Path, required=True)
    return parser.parse_args()


def spdx_id(kind: str, value: str) -> str:
    digest = hashlib.sha256(value.encode("utf-8")).hexdigest()[:16]
    return f"SPDXRef-{kind}-{digest}"


def read_json(path: Path) -> Any:
    with path.open(encoding="utf-8") as stream:
        return json.load(stream)


def metadata_value(text: str, variable: str) -> str | None:
    match = re.search(
        rf'^{re.escape(variable)}="([^"]+)"$', text, flags=re.MULTILINE
    )
    return match.group(1) if match else None


def maven_license(group: str, rules: list[dict[str, str]]) -> str | None:
    matches = [
        rule for rule in rules if group == rule["prefix"]
        or group.startswith(rule["prefix"])
    ]
    if not matches:
        return None
    return max(matches, key=lambda item: len(item["prefix"]))["license"]


def package(
    *,
    name: str,
    version: str,
    license_expression: str,
    download_location: str,
    purl: str | None = None,
) -> dict[str, Any]:
    result: dict[str, Any] = {
        "SPDXID": spdx_id("Package", f"{name}@{version}"),
        "name": name,
        "versionInfo": version,
        "downloadLocation": download_location,
        "filesAnalyzed": False,
        "licenseConcluded": license_expression,
        "licenseDeclared": license_expression,
        "copyrightText": "NOASSERTION",
        "primaryPackagePurpose": "LIBRARY",
    }
    if purl:
        result["externalRefs"] = [
            {
                "referenceCategory": "PACKAGE-MANAGER",
                "referenceType": "purl",
                "referenceLocator": purl,
            }
        ]
    return result


def main() -> None:
    args = parse_args()
    policy = read_json(POLICY_PATH)
    gradle_report = read_json(args.gradle_report)
    violations: list[str] = []

    if policy.get("schemaVersion") != 1:
        violations.append("unsupported dependency policy schema")
    allowed = set(policy.get("allowedLicenses", []))
    if not allowed:
        violations.append("license allowlist is empty")

    packages: list[dict[str, Any]] = []
    seen_coordinates: set[str] = set()
    for component in gradle_report.get("components", []):
        group = component.get("group", "")
        name = component.get("name", "")
        version = component.get("version", "")
        coordinate = f"{group}:{name}:{version}"
        if not all((group, name, version)):
            violations.append(f"incomplete Maven coordinate: {coordinate}")
            continue
        if coordinate in seen_coordinates:
            violations.append(f"duplicate Maven coordinate: {coordinate}")
            continue
        seen_coordinates.add(coordinate)
        license_expression = maven_license(
            group, policy.get("mavenGroupRules", [])
        )
        if license_expression is None:
            violations.append(
                f"no reviewed license rule for Maven component {coordinate}"
            )
            continue
        if license_expression not in allowed:
            violations.append(
                f"Maven component {coordinate} uses non-allowlisted "
                f"license {license_expression}"
            )
        purl = (
            f"pkg:maven/{quote(group, safe='.')}/{quote(name, safe='')}"
            f"@{quote(version, safe='')}"
        )
        packages.append(
            package(
                name=f"{group}:{name}",
                version=version,
                license_expression=license_expression,
                download_location=purl,
                purl=purl,
            )
        )

    actual_targets: set[str] = set()
    for cmake_file in sorted(NATIVE_CMAKE_DIR.glob("*/CMakeLists.txt")):
        text = cmake_file.read_text(encoding="utf-8")
        actual_targets.update(
            re.findall(r"add_library\s*\(\s*(local_[A-Za-z0-9_]+)", text)
        )
    reviewed_targets: set[str] = set()
    native_names: set[str] = set()
    for component in policy.get("nativeComponents", []):
        name = component["name"]
        version = component["version"]
        if name in native_names:
            violations.append(f"duplicate native policy component: {name}")
        native_names.add(name)
        license_expression = component["license"]
        if license_expression not in allowed:
            violations.append(
                f"native component {name} uses non-allowlisted "
                f"license {license_expression}"
            )
        license_file = ROOT / component["licenseFile"]
        if not license_file.is_file():
            violations.append(
                f"missing license evidence for {name}: "
                f"{component['licenseFile']}"
            )
        source_evidence = component.get("sourceEvidence")
        if source_evidence and not (ROOT / source_evidence).is_file():
            violations.append(
                f"missing source evidence for {name}: {source_evidence}"
            )
        cmake_target = component.get("cmakeTarget")
        if cmake_target:
            reviewed_targets.add(cmake_target)
        metadata = component.get("metadata")
        if metadata:
            metadata_path = ROOT / metadata
            if not metadata_path.is_file():
                violations.append(f"missing metadata for {name}: {metadata}")
            else:
                text = metadata_path.read_text(encoding="utf-8")
                if metadata_value(text, "PN") != name:
                    violations.append(f"{metadata}: PN drift for {name}")
                if metadata_value(text, "PV") != version:
                    violations.append(f"{metadata}: PV drift for {name}")
                checksum = metadata_value(text, "SHA512")
                if checksum is None or not re.fullmatch(
                    r"[0-9a-f]{128}", checksum
                ):
                    violations.append(
                        f"{metadata}: missing pinned SHA-512 for {name}"
                    )
        packages.append(
            package(
                name=name,
                version=version,
                license_expression=license_expression,
                download_location=component["downloadLocation"],
            )
        )

    if actual_targets != reviewed_targets:
        violations.append(
            "Android native CMake target policy drift; "
            f"unreviewed={sorted(actual_targets - reviewed_targets)}, "
            f"stale={sorted(reviewed_targets - actual_targets)}"
        )

    gradle_text = GRADLE_BUILD.read_text(encoding="utf-8")
    if "writeReleaseDependencyInventory" not in gradle_text:
        violations.append("Gradle release dependency inventory task is missing")
    if gradle_report.get("configuration") != "releaseRuntimeClasspath":
        violations.append("Gradle report is not releaseRuntimeClasspath")

    if violations:
        raise RuntimeError(
            "Dependency policy violations found:\n" + "\n".join(violations)
        )

    release_version = read_json(ROOT / "release-version.json")
    root_version = release_version["versionName"]
    root_package = {
        "SPDXID": "SPDXRef-Package-CoolReader",
        "name": "CoolReader Android",
        "versionInfo": root_version,
        "downloadLocation": "https://github.com/hafych/coolreader",
        "filesAnalyzed": False,
        "licenseConcluded": "GPL-2.0-or-later",
        "licenseDeclared": "GPL-2.0-or-later",
        "copyrightText": "NOASSERTION",
        "primaryPackagePurpose": "APPLICATION",
        "externalRefs": [
            {
                "referenceCategory": "PACKAGE-MANAGER",
                "referenceType": "purl",
                "referenceLocator": (
                    "pkg:github/hafych/coolreader@" + quote(root_version)
                ),
            }
        ],
    }
    packages.sort(key=lambda item: (item["name"], item["versionInfo"]))
    relationships = [
        {
            "spdxElementId": "SPDXRef-DOCUMENT",
            "relationshipType": "DESCRIBES",
            "relatedSpdxElement": root_package["SPDXID"],
        }
    ]
    relationships.extend(
        {
            "spdxElementId": root_package["SPDXID"],
            "relationshipType": "DEPENDS_ON",
            "relatedSpdxElement": item["SPDXID"],
        }
        for item in packages
    )
    created = dt.datetime.now(dt.timezone.utc).replace(microsecond=0)
    namespace_digest = hashlib.sha256(
        "\n".join(
            f"{item['name']}@{item['versionInfo']}" for item in packages
        ).encode("utf-8")
    ).hexdigest()
    document = {
        "spdxVersion": "SPDX-2.3",
        "dataLicense": "CC0-1.0",
        "SPDXID": "SPDXRef-DOCUMENT",
        "name": f"coolreader-android-{root_version}",
        "documentNamespace": (
            "https://github.com/hafych/coolreader/sbom/"
            + namespace_digest
        ),
        "creationInfo": {
            "created": created.isoformat().replace("+00:00", "Z"),
            "creators": ["Tool: tools/generate_dependency_sbom.py"],
        },
        "packages": [root_package, *packages],
        "relationships": relationships,
    }
    args.output.parent.mkdir(parents=True, exist_ok=True)
    args.output.write_text(
        json.dumps(document, indent=2, sort_keys=True) + "\n",
        encoding="utf-8",
    )
    print(
        f"Dependency policy OK: {len(packages)} reviewed components; "
        f"SPDX written to {args.output}"
    )


if __name__ == "__main__":
    main()
