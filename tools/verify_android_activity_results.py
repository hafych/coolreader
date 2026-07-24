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
SERVICE_DEPENDENCIES = SOURCE / "crengine" / "ServiceDependencies.java"
READER_VIEW = SOURCE / "crengine" / "ReaderView.java"
FILE_BROWSER = SOURCE / "crengine" / "FileBrowser.java"
ROOT_VIEW = SOURCE / "crengine" / "CRRootView.java"
COVERPAGE_MANAGER = SOURCE / "crengine" / "CoverpageManager.java"
FILE_SYSTEM_FOLDERS = SOURCE / "crengine" / "FileSystemFolders.java"
UTILS = SOURCE / "crengine" / "Utils.java"
ABOUT_DIALOG = SOURCE / "crengine" / "AboutDialog.java"
OPTIONS_DIALOG = SOURCE / "crengine" / "OptionsDialog.java"
SERVICE_ACCESSORS = (
    SOURCE / "db" / "CRDBServiceAccessor.java",
    SOURCE / "sync2" / "SyncServiceAccessor.java",
    SOURCE / "tts" / "TTSControlServiceAccessor.java",
)
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

    dependencies_text = SERVICE_DEPENDENCIES.read_text(encoding="utf-8")
    for marker in (
        "public final class ServiceDependencies",
        "private final ServiceLifecycle lifecycle",
        "public ServiceLifecycle getLifecycle()",
    ):
        if marker not in dependencies_text:
            violations.append(
                f"{relative(SERVICE_DEPENDENCIES)} omits marker: {marker}")

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

    coverpage_manager_text = COVERPAGE_MANAGER.read_text(encoding="utf-8")
    if re.search(r"\bServices\.", coverpage_manager_text):
        violations.append(
            f"{relative(COVERPAGE_MANAGER)} still uses the static service "
            "locator")
    for marker in (
        "private final Engine mEngine",
        "private final ServiceLifecycle mLifecycle",
        "public CoverpageManager(Engine engine, ServiceLifecycle lifecycle)",
        "if (!mLifecycle.isActive())",
    ):
        if marker not in coverpage_manager_text:
            violations.append(
                f"{relative(COVERPAGE_MANAGER)} omits marker: {marker}")

    for path in (FILE_SYSTEM_FOLDERS, UTILS):
        text = path.read_text(encoding="utf-8")
        if re.search(r"\bServices\.", text):
            violations.append(
                f"{relative(path)} still uses the static service locator")
    folders_text = FILE_SYSTEM_FOLDERS.read_text(encoding="utf-8")
    if "private final Scanner mScanner" not in folders_text:
        violations.append(
            f"{relative(FILE_SYSTEM_FOLDERS)} does not retain its Scanner "
            "dependency")
    utils_text = UTILS.read_text(encoding="utf-8")
    for marker in (
        "deleteFolder(FileInfo folder, Scanner scanner",
        "deleteFolderDocTree(FileInfo folder, Scanner scanner",
    ):
        if marker not in utils_text:
            violations.append(f"{relative(UTILS)} omits marker: {marker}")

    for path in (ABOUT_DIALOG, OPTIONS_DIALOG):
        text = path.read_text(encoding="utf-8")
        if re.search(r"\bServices\.", text):
            violations.append(
                f"{relative(path)} still uses the static service locator")
    options_text = OPTIONS_DIALOG.read_text(encoding="utf-8")
    for marker in (
        "private final Engine mEngine",
        "OptionsDialog(BaseActivity activity, Engine engine",
    ):
        if marker not in options_text:
            violations.append(
                f"{relative(OPTIONS_DIALOG)} omits marker: {marker}")

    for path in SERVICE_ACCESSORS:
        text = path.read_text(encoding="utf-8")
        if re.search(r"\bActivity\s+mActivity\s*[=;]", text):
            violations.append(
                f"{relative(path)} strongly retains an Activity")
        for marker in (
            "private final Context mContext",
            "context.getApplicationContext()",
        ):
            if marker not in text:
                violations.append(
                    f"{relative(path)} omits lifecycle marker: {marker}")

    if re.search(r"\bServices\.get", base_text):
        violations.append(
            f"{relative(BASE_ACTIVITY)} still reads the static service locator")
    for marker in (
        "mServiceDependencies = Services.startServices(this)",
        "protected final ServiceDependencies getServiceDependencies()",
    ):
        if marker not in base_text:
            violations.append(
                f"{relative(BASE_ACTIVITY)} omits lifecycle marker: {marker}")

    cool_reader_text = COOL_READER.read_text(encoding="utf-8")
    for marker in REQUIRED_COOL_READER_MARKERS:
        if marker not in cool_reader_text:
            violations.append(
                f"{relative(COOL_READER)} omits marker: {marker}")
    if re.search(r"\bServices\.get", cool_reader_text):
        violations.append(
            f"{relative(COOL_READER)} still reads the static service locator")
    if "ServiceDependencies dependencies = getServiceDependencies()" not in (
            cool_reader_text):
        violations.append(
            f"{relative(COOL_READER)} does not capture its service generation")

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
