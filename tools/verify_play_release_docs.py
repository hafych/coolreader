#!/usr/bin/env python3
"""Validate the prepared Play release documentation against the source tree."""

from __future__ import annotations

import re
from pathlib import Path


ROOT = Path(__file__).resolve().parents[1]
DOCS = ROOT / "docs"
LISTING = DOCS / "PLAY_LISTING.md"
REQUIRED_DOCS = (
    LISTING,
    DOCS / "PRIVACY_POLICY.md",
    DOCS / "DATA_SAFETY.md",
    DOCS / "PLAY_RELEASE.md",
    DOCS / "IDENTITY_AND_ASSETS.md",
)
LIMITS = {
    "Title": 30,
    "Short description": 80,
    "Full description": 4000,
}
LOCALES = ("en-US", "ru-RU")
OWNER_PLACEHOLDERS = (
    "[DEVELOPER LEGAL NAME]",
    "[SUPPORT CONTACT EMAIL]",
    "[PRIVACY CONTACT EMAIL]",
    "[PUBLIC POLICY URL]",
    "[OWNER DECISION REQUIRED]",
)


def relative(path: Path) -> str:
    return str(path.relative_to(ROOT))


def section(text: str, heading: str, next_level: int) -> str:
    marker = "#" * next_level
    match = re.search(
        rf"^{marker} {re.escape(heading)}\n(.*?)(?=^{marker} |\Z)",
        text,
        flags=re.MULTILINE | re.DOTALL,
    )
    if not match:
        raise ValueError(f"missing section {marker} {heading}")
    return match.group(1).strip()


def main() -> None:
    violations: list[str] = []

    for path in REQUIRED_DOCS:
        if not path.is_file():
            violations.append(f"missing release document: {relative(path)}")
    if violations:
        raise RuntimeError("\n".join(violations))

    listing = LISTING.read_text(encoding="utf-8")
    for locale in LOCALES:
        try:
            locale_text = section(listing, locale, 2)
        except ValueError as error:
            violations.append(str(error))
            continue
        for field, limit in LIMITS.items():
            try:
                value = section(locale_text, field, 3)
            except ValueError as error:
                violations.append(f"{locale}: {error}")
                continue
            length = len(value)
            if not value:
                violations.append(f"{locale} {field} is empty")
            elif length > limit:
                violations.append(
                    f"{locale} {field} is {length} characters; limit is {limit}"
                )

    all_docs = "\n".join(
        path.read_text(encoding="utf-8") for path in REQUIRED_DOCS
    )
    for phrase in (
        "consumption-only",
        "no ads",
        "analytics",
        "crash-reporting",
        "Storage Access Framework",
        "staged rollout",
    ):
        if phrase.casefold() not in all_docs.casefold():
            violations.append(f"release docs omit required fact: {phrase}")

    identity = (DOCS / "IDENTITY_AND_ASSETS.md").read_text(encoding="utf-8")
    privacy = (DOCS / "PRIVACY_POLICY.md").read_text(encoding="utf-8")
    for placeholder in OWNER_PLACEHOLDERS:
        if placeholder not in identity and placeholder not in privacy:
            violations.append(f"owner placeholder is not tracked: {placeholder}")

    gradle = (ROOT / "android" / "app" / "build.gradle").read_text(
        encoding="utf-8"
    )
    for source_fact in (
        'applicationId "org.coolreader"',
        "versionCode 32570",
        'versionName "3.2.59-1"',
    ):
        if source_fact not in gradle:
            violations.append(
                f"Android identity changed; refresh Play docs: {source_fact}"
            )

    manifests = (
        ROOT / "android" / "AndroidManifest.xml",
        ROOT / "android" / "app" / "src" / "main" / "AndroidManifest.xml",
    )
    for manifest in manifests:
        text = manifest.read_text(encoding="utf-8")
        if 'android:allowBackup="false"' not in text:
            violations.append(f"backup policy drift in {relative(manifest)}")

    dependency_text = gradle.casefold()
    for forbidden_sdk in ("firebase-analytics", "firebase-crashlytics", "sentry"):
        if forbidden_sdk in dependency_text:
            violations.append(
                f"new telemetry dependency needs disclosure: {forbidden_sdk}"
            )

    workflow = (ROOT / ".github" / "workflows" / "build.yml").read_text(
        encoding="utf-8"
    )
    command = "python3 tools/verify_play_release_docs.py"
    if command not in workflow:
        violations.append("GitHub Actions does not validate Play release docs")

    if violations:
        raise RuntimeError(
            "Play release documentation violations found:\n"
            + "\n".join(violations)
        )

    print(
        "Play release docs OK: listing limits, privacy baseline and owner "
        "blockers are explicit"
    )


if __name__ == "__main__":
    main()
