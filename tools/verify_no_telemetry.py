#!/usr/bin/env python3
"""Enforce the owner decision that release builds collect no telemetry."""

from __future__ import annotations

import json
from pathlib import Path


ROOT = Path(__file__).resolve().parents[1]
ANDROID = ROOT / "android"
POLICY_PATH = ROOT / "telemetry-policy.json"
CRASH_PRIVACY = (
    ANDROID / "src" / "org" / "coolreader" / "crengine"
    / "CrashPrivacy.java"
)
RELEASE_MANIFESTS = (
    ANDROID / "AndroidManifest.xml",
    ANDROID / "app" / "src" / "main" / "AndroidManifest.xml",
    ANDROID / "genrescollection" / "src" / "main" / "AndroidManifest.xml",
)
BUILD_FILES = (
    ANDROID / "build.gradle",
    ANDROID / "settings.gradle",
    ANDROID / "app" / "build.gradle",
    ANDROID / "genrescollection" / "build.gradle",
    ROOT / "dependency-policy.json",
)
REQUIRED_LOCAL_CRASH_MARKERS = (
    "Thread.setDefaultUncaughtExceptionHandler",
    "LogRedactor.sanitizeThrowable",
    "previous.uncaughtException",
)
FORBIDDEN_CRASH_NETWORK_MARKERS = (
    "java.net.",
    "SecureHttp",
    "HttpURLConnection",
    "Socket(",
)


def relative(path: Path) -> str:
    return str(path.relative_to(ROOT))


def main() -> None:
    policy = json.loads(POLICY_PATH.read_text(encoding="utf-8"))
    violations: list[str] = []
    if policy.get("schemaVersion") != 1:
        violations.append("unsupported telemetry policy schema")
    if policy.get("status") != "disabled":
        violations.append("telemetry policy must remain disabled")
    if policy.get("decisionRequiredBeforeEnable") is not True:
        violations.append("telemetry enablement must require an owner decision")
    decision_topics = policy.get("requiredDecisionTopics", [])
    if len(decision_topics) < 6:
        violations.append("telemetry decision topics are incomplete")
    forbidden_tokens = policy.get("forbiddenSdkTokens", [])
    if not forbidden_tokens:
        violations.append("telemetry SDK denylist is empty")

    live_files = [
        *BUILD_FILES,
        *RELEASE_MANIFESTS,
        *sorted((ANDROID / "src").rglob("*.java")),
    ]
    for path in live_files:
        text = path.read_text(encoding="utf-8").casefold()
        for token in forbidden_tokens:
            if token.casefold() in text:
                violations.append(
                    f"{relative(path)} contains forbidden telemetry SDK "
                    f"token: {token}"
                )
    for jar in sorted((ANDROID / "app" / "libs").glob("*.jar")):
        name = jar.name.casefold()
        for token in forbidden_tokens:
            if token.casefold().split(".")[-1] in name:
                violations.append(
                    f"{relative(jar)} resembles forbidden telemetry SDK "
                    f"token: {token}"
                )

    crash_text = CRASH_PRIVACY.read_text(encoding="utf-8")
    for marker in REQUIRED_LOCAL_CRASH_MARKERS:
        if marker not in crash_text:
            violations.append(
                f"{relative(CRASH_PRIVACY)} omits local crash marker: {marker}"
            )
    for marker in FORBIDDEN_CRASH_NETWORK_MARKERS:
        if marker in crash_text:
            violations.append(
                f"{relative(CRASH_PRIVACY)} contains network marker: {marker}"
            )

    for path in (
        ROOT / "docs" / "TELEMETRY_POLICY.md",
        ROOT / "docs" / "PRIVACY_POLICY.md",
        ROOT / "docs" / "DATA_SAFETY.md",
    ):
        text = path.read_text(encoding="utf-8").casefold()
        if "no telemetry" not in text and "telemetry" not in text:
            violations.append(
                f"{relative(path)} omits the no-telemetry decision"
            )

    command = "python3 tools/verify_no_telemetry.py"
    for workflow in (
        ROOT / ".github" / "workflows" / "build.yml",
        ROOT / ".github" / "workflows" / "release.yml",
    ):
        if command not in workflow.read_text(encoding="utf-8"):
            violations.append(
                f"{relative(workflow)} does not run the telemetry policy gate"
            )

    if violations:
        raise RuntimeError(
            "No-telemetry policy violations found:\n"
            + "\n".join(violations)
        )
    print(
        "No-telemetry policy OK: local redaction only; "
        "no analytics or crash-reporting SDK"
    )


if __name__ == "__main__":
    main()
