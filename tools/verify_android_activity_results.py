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
TOAST_VIEW = SOURCE / "crengine" / "ToastView.java"
SCANNER = SOURCE / "crengine" / "Scanner.java"
READER_VIEW = SOURCE / "crengine" / "ReaderView.java"
PAGE_FLIP_GEOMETRY = SOURCE / "crengine" / "PageFlipGeometry.java"
PAGE_CURVE_TABLES = SOURCE / "crengine" / "PageCurveTables.java"
BACKGROUND_THREAD = SOURCE / "crengine" / "BackgroundThread.java"
DEFERRED_TASK_QUEUE = SOURCE / "crengine" / "DeferredTaskQueue.java"
FREEZABLE_REGISTRY = SOURCE / "crengine" / "FreezableRegistry.java"
PAGE_FLIP_GEOMETRY_TEST = (
    ROOT
    / "android"
    / "app"
    / "src"
    / "test"
    / "java"
    / "org"
    / "coolreader"
    / "crengine"
    / "PageFlipGeometryTest.java"
)
PAGE_CURVE_TABLES_TEST = (
    ROOT
    / "android"
    / "app"
    / "src"
    / "test"
    / "java"
    / "org"
    / "coolreader"
    / "crengine"
    / "PageCurveTablesTest.java"
)
DEFERRED_TASK_QUEUE_TEST = (
    ROOT
    / "android"
    / "app"
    / "src"
    / "test"
    / "java"
    / "org"
    / "coolreader"
    / "crengine"
    / "DeferredTaskQueueTest.java"
)
FREEZABLE_REGISTRY_TEST = (
    ROOT
    / "android"
    / "app"
    / "src"
    / "test"
    / "java"
    / "org"
    / "coolreader"
    / "crengine"
    / "FreezableRegistryTest.java"
)
HYPH_DICT_REGISTRY_TEST = (
    ROOT
    / "android"
    / "app"
    / "src"
    / "test"
    / "java"
    / "org"
    / "coolreader"
    / "crengine"
    / "HyphDictRegistryTest.java"
)
FILE_BROWSER = SOURCE / "crengine" / "FileBrowser.java"
ROOT_VIEW = SOURCE / "crengine" / "CRRootView.java"
COVERPAGE_MANAGER = SOURCE / "crengine" / "CoverpageManager.java"
FILE_SYSTEM_FOLDERS = SOURCE / "crengine" / "FileSystemFolders.java"
UTILS = SOURCE / "crengine" / "Utils.java"
ABOUT_DIALOG = SOURCE / "crengine" / "AboutDialog.java"
OPTIONS_DIALOG = SOURCE / "crengine" / "OptionsDialog.java"
BOOK_INFO_DIALOGS = (
    SOURCE / "crengine" / "BookInfoDialog.java",
    SOURCE / "crengine" / "BookInfoEditDialog.java",
    SOURCE / "crengine" / "OnlineStoreBookInfoDialog.java",
    SOURCE / "crengine" / "TTSToolbarDlg.java",
)
SYNCHRONIZER = SOURCE / "sync2" / "Synchronizer.java"
OPDS_UTIL = SOURCE / "crengine" / "OPDSUtil.java"
MAIN_DB = SOURCE / "db" / "MainDB.java"
CRDB_SERVICE = SOURCE / "db" / "CRDBService.java"
FILE_INFO = SOURCE / "crengine" / "FileInfo.java"
DOCUMENT_FILE_CACHE = SOURCE / "crengine" / "DocumentFileCache.java"
EXTERNAL_DOCUMENT_VALIDATOR = (
    SOURCE / "crengine" / "ExternalDocumentValidator.java"
)
ONLINE_STORE_PLUGIN_MANAGER = (
    SOURCE / "plugins" / "OnlineStorePluginManager.java"
)
LITRES_PLUGIN = SOURCE / "plugins" / "litres" / "LitresPlugin.java"
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
        "private final MountPathCorrector mPathCorrector",
        "private static final List<File> MOUNTED_ROOTS",
        "private static final Map<String, String> MOUNTED_ROOTS_MAP",
        "private static final int PROGRESS_STYLE",
        "public static final int DOM_VERSION_CURRENT",
        "String[] fonts = findFonts()",
        "private static final FreezableRegistry<HyphDict> REGISTRY",
        "public final String language",
        "static synchronized HyphDict[] freezeValues()",
        "initDictionaries(HyphDict.freezeValues())",
    ):
        if marker not in engine_text:
            violations.append(f"{relative(ENGINE)} omits marker: {marker}")
    for marker in (
        "private static File[] mountedRootsList",
        "private static Map<String, String> mountedRootsMap",
        "private static MountPathCorrector pathCorrector",
        "private static String[] mFonts",
        "private static HyphDict[] values",
    ):
        if marker in engine_text:
            violations.append(
                f"{relative(ENGINE)} retains mutable process snapshot "
                f"state: {marker}")

    freezable_registry_text = FREEZABLE_REGISTRY.read_text(
        encoding="utf-8")
    for marker in (
        "synchronized boolean add(T item)",
        "synchronized List<T> snapshot()",
        "synchronized List<T> freeze()",
        "Collections.unmodifiableList(",
        "builder.clear()",
    ):
        if marker not in freezable_registry_text:
            violations.append(
                f"{relative(FREEZABLE_REGISTRY)} omits registry marker: "
                f"{marker}")
    for path, markers in (
        (
            FREEZABLE_REGISTRY_TEST,
            (
                "snapshotsDoNotExposeBuilderStorage",
                "freezeIsIdempotentAndRejectsLatePublication",
                "nullItemsAreRejectedBeforePublication",
            ),
        ),
        (
            HYPH_DICT_REGISTRY_TEST,
            (
                "valuesReturnIndependentOrderedSnapshots",
                "frozenNativeSnapshotRejectsLateFilePublication",
            ),
        ),
    ):
        text = path.read_text(encoding="utf-8")
        for marker in markers:
            if marker not in text:
                violations.append(
                    f"{relative(path)} omits registry regression: "
                    f"{marker}")

    services_text = SERVICES.read_text(encoding="utf-8")
    if re.search(
            r"\bstatic(?:\s+volatile)?\s+"
            r"(?:Engine|Scanner|History|CoverpageManager|"
            r"FileSystemFolders|GenresCollection|DocumentFileCache|"
            r"ServiceLifecycle)\b",
            services_text):
        violations.append(
            f"{relative(SERVICES)} retains a static service graph")
    for marker in (
        "private Engine engine",
        "public ServiceDependencies startServices(BaseActivity activity)",
        "lifecycle = new ServiceLifecycle(",
        "public void stopServices(BaseActivity activity)",
        "stoppedLifecycle.close()",
    ):
        if marker not in services_text:
            violations.append(
                f"{relative(SERVICES)} omits lifecycle marker: {marker}")
    if "BackgroundThread.instance().quit()" in services_text:
        violations.append(
            f"{relative(SERVICES)} lets Activity teardown stop the "
            "process-scoped dispatcher")

    background_thread_text = BACKGROUND_THREAD.read_text(encoding="utf-8")
    for marker in (
        "private static volatile BackgroundThread instance",
        "private volatile Handler handler",
        "private volatile Handler guiHandler",
        "backgroundTasks.attach(",
        "backgroundTasks.attach(null)",
        "guiTasks.attach(",
    ):
        if marker not in background_thread_text:
            violations.append(
                f"{relative(BACKGROUND_THREAD)} omits dispatcher marker: "
                f"{marker}")
    for marker in (
        "ArrayList<Runnable> posted",
        "postedGUI",
        "delayedTaskId",
        "mStopped",
    ):
        if marker in background_thread_text:
            violations.append(
                f"{relative(BACKGROUND_THREAD)} retains legacy dispatcher "
                f"state: {marker}")

    deferred_task_queue_text = DEFERRED_TASK_QUEUE.read_text(
        encoding="utf-8")
    for marker in (
        "synchronized int attach(Dispatcher<T> dispatcher)",
        "synchronized boolean post(T task, long delay)",
        "pending.subList(0, delivered).clear()",
        "Math.max(0, delay)",
    ):
        if marker not in deferred_task_queue_text:
            violations.append(
                f"{relative(DEFERRED_TASK_QUEUE)} omits handoff marker: "
                f"{marker}")

    deferred_task_queue_test_text = DEFERRED_TASK_QUEUE_TEST.read_text(
        encoding="utf-8")
    for marker in (
        "queuedTasksKeepDelayAndDrainOnlyOnce",
        "attachAndPostSerializeWithoutLoss",
        "rejectedDrainRemainsAvailableForNextTarget",
    ):
        if marker not in deferred_task_queue_test_text:
            violations.append(
                f"{relative(DEFERRED_TASK_QUEUE_TEST)} omits regression "
                f"marker: {marker}")

    toast_text = TOAST_VIEW.read_text(encoding="utf-8")
    if re.search(
            r"\bstatic\s+(?:final\s+)?(?:View|Handler|PopupWindow|"
            r"Queue<|LinkedBlockingQueue<|AtomicBoolean)\b",
            toast_text):
        violations.append(
            f"{relative(TOAST_VIEW)} retains static Activity-owned UI state")
    for marker in (
        "public final class ToastView",
        "private final Queue<ToastMessage> queue",
        "public void close()",
    ):
        if marker not in toast_text:
            violations.append(
                f"{relative(TOAST_VIEW)} omits lifecycle marker: {marker}")

    dependencies_text = SERVICE_DEPENDENCIES.read_text(encoding="utf-8")
    for marker in (
        "public final class ServiceDependencies",
        "private final ServiceLifecycle lifecycle",
        "public ServiceLifecycle getLifecycle()",
    ):
        if marker not in dependencies_text:
            violations.append(
                f"{relative(SERVICE_DEPENDENCIES)} omits marker: {marker}")

    scanner_text = SCANNER.read_text(encoding="utf-8")
    if re.search(r"\bBaseActivity\s+\w+\s*(?:=|;)", scanner_text):
        violations.append(
            f"{relative(SCANNER)} strongly retains its Activity")
    for marker in (
        "private final Context mContext",
        "Context applicationContext = context.getApplicationContext()",
    ):
        if marker not in scanner_text:
            violations.append(f"{relative(SCANNER)} omits marker: {marker}")

    plugin_manager_text = ONLINE_STORE_PLUGIN_MANAGER.read_text(
        encoding="utf-8")
    litres_plugin_text = LITRES_PLUGIN.read_text(encoding="utf-8")
    for path, text in (
            (ONLINE_STORE_PLUGIN_MANAGER, plugin_manager_text),
            (LITRES_PLUGIN, litres_plugin_text)):
        if re.search(r"\bActivity\s+\w+\s*(?:=|;|,|\))", text):
            violations.append(
                f"{relative(path)} retains an Activity in the plugin cache")
    if "context.getApplicationContext()" not in plugin_manager_text:
        violations.append(
            f"{relative(ONLINE_STORE_PLUGIN_MANAGER)} does not normalize "
            "cached plugins to application context")
    if "private final Context applicationContext" not in litres_plugin_text:
        violations.append(
            f"{relative(LITRES_PLUGIN)} does not retain application context")

    for path in sorted(SOURCE.rglob("*.java")):
        text = path.read_text(encoding="utf-8")
        find_pattern(
            path,
            text,
            r"\bEngine\.getInstance\s*\(",
            "uses the legacy Activity-owned Engine singleton",
            violations)
        find_pattern(
            path,
            text,
            r"\bServices\.(?:get|startServices|stopServices|isStopped)\s*\(",
            "uses the static service locator",
            violations)

    reader_view_text = READER_VIEW.read_text(encoding="utf-8")
    if re.search(r"\bServices\.", reader_view_text):
        violations.append(
            f"{relative(READER_VIEW)} still uses the static service locator")
    for marker in (
        "private final Scanner mScanner",
        "private final History mHistory",
        "private final CoverpageManager mCoverpageManager",
        "private final GenresCollection mGenresCollection",
        "private final DocumentFileCache mDocumentCache",
        "private final ServiceLifecycle mServiceLifecycle",
    ):
        if marker not in reader_view_text:
            violations.append(f"{relative(READER_VIEW)} omits marker: {marker}")
    if "PageFlipGeometry.tableIndex(" not in reader_view_text:
        violations.append(
            f"{relative(READER_VIEW)} does not use bounded page-flip "
            "table indexing")
    if "private static final PageCurveTables PAGE_CURVE_TABLES" not in (
            reader_view_text):
        violations.append(
            f"{relative(READER_VIEW)} does not use one final page-curve "
            "table owner")
    for marker in (
        "private static int[] SIN_TABLE",
        "private static int[] ASIN_TABLE",
        "private static int[] SRC_TABLE",
        "private static int[] DST_TABLE",
    ):
        if marker in reader_view_text:
            violations.append(
                f"{relative(READER_VIEW)} retains mutable page-curve "
                f"storage: {marker}")
    find_pattern(
        READER_VIEW,
        reader_view_text,
        r"\b(?:private\s+static|static\s+private|static)\s+"
        r"(?:final\s+)?SimpleDateFormat\b",
        "retains a process-wide mutable date formatter",
        violations)
    find_pattern(
        READER_VIEW,
        reader_view_text,
        r"\[[^\]\n]*\*\s*SIN_TABLE_SIZE\s*/",
        "indexes a page-flip table with unchecked int arithmetic",
        violations)

    page_flip_geometry_text = PAGE_FLIP_GEOMETRY.read_text(encoding="utf-8")
    for marker in (
        "static int tableIndex(int value, int maximum, int lastIndex)",
        "value >= maximum",
        "(long) value * lastIndex / maximum",
    ):
        if marker not in page_flip_geometry_text:
            violations.append(
                f"{relative(PAGE_FLIP_GEOMETRY)} omits bounded-index "
                f"marker: {marker}")

    page_flip_geometry_test_text = PAGE_FLIP_GEOMETRY_TEST.read_text(
        encoding="utf-8")
    for marker in (
        "indexIsClampedToBothTableEdges",
        "indexUsesWidenedIntermediateArithmetic",
        "Integer.MAX_VALUE - 1",
    ):
        if marker not in page_flip_geometry_test_text:
            violations.append(
                f"{relative(PAGE_FLIP_GEOMETRY_TEST)} omits regression "
                f"marker: {marker}")

    page_curve_tables_text = PAGE_CURVE_TABLES.read_text(encoding="utf-8")
    for marker in (
        "final class PageCurveTables",
        "private final int[] sine",
        "private final int[] arcsine",
        "private final int[] sourceAngle",
        "private final int[] destinationShift",
        "private static double shiftAngle(double dx)",
    ):
        if marker not in page_curve_tables_text:
            violations.append(
                f"{relative(PAGE_CURVE_TABLES)} omits immutable curve "
                f"marker: {marker}")

    page_curve_tables_test_text = PAGE_CURVE_TABLES_TEST.read_text(
        encoding="utf-8")
    for marker in (
        "valuesMatchLegacyPageCurlLookup",
        "everyCurveIsMonotonic",
        "invalidTableShapeIsRejected",
    ):
        if marker not in page_curve_tables_test_text:
            violations.append(
                f"{relative(PAGE_CURVE_TABLES_TEST)} omits curve regression "
                f"marker: {marker}")

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

    for path in BOOK_INFO_DIALOGS:
        text = path.read_text(encoding="utf-8")
        if re.search(r"\bServices\.", text):
            violations.append(
                f"{relative(path)} still uses the static service locator")

    synchronizer_text = SYNCHRONIZER.read_text(encoding="utf-8")
    if re.search(r"\bServices\.", synchronizer_text):
        violations.append(
            f"{relative(SYNCHRONIZER)} still uses the static service locator")
    for marker in (
        "private final Scanner mScanner",
        "CoolReader coolReader,\n\t\t\tScanner scanner,",
    ):
        if marker not in synchronizer_text:
            violations.append(
                f"{relative(SYNCHRONIZER)} omits marker: {marker}")

    opds_text = OPDS_UTIL.read_text(encoding="utf-8")
    if re.search(r"\bServices\.", opds_text):
        violations.append(
            f"{relative(OPDS_UTIL)} still uses the static service locator")
    if re.search(
            r"\bstatic\s+DownloadTask\s+\w+\s*(?:=|;)",
            opds_text):
        violations.append(
            f"{relative(OPDS_UTIL)} statically retains an Activity-owned task")
    for marker in (
        "final private Engine engine",
        "final private ServiceLifecycle serviceLifecycle",
        "cancelled || !serviceLifecycle.isActive()",
    ):
        if marker not in opds_text:
            violations.append(f"{relative(OPDS_UTIL)} omits marker: {marker}")

    main_db_text = MAIN_DB.read_text(encoding="utf-8")
    if re.search(r"\bServices\.", main_db_text):
        violations.append(
            f"{relative(MAIN_DB)} still uses the static service locator")
    for marker in (
        "private final GenresCollection mGenresCollection",
        "public MainDB(GenresCollection genresCollection)",
    ):
        if marker not in main_db_text:
            violations.append(f"{relative(MAIN_DB)} omits marker: {marker}")
    crdb_service_text = CRDB_SERVICE.read_text(encoding="utf-8")
    if (
            "new MainDB(GenresCollection.getInstance("
            "getApplicationContext()))" not in crdb_service_text):
        violations.append(
            f"{relative(CRDB_SERVICE)} does not assemble MainDB dependencies")

    file_info_text = FILE_INFO.read_text(encoding="utf-8")
    if re.search(r"\bServices\.", file_info_text):
        violations.append(
            f"{relative(FILE_INFO)} still uses the static service locator")
    if "Engine.getArchiveItems(arcname)" not in file_info_text:
        violations.append(
            f"{relative(FILE_INFO)} does not use the stateless archive API")
    if "public static ArrayList<ZipEntry> getArchiveItems" not in engine_text:
        violations.append(
            f"{relative(ENGINE)} archive enumeration is instance-bound")

    document_cache_text = DOCUMENT_FILE_CACHE.read_text(encoding="utf-8")
    if "DocumentFileCache(Activity" in document_cache_text:
        violations.append(
            f"{relative(DOCUMENT_FILE_CACHE)} requires an Activity context")
    if "public DocumentFileCache(Context context)" not in document_cache_text:
        violations.append(
            f"{relative(DOCUMENT_FILE_CACHE)} omits its Context constructor")

    validator_text = EXTERNAL_DOCUMENT_VALIDATOR.read_text(
        encoding="utf-8")
    for marker in (
        "public final class ExternalDocumentValidator",
        "public DocumentSource validate(",
        "DocumentFormatDetector.resolve(",
    ):
        if marker not in validator_text:
            violations.append(
                f"{relative(EXTERNAL_DOCUMENT_VALIDATOR)} omits marker: "
                f"{marker}")

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

    for marker in (
        "private final ToastView mToastView = new ToastView()",
        "mToastView.close()",
        "private final Services mServices = new Services()",
        "mServiceDependencies = mServices.startServices(this)",
        "public final ServiceDependencies getServiceDependencies()",
        "mServices.stopServices(this)",
    ):
        if marker not in base_text:
            violations.append(
                f"{relative(BASE_ACTIVITY)} omits lifecycle marker: {marker}")

    cool_reader_text = COOL_READER.read_text(encoding="utf-8")
    for marker in REQUIRED_COOL_READER_MARKERS:
        if marker not in cool_reader_text:
            violations.append(
                f"{relative(COOL_READER)} omits marker: {marker}")
    if "ServiceDependencies dependencies = getServiceDependencies()" not in (
            cool_reader_text):
        violations.append(
            f"{relative(COOL_READER)} does not capture its service generation")
    if "mExternalDocumentValidator.validate(" not in cool_reader_text:
        violations.append(
            f"{relative(COOL_READER)} does not delegate external source "
            "validation")
    if "stopServices();" not in cool_reader_text:
        violations.append(
            f"{relative(COOL_READER)} does not stop its owned service graph")
    find_pattern(
        COOL_READER,
        cool_reader_text,
        r"\b(?:private\s+)?static\s+(?:final\s+)?Debug\.MemoryInfo\b",
        "shares a mutable heap diagnostic snapshot across calls",
        violations)
    find_pattern(
        COOL_READER,
        cool_reader_text,
        r"\b(?:private\s+)?static\s+(?:final\s+)?Field\[\]\s+\w+\b",
        "retains reflection results as mutable Activity static state",
        violations)
    if "final Debug.MemoryInfo info = new Debug.MemoryInfo()" not in (
            cool_reader_text):
        violations.append(
            f"{relative(COOL_READER)} does not scope heap diagnostics "
            "to one invocation")

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
