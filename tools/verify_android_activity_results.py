#!/usr/bin/env python3
"""Keep active Android result and permission flows on lifecycle-aware APIs."""

from __future__ import annotations

import re
from pathlib import Path


ROOT = Path(__file__).resolve().parents[1]
SOURCE = ROOT / "android" / "src" / "org" / "coolreader"
COOL_READER = SOURCE / "CoolReader.java"
DICTIONARIES = SOURCE / "Dictionaries.java"
BASE_ACTIVITY = SOURCE / "crengine" / "BaseActivity.java"
NOOK_CONTROLLER = SOURCE / "crengine" / "N2EpdController.java"
ENGINE = SOURCE / "crengine" / "Engine.java"
SERVICES = SOURCE / "crengine" / "Services.java"
READER_VIEW = SOURCE / "crengine" / "ReaderView.java"
FILE_BROWSER = SOURCE / "crengine" / "FileBrowser.java"
ROOT_VIEW = SOURCE / "crengine" / "CRRootView.java"
ACTIVE_RESULT_SOURCES = (COOL_READER, DICTIONARIES, BASE_ACTIVITY)
FORBIDDEN_RESULT_PATTERNS = (
    r"\bstartActivityForResult\s*\(",
    r"\bvoid\s+onActivityResult\s*\(",
)
FORBIDDEN_PERMISSION_PATTERNS = (
    r"\brequestPermissions\s*\(",
    r"\bvoid\s+onRequestPermissionsResult\s*\(",
)
REQUIRED_COOL_READER_MARKERS = (
    "mSelectLibraryRootLauncher",
    "mOpenLibraryDocumentLauncher",
    "mOpenDocumentTreeLauncher",
    "registerForActivityResult",
    "STATE_OPEN_DOCUMENT_TREE_COMMAND",
    "STATE_OPEN_DOCUMENT_TREE_ARG",
)


def relative(path: Path) -> str:
    return str(path.relative_to(ROOT))


def find_pattern(
        path: Path,
        text: str,
        pattern: str,
        reason: str,
        violations: list[str]) -> None:
    for match in re.finditer(pattern, text):
        line = text.count("\n", 0, match.start()) + 1
        violations.append(f"{relative(path)}:{line}: {reason}")


def main() -> None:
    violations: list[str] = []
    for path in ACTIVE_RESULT_SOURCES:
        text = path.read_text(encoding="utf-8")
        for pattern in FORBIDDEN_RESULT_PATTERNS:
            find_pattern(
                path,
                text,
                pattern,
                "uses a legacy activity-result callback",
                violations)

    for path in sorted(SOURCE.rglob("*.java")):
        text = path.read_text(encoding="utf-8")
        for pattern in FORBIDDEN_PERMISSION_PATTERNS:
            find_pattern(
                path,
                text,
                pattern,
                "uses a manual runtime-permission callback",
                violations)

    base_text = BASE_ACTIVITY.read_text(encoding="utf-8")
    if "extends ComponentActivity" not in base_text:
        violations.append("BaseActivity is not lifecycle-aware")
    if "mDictionaryLauncher" not in base_text:
        violations.append("Dictan result launcher is missing")

    nook_controller_text = NOOK_CONTROLLER.read_text(encoding="utf-8")
    if re.search(
            r"\bstatic\s+Activity\s+\w+\s*(?:=|;)",
            nook_controller_text):
        violations.append(
            f"{relative(NOOK_CONTROLLER)} retains a static Activity")
    if "n2MainActivity" in nook_controller_text:
        violations.append(
            f"{relative(NOOK_CONTROLLER)} retains the legacy Activity slot")
    if re.search(
            r"\bstatic\s+Object\s+mEpdController\b",
            nook_controller_text):
        violations.append(
            f"{relative(NOOK_CONTROLLER)} retains the vendor controller "
            "statically")

    engine_text = ENGINE.read_text(encoding="utf-8")
    if re.search(r"\bBaseActivity\s+mActivity\s*[=;]", engine_text):
        violations.append(
            f"{relative(ENGINE)} strongly retains an Activity")
    if re.search(r"\bstatic\s+Engine\s+instance\b", engine_text):
        violations.append(
            f"{relative(ENGINE)} retains the legacy Engine singleton")
    for marker in (
        "WeakReference<BaseActivity>",
        "mAppContext",
        "detachActivity",
    ):
        if marker not in engine_text:
            violations.append(f"{relative(ENGINE)} omits marker: {marker}")

    services_text = SERVICES.read_text(encoding="utf-8")
    for marker in (
        "!mEngine.isAttachedTo(activity)",
        "Engine engine = mEngine",
        "mGeneration == stoppedGeneration",
    ):
        if marker not in services_text:
            violations.append(
                f"{relative(SERVICES)} omits lifecycle marker: {marker}")

    for path in sorted(SOURCE.rglob("*.java")):
        text = path.read_text(encoding="utf-8")
        find_pattern(
            path,
            text,
            r"\bEngine\.getInstance\s*\(",
            "uses the legacy Activity-owned Engine singleton",
            violations)

    reader_view_text = READER_VIEW.read_text(encoding="utf-8")
    if re.search(r"\bServices\.", reader_view_text):
        violations.append(
            f"{relative(READER_VIEW)} still uses the static service locator")
    for marker in (
        "private final Scanner mScanner",
        "private final History mHistory",
        "private final DocumentFileCache mDocumentCache",
        "private final ServiceLifecycle mServiceLifecycle",
    ):
        if marker not in reader_view_text:
            violations.append(f"{relative(READER_VIEW)} omits marker: {marker}")

    for path in (FILE_BROWSER, ROOT_VIEW):
        text = path.read_text(encoding="utf-8")
        if re.search(r"\bServices\.", text):
            violations.append(
                f"{relative(path)} still uses the static service locator")

    cool_reader_text = COOL_READER.read_text(encoding="utf-8")
    for marker in REQUIRED_COOL_READER_MARKERS:
        if marker not in cool_reader_text:
            violations.append(
                f"{relative(COOL_READER)} omits marker: {marker}")
    allowed_composition_reads = {
        "mEngine = Services.getEngine();",
        "mScanner = Services.getScanner();",
        "mHistory = Services.getHistory();",
        "mCoverpageManager = Services.getCoverpageManager();",
        "mDocumentCache = Services.getDocumentCache();",
        "mFileSystemFolders = Services.getFileSystemFolders();",
        "mServiceLifecycle = Services.getLifecycle();",
    }
    for line_number, line in enumerate(
            cool_reader_text.splitlines(), start=1):
        if "Services.get" in line and line.strip() not in allowed_composition_reads:
            violations.append(
                f"{relative(COOL_READER)}:{line_number}: reads the static "
                "service locator outside the composition root")

    command = "python3 tools/verify_android_activity_results.py"
    for workflow in (
        ROOT / ".github" / "workflows" / "build.yml",
        ROOT / ".github" / "workflows" / "release.yml",
    ):
        if command not in workflow.read_text(encoding="utf-8"):
            violations.append(
                f"{relative(workflow)} does not run the activity-result gate")

    if violations:
        raise RuntimeError(
            "Android activity-result policy violations found:\n"
            + "\n".join(violations))
    print(
        "Android activity-result policy OK: active result flows use "
        "lifecycle-aware launchers")


if __name__ == "__main__":
    main()
