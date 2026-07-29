#!/usr/bin/env python3
"""Keep active Android result and permission flows on lifecycle-aware APIs."""

from __future__ import annotations

import re
from pathlib import Path


ROOT = Path(__file__).resolve().parents[1]
SOURCE = ROOT / "android" / "src" / "org" / "coolreader"
COOL_READER = SOURCE / "CoolReader.java"
DICTIONARIES = SOURCE / "Dictionaries.java"
DICTIONARY_CATALOG = SOURCE / "DictionaryCatalog.java"
BASE_ACTIVITY = SOURCE / "crengine" / "BaseActivity.java"
APP_LOCALE_SELECTION = SOURCE / "crengine" / "AppLocaleSelection.java"
SETTINGS = SOURCE / "crengine" / "Settings.java"
PROFILE_SETTINGS_FILTER = (
    SOURCE / "crengine" / "ProfileSettingsFilter.java"
)
PROFILE_SETTINGS_FILTER_TEST = (
    ROOT
    / "android"
    / "app"
    / "src"
    / "test"
    / "java"
    / "org"
    / "coolreader"
    / "crengine"
    / "ProfileSettingsFilterTest.java"
)
SETTINGS_FILE_STORE = (
    SOURCE / "crengine" / "SettingsFileStore.java"
)
SETTINGS_FILE_STORE_TEST = (
    ROOT
    / "android"
    / "app"
    / "src"
    / "test"
    / "java"
    / "org"
    / "coolreader"
    / "crengine"
    / "SettingsFileStoreTest.java"
)
AUDIOBOOK_TIMING_CACHE = (
    SOURCE / "crengine" / "AudiobookTimingCache.java"
)
AUDIOBOOK_TIMING_CACHE_TEST = (
    ROOT
    / "android"
    / "app"
    / "src"
    / "test"
    / "java"
    / "org"
    / "coolreader"
    / "crengine"
    / "AudiobookTimingCacheTest.java"
)
WORD_TIMING_AUDIOBOOK_MATCHER = (
    SOURCE / "crengine" / "WordTimingAudiobookMatcher.java"
)
WORD_TIMING_AUDIOBOOK_MATCHER_TEST = (
    ROOT
    / "android"
    / "app"
    / "src"
    / "test"
    / "java"
    / "org"
    / "coolreader"
    / "crengine"
    / "WordTimingAudiobookMatcherTest.java"
)
READER_ACTION = SOURCE / "crengine" / "ReaderAction.java"
ACTION_ICON_SET = SOURCE / "crengine" / "ActionIconSet.java"
DEFAULT_INPUT_ACTIONS = SOURCE / "crengine" / "DefaultInputActions.java"
CR_TOOLBAR = SOURCE / "crengine" / "CRToolBar.java"
NOOK_CONTROLLER = SOURCE / "crengine" / "N2EpdController.java"
NOOK_CONTROLLER_BINDINGS = (
    SOURCE / "crengine" / "NookEpdControllerBindings.java"
)
ENGINE = SOURCE / "crengine" / "Engine.java"
BACKGROUND_TEXTURE_INFO = (
    SOURCE / "crengine" / "BackgroundTextureInfo.java"
)
BACKGROUND_TEXTURE_CATALOG = (
    SOURCE / "crengine" / "BackgroundTextureCatalog.java"
)
BACKGROUND_TEXTURE_CATALOG_TEST = (
    ROOT
    / "android"
    / "app"
    / "src"
    / "test"
    / "java"
    / "org"
    / "coolreader"
    / "crengine"
    / "BackgroundTextureCatalogTest.java"
)
DOCUMENT_FORMAT = SOURCE / "crengine" / "DocumentFormat.java"
DOCUMENT_FORMAT_TEST = (
    ROOT
    / "android"
    / "app"
    / "src"
    / "test"
    / "java"
    / "org"
    / "coolreader"
    / "crengine"
    / "DocumentFormatTest.java"
)
SERVICES = SOURCE / "crengine" / "Services.java"
SERVICE_DEPENDENCIES = SOURCE / "crengine" / "ServiceDependencies.java"
TOAST_VIEW = SOURCE / "crengine" / "ToastView.java"
SCANNER = SOURCE / "crengine" / "Scanner.java"
READER_VIEW = SOURCE / "crengine" / "ReaderView.java"
TTS_TOOLBAR = SOURCE / "crengine" / "TTSToolbarDlg.java"
MOTION_WATCHDOG = (
    ROOT
    / "android"
    / "src"
    / "com"
    / "s_trace"
    / "motion_watchdog"
    / "MotionWatchdogHandler.java"
)
MOTION_WATCHDOG_FADE_STATE = (
    ROOT
    / "android"
    / "src"
    / "com"
    / "s_trace"
    / "motion_watchdog"
    / "MotionWatchdogFadeState.java"
)
MOTION_WATCHDOG_FADE_STATE_TEST = (
    ROOT
    / "android"
    / "app"
    / "src"
    / "test"
    / "java"
    / "com"
    / "s_trace"
    / "motion_watchdog"
    / "MotionWatchdogFadeStateTest.java"
)
REPEAT_ON_TOUCH_LISTENER = (
    SOURCE / "crengine" / "RepeatOnTouchListener.java"
)
GESTURE_ACCELERATION = SOURCE / "crengine" / "GestureAcceleration.java"
ANIMATION_TIMING = SOURCE / "crengine" / "AnimationTiming.java"
AUTO_SCROLL_SESSION_STATE = (
    SOURCE / "crengine" / "AutoScrollSessionState.java"
)
READING_TIME_TRACKER = SOURCE / "crengine" / "ReadingTimeTracker.java"
READING_TIME_FORMATTER = SOURCE / "crengine" / "ReadingTimeFormatter.java"
BATTERY_STATUS = SOURCE / "crengine" / "BatteryStatus.java"
READER_PROGRESS_STATE = (
    SOURCE / "crengine" / "ReaderProgressState.java"
)
PROGRESS_DIALOG = SOURCE / "crengine" / "ProgressDialog.java"
PROGRESS_DISPLAY_STATE = (
    SOURCE / "crengine" / "ProgressDisplayState.java"
)
PROGRESS_UI_STATE = (
    SOURCE / "crengine" / "ProgressUiState.java"
)
FONT_FACE_SWITCHER = SOURCE / "crengine" / "FontFaceSwitcher.java"
VM_RUNTIME_HACK = SOURCE / "crengine" / "VMRuntimeHack.java"
BITMAP_MEMORY_ACCOUNTING = (
    SOURCE / "crengine" / "BitmapMemoryAccounting.java"
)
PAGE_FLIP_GEOMETRY = SOURCE / "crengine" / "PageFlipGeometry.java"
TAP_ZONE_GEOMETRY = SOURCE / "crengine" / "TapZoneGeometry.java"
TAP_HIGHLIGHT_STATE = (
    SOURCE / "crengine" / "TapHighlightState.java"
)
VIEWPORT_RESIZE_STATE = (
    SOURCE / "crengine" / "ViewportResizeState.java"
)
POSITION_PROPERTIES = SOURCE / "crengine" / "PositionProperties.java"
DOCUMENT_POSITION_POLICY = (
    SOURCE / "crengine" / "DocumentPositionPolicy.java"
)
EINK_REFRESH_LEASE_TRACKER = (
    SOURCE / "crengine" / "EinkRefreshLeaseTracker.java"
)
PAGE_CURVE_TABLES = SOURCE / "crengine" / "PageCurveTables.java"
BACKLIGHT_OPTIONS = SOURCE / "crengine" / "BacklightOptions.java"
BACKLIGHT_TIMEOUT_POLICY = (
    SOURCE / "crengine" / "BacklightTimeoutPolicy.java"
)
FEED_TIMESTAMP_PARSER = (
    SOURCE / "crengine" / "FeedTimestampParser.java"
)
BACKGROUND_THREAD = SOURCE / "crengine" / "BackgroundThread.java"
DEFERRED_TASK_QUEUE = SOURCE / "crengine" / "DeferredTaskQueue.java"
DELAYED_EXECUTOR = SOURCE / "crengine" / "DelayedExecutor.java"
REPLACEABLE_TASK_SLOT = (
    SOURCE / "crengine" / "ReplaceableTaskSlot.java"
)
CLOSEABLE_TASK_GATE = (
    SOURCE / "crengine" / "CloseableTaskGate.java"
)
BLOCKING_RESULT = SOURCE / "crengine" / "BlockingResult.java"
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
TAP_ZONE_GEOMETRY_TEST = (
    ROOT
    / "android"
    / "app"
    / "src"
    / "test"
    / "java"
    / "org"
    / "coolreader"
    / "crengine"
    / "TapZoneGeometryTest.java"
)
TAP_HIGHLIGHT_STATE_TEST = (
    ROOT
    / "android"
    / "app"
    / "src"
    / "test"
    / "java"
    / "org"
    / "coolreader"
    / "crengine"
    / "TapHighlightStateTest.java"
)
VIEWPORT_RESIZE_STATE_TEST = (
    ROOT
    / "android"
    / "app"
    / "src"
    / "test"
    / "java"
    / "org"
    / "coolreader"
    / "crengine"
    / "ViewportResizeStateTest.java"
)
POSITION_PROPERTIES_TEST = (
    ROOT
    / "android"
    / "app"
    / "src"
    / "test"
    / "java"
    / "org"
    / "coolreader"
    / "crengine"
    / "PositionPropertiesTest.java"
)
DOCUMENT_POSITION_POLICY_TEST = (
    ROOT
    / "android"
    / "app"
    / "src"
    / "test"
    / "java"
    / "org"
    / "coolreader"
    / "crengine"
    / "DocumentPositionPolicyTest.java"
)
EINK_REFRESH_LEASE_TRACKER_TEST = (
    ROOT
    / "android"
    / "app"
    / "src"
    / "test"
    / "java"
    / "org"
    / "coolreader"
    / "crengine"
    / "EinkRefreshLeaseTrackerTest.java"
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
REPLACEABLE_TASK_SLOT_TEST = (
    ROOT
    / "android"
    / "app"
    / "src"
    / "test"
    / "java"
    / "org"
    / "coolreader"
    / "crengine"
    / "ReplaceableTaskSlotTest.java"
)
CLOSEABLE_TASK_GATE_TEST = (
    ROOT
    / "android"
    / "app"
    / "src"
    / "test"
    / "java"
    / "org"
    / "coolreader"
    / "crengine"
    / "CloseableTaskGateTest.java"
)
BLOCKING_RESULT_TEST = (
    ROOT
    / "android"
    / "app"
    / "src"
    / "test"
    / "java"
    / "org"
    / "coolreader"
    / "crengine"
    / "BlockingResultTest.java"
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
BACKLIGHT_OPTIONS_TEST = (
    ROOT
    / "android"
    / "app"
    / "src"
    / "test"
    / "java"
    / "org"
    / "coolreader"
    / "crengine"
    / "BacklightOptionsTest.java"
)
BACKLIGHT_TIMEOUT_POLICY_TEST = (
    ROOT
    / "android"
    / "app"
    / "src"
    / "test"
    / "java"
    / "org"
    / "coolreader"
    / "crengine"
    / "BacklightTimeoutPolicyTest.java"
)
FEED_TIMESTAMP_PARSER_TEST = (
    ROOT
    / "android"
    / "app"
    / "src"
    / "test"
    / "java"
    / "org"
    / "coolreader"
    / "crengine"
    / "FeedTimestampParserTest.java"
)
NOOK_CONTROLLER_BINDINGS_TEST = (
    ROOT
    / "android"
    / "app"
    / "src"
    / "test"
    / "java"
    / "org"
    / "coolreader"
    / "crengine"
    / "NookEpdControllerBindingsTest.java"
)
VM_RUNTIME_HACK_TEST = (
    ROOT
    / "android"
    / "app"
    / "src"
    / "test"
    / "java"
    / "org"
    / "coolreader"
    / "crengine"
    / "VMRuntimeHackTest.java"
)
BITMAP_MEMORY_ACCOUNTING_TEST = (
    ROOT
    / "android"
    / "app"
    / "src"
    / "test"
    / "java"
    / "org"
    / "coolreader"
    / "crengine"
    / "BitmapMemoryAccountingTest.java"
)
BATTERY_STATUS_TEST = (
    ROOT
    / "android"
    / "app"
    / "src"
    / "test"
    / "java"
    / "org"
    / "coolreader"
    / "crengine"
    / "BatteryStatusTest.java"
)
READER_PROGRESS_STATE_TEST = (
    ROOT
    / "android"
    / "app"
    / "src"
    / "test"
    / "java"
    / "org"
    / "coolreader"
    / "crengine"
    / "ReaderProgressStateTest.java"
)
PROGRESS_DISPLAY_STATE_TEST = (
    ROOT
    / "android"
    / "app"
    / "src"
    / "test"
    / "java"
    / "org"
    / "coolreader"
    / "crengine"
    / "ProgressDisplayStateTest.java"
)
PROGRESS_UI_STATE_TEST = (
    ROOT
    / "android"
    / "app"
    / "src"
    / "test"
    / "java"
    / "org"
    / "coolreader"
    / "crengine"
    / "ProgressUiStateTest.java"
)
FONT_FACE_SWITCHER_TEST = (
    ROOT
    / "android"
    / "app"
    / "src"
    / "test"
    / "java"
    / "org"
    / "coolreader"
    / "crengine"
    / "FontFaceSwitcherTest.java"
)
ACTION_ICON_SET_TEST = (
    ROOT
    / "android"
    / "app"
    / "src"
    / "test"
    / "java"
    / "org"
    / "coolreader"
    / "crengine"
    / "ActionIconSetTest.java"
)
DEFAULT_INPUT_ACTIONS_TEST = (
    ROOT
    / "android"
    / "app"
    / "src"
    / "test"
    / "java"
    / "org"
    / "coolreader"
    / "crengine"
    / "DefaultInputActionsTest.java"
)
GESTURE_ACCELERATION_TEST = (
    ROOT
    / "android"
    / "app"
    / "src"
    / "test"
    / "java"
    / "org"
    / "coolreader"
    / "crengine"
    / "GestureAccelerationTest.java"
)
ANIMATION_TIMING_TEST = (
    ROOT
    / "android"
    / "app"
    / "src"
    / "test"
    / "java"
    / "org"
    / "coolreader"
    / "crengine"
    / "AnimationTimingTest.java"
)
AUTO_SCROLL_SESSION_STATE_TEST = (
    ROOT
    / "android"
    / "app"
    / "src"
    / "test"
    / "java"
    / "org"
    / "coolreader"
    / "crengine"
    / "AutoScrollSessionStateTest.java"
)
READING_TIME_TRACKER_TEST = (
    ROOT
    / "android"
    / "app"
    / "src"
    / "test"
    / "java"
    / "org"
    / "coolreader"
    / "crengine"
    / "ReadingTimeTrackerTest.java"
)
READING_TIME_FORMATTER_TEST = (
    ROOT
    / "android"
    / "app"
    / "src"
    / "test"
    / "java"
    / "org"
    / "coolreader"
    / "crengine"
    / "ReadingTimeFormatterTest.java"
)
DICTIONARY_CATALOG_TEST = (
    ROOT
    / "android"
    / "app"
    / "src"
    / "test"
    / "java"
    / "org"
    / "coolreader"
    / "DictionaryCatalogTest.java"
)
APP_LOCALE_SELECTION_TEST = (
    ROOT
    / "android"
    / "app"
    / "src"
    / "test"
    / "java"
    / "org"
    / "coolreader"
    / "crengine"
    / "AppLocaleSelectionTest.java"
)
FILE_BROWSER = SOURCE / "crengine" / "FileBrowser.java"
ROOT_VIEW = SOURCE / "crengine" / "CRRootView.java"
COVERPAGE_MANAGER = SOURCE / "crengine" / "CoverpageManager.java"
FILE_SYSTEM_FOLDERS = SOURCE / "crengine" / "FileSystemFolders.java"
UTILS = SOURCE / "crengine" / "Utils.java"
AUDIO_FILE_SELECTOR = SOURCE / "crengine" / "AudioFileSelector.java"
FILE_NAME_TRANSCRIBER = SOURCE / "crengine" / "FileNameTranscriber.java"
AUDIO_FILE_SELECTOR_TEST = (
    ROOT
    / "android"
    / "app"
    / "src"
    / "test"
    / "java"
    / "org"
    / "coolreader"
    / "crengine"
    / "AudioFileSelectorTest.java"
)
FILE_NAME_TRANSCRIBER_TEST = (
    ROOT
    / "android"
    / "app"
    / "src"
    / "test"
    / "java"
    / "org"
    / "coolreader"
    / "crengine"
    / "FileNameTranscriberTest.java"
)
WORD_TIMING_AUDIOBOOK_MATCHER = (
    SOURCE / "crengine" / "WordTimingAudiobookMatcher.java"
)
TTS_CONTROL_SERVICE = SOURCE / "tts" / "TTSControlService.java"
ABOUT_DIALOG = SOURCE / "crengine" / "AboutDialog.java"
OPTIONS_DIALOG = SOURCE / "crengine" / "OptionsDialog.java"
INTERFACE_THEME = SOURCE / "crengine" / "InterfaceTheme.java"
INTERFACE_THEME_CATALOG = (
    SOURCE / "crengine" / "InterfaceThemeCatalog.java"
)
INTERFACE_THEME_TEST = (
    ROOT
    / "android"
    / "app"
    / "src"
    / "test"
    / "java"
    / "org"
    / "coolreader"
    / "crengine"
    / "InterfaceThemeTest.java"
)
STYLE_OPTION_CATALOG = SOURCE / "crengine" / "StyleOptionCatalog.java"
STYLE_OPTION_CATALOG_TEST = (
    ROOT
    / "android"
    / "app"
    / "src"
    / "test"
    / "java"
    / "org"
    / "coolreader"
    / "crengine"
    / "StyleOptionCatalogTest.java"
)
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
    for marker in (
        "private final ScreenBacklightControl backlightControl",
        "private Runnable backlightTimerTask",
        "private long lastUserActivityTime",
        "BacklightTimeoutPolicy.nextCheckDelay(",
        "BacklightTimeoutPolicy.shouldDim(",
        "BacklightTimeoutPolicy.isExpired(",
    ):
        if marker not in base_text:
            violations.append(
                f"{relative(BASE_ACTIVITY)} omits backlight lifecycle "
                f"marker: {marker}")
    if "private static long lastUserActivityTime" in base_text:
        violations.append(
            f"{relative(BASE_ACTIVITY)} retains process-wide user activity")
    for marker in (
        "private ActionIconSet actionIcons = ActionIconSet.empty()",
        "int getActionIconId(ReaderAction action)",
        "actionIcons = ActionIconSet.builder()",
        "private final DefaultInputActions defaultInputActions",
        "DefaultInputActions.create(",
        "defaultInputActions.applyTo(",
    ):
        if marker not in base_text:
            violations.append(
                f"{relative(BASE_ACTIVITY)} omits Activity-owned action "
                f"marker: {marker}")
    for legacy in (
        "DEF_KEY_ACTIONS",
        "DEF_NOOK_KEY_ACTIONS",
        "DEF_TAP_ACTIONS",
        ".setIconId(",
    ):
        if legacy in base_text:
            violations.append(
                f"{relative(BASE_ACTIVITY)} retains shared action state: "
                f"{legacy}")
    for marker in (
        "protected static final String PREF_FILE",
        "protected static final String PREF_LAST_BOOK",
        "protected static final String PREF_LAST_LOCATION",
        "protected static final String PREF_LAST_NOTIFICATION_MASK",
        "protected static final String PREF_LAST_LOGCAT",
        "private static final String PREF_HELP_FILE",
        "private static final boolean DEBUG_RESET_OPTIONS",
        "private final Locale systemLocale = currentSystemLocale()",
        "Resources.getSystem().getConfiguration()",
        "configuration.getLocales().get(0)",
        "AppLocaleSelection.resolve(lang, systemLocale)",
    ):
        if marker not in base_text:
            violations.append(
                f"{relative(BASE_ACTIVITY)} omits immutable locale/config "
                f"marker: {marker}")
    for legacy in (
        "protected static String PREF_",
        "private static String PREF_HELP_FILE",
        "private static boolean DEBUG_RESET_OPTIONS",
        "static final Locale defaultLocale",
    ):
        if legacy in base_text:
            violations.append(
                f"{relative(BASE_ACTIVITY)} retains mutable/process config: "
                f"{legacy}")
    for marker in (
        "private final InterfaceThemeCatalog interfaceThemes",
        "InterfaceThemeCatalog.create(DeviceInfo.EINK_SCREEN)",
        "InterfaceThemeCatalog getInterfaceThemes()",
        "interfaceThemes.findByCode(themeCode)",
    ):
        if marker not in base_text:
            violations.append(
                f"{relative(BASE_ACTIVITY)} omits Activity-owned theme "
                f"marker: {marker}")
    for marker in (
        "private final ProfileSettingsFilter profileSettingsFilter",
        "ProfileSettingsFilter.legacy()",
        "res = profileSettingsFilter.filter(res)",
        "settings = profileSettingsFilter.filter(settings)",
    ):
        if marker not in base_text:
            violations.append(
                f"{relative(BASE_ACTIVITY)} omits profile filter marker: "
                f"{marker}")
    for legacy in (
        "filterProfileSettings(",
        "Settings.PROFILE_SETTINGS",
    ):
        if legacy in base_text:
            violations.append(
                f"{relative(BASE_ACTIVITY)} retains inline profile rules: "
                f"{legacy}")
    for marker in (
        "private final SettingsFileStore settingsFileStore",
        "new SettingsFileStore()",
        "settingsFileStore.save(f, settings)",
    ):
        if marker not in base_text:
            violations.append(
                f"{relative(BASE_ACTIVITY)} omits settings store marker: "
                f"{marker}")
    for legacy in (
        "saveSettingsTask",
        "FileOutputStream os = new FileOutputStream(f)",
        "settings.store(os,",
    ):
        if legacy in base_text:
            violations.append(
                f"{relative(BASE_ACTIVITY)} retains inline/dead settings "
                f"persistence marker: {legacy}")

    settings_text = SETTINGS.read_text(encoding="utf-8")
    if "PROFILE_SETTINGS" in settings_text:
        violations.append(
            f"{relative(SETTINGS)} exposes mutable profile rule storage")

    profile_settings_filter_text = PROFILE_SETTINGS_FILTER.read_text(
        encoding="utf-8")
    for marker in (
        "final class ProfileSettingsFilter",
        "private final List<String> patterns",
        "Collections.unmodifiableList(copy)",
        "static ProfileSettingsFilter legacy()",
        "boolean includes(String key)",
        'key.startsWith("styles.")',
        "pattern.equalsIgnoreCase(key)",
        "Properties filter(Properties settings)",
        "synchronized (settings)",
    ):
        if marker not in profile_settings_filter_text:
            violations.append(
                f"{relative(PROFILE_SETTINGS_FILTER)} omits immutable profile "
                f"filter marker: {marker}")

    profile_settings_filter_test_text = (
        PROFILE_SETTINGS_FILTER_TEST.read_text(encoding="utf-8")
    )
    for marker in (
        "legacyPatternsPreserveOrderAndValues",
        "exactAndPrefixMatchingPreserveLegacyCaseRules",
        "filteringCopiesOnlyProfileSettings",
        "patternStorageIsCopiedAndUnmodifiable",
        "invalidDefinitionsAndInputAreRejected",
        '"app.ui.theme*"',
    ):
        if marker not in profile_settings_filter_test_text:
            violations.append(
                f"{relative(PROFILE_SETTINGS_FILTER_TEST)} omits profile "
                f"filter regression: {marker}")

    settings_store_text = SETTINGS_FILE_STORE.read_text(
        encoding="utf-8")
    for marker in (
        "final class SettingsFileStore",
        "void save(File target, Properties settings) throws IOException",
        'throw new IllegalArgumentException(',
        '"target must not be null"',
        '"settings must not be null"',
        "try (FileOutputStream output =",
        'settings.store(output, "Cool Reader 3 settings")',
    ):
        if marker not in settings_store_text:
            violations.append(
                f"{relative(SETTINGS_FILE_STORE)} omits scoped settings "
                f"persistence marker: {marker}")

    settings_store_test_text = SETTINGS_FILE_STORE_TEST.read_text(
        encoding="utf-8")
    for marker in (
        "saveRoundTripsProperties",
        "saveTruncatesThePreviousSnapshot",
        "nullSnapshotCannotTruncateExistingSettings",
        'store.save(target, null)',
        '"preserved"',
    ):
        if marker not in settings_store_test_text:
            violations.append(
                f"{relative(SETTINGS_FILE_STORE_TEST)} omits settings "
                f"persistence regression: {marker}")

    audiobook_cache_text = AUDIOBOOK_TIMING_CACHE.read_text(
        encoding="utf-8")
    for marker in (
        "final class AudiobookTimingCache",
        "List<Entry> read(File source) throws IOException",
        "void write(File target, List<Entry> entries)",
        "try (BufferedReader reader =",
        "return Collections.unmodifiableList(entries)",
        "List<Entry> snapshot = new ArrayList<>(entries)",
        "try (FileWriter writer = new FileWriter(target))",
        "String[] columns = line.split(\",\", 6)",
        "static final class Entry",
        "private final double totalBookDuration",
        "Double.isNaN(value)",
        "Double.isInfinite(value)",
    ):
        if marker not in audiobook_cache_text:
            violations.append(
                f"{relative(AUDIOBOOK_TIMING_CACHE)} omits strict "
                f"audiobook cache marker: {marker}")

    audiobook_cache_test_text = (
        AUDIOBOOK_TIMING_CACHE_TEST.read_text(encoding="utf-8")
    )
    for marker in (
        "entriesRoundTripWithoutMutableListEscape",
        "malformedValuesAreRejectedWithLineNumber",
        "invalidSnapshotCannotTruncateExistingCache",
        '"part,2.mp3"',
        '"p1,NaN,0,1,true,a.mp3',
        "restored.clear()",
    ):
        if marker not in audiobook_cache_test_text:
            violations.append(
                f"{relative(AUDIOBOOK_TIMING_CACHE_TEST)} omits "
                f"audiobook cache regression: {marker}")

    word_timing_matcher_text = (
        WORD_TIMING_AUDIOBOOK_MATCHER.read_text(encoding="utf-8")
    )
    for marker in (
        "private final AudiobookTimingCache timingCache",
        "try (BufferedReader br =",
        "timingCache.read(sentenceTimingCacheFile)",
        "Map<String, AudiobookTimingCache.Entry> pending",
        "pending.size() != sentencesByStartPos.size()",
        "Map<SentenceInfo, SentenceTiming> resolved",
        "entry.getKey().sentenceTiming = entry.getValue()",
        "timingCache.write(sentenceTimingCacheFile, entries)",
        "finally {",
        "retriever.release()",
    ):
        if marker not in word_timing_matcher_text:
            violations.append(
                f"{relative(WORD_TIMING_AUDIOBOOK_MATCHER)} omits "
                f"atomic timing publication marker: {marker}")
    for legacy in (
        "parseSentenceTimingLine(",
        "new FileWriter(sentenceTimingCacheFile)",
        "br.close()",
        "fw.close()",
    ):
        if legacy in word_timing_matcher_text:
            violations.append(
                f"{relative(WORD_TIMING_AUDIOBOOK_MATCHER)} retains "
                f"incremental/unscoped timing cache marker: {legacy}")

    word_timing_matcher_test_text = (
        WORD_TIMING_AUDIOBOOK_MATCHER_TEST.read_text(
            encoding="utf-8")
    )
    for marker in (
        "completeCachePublishesOneAtomicTimingSnapshot",
        "incompleteCacheCannotPartiallyReplaceTimings",
        "unknownOrDuplicatePositionRejectsWholeCache",
        "publishedSnapshotCanBeWrittenAndReadAgain",
        "assertSame(oldFirst, first.sentenceTiming)",
    ):
        if marker not in word_timing_matcher_test_text:
            violations.append(
                f"{relative(WORD_TIMING_AUDIOBOOK_MATCHER_TEST)} omits "
                f"atomic timing regression: {marker}")

    app_locale_text = APP_LOCALE_SELECTION.read_text(encoding="utf-8")
    for marker in (
        "final class AppLocaleSelection",
        "private final Locale locale",
        "private final String code",
        "if (language == Settings.Lang.DEFAULT)",
        "Settings.Lang.getCode(systemLocale)",
        "Locale locale = language.getLocale()",
    ):
        if marker not in app_locale_text:
            violations.append(
                f"{relative(APP_LOCALE_SELECTION)} omits immutable locale "
                f"marker: {marker}")

    app_locale_test_text = APP_LOCALE_SELECTION_TEST.read_text(
        encoding="utf-8")
    for marker in (
        "systemSettingUsesCurrentActivitySnapshot",
        "explicitSettingIgnoresSystemLocale",
        "missingInputsAreRejected",
        '"pt_BR"',
        "Settings.Lang.UK",
    ):
        if marker not in app_locale_test_text:
            violations.append(
                f"{relative(APP_LOCALE_SELECTION_TEST)} omits locale "
                f"regression: {marker}")

    reader_action_text = READER_ACTION.read_text(encoding="utf-8")
    for marker in (
        "final public int iconId",
        "private final boolean canRepeat",
        "private final boolean mayAssignOnTap",
        "private ReaderAction withIcon(int iconId)",
        "private final static List<ReaderAction> ASSIGNABLE_ACTIONS",
        "static ReaderAction[] availableActions(",
        "Collections.unmodifiableList(",
        "switch (type)",
    ):
        if marker not in reader_action_text:
            violations.append(
                f"{relative(READER_ACTION)} omits immutable action marker: "
                f"{marker}")
    for legacy in (
        "TYPE_PROP_SUBPATH",
        "public final static ReaderAction[] AVAILABLE_ACTIONS",
        "public ReaderAction setIconId(",
    ):
        if legacy in reader_action_text:
            violations.append(
                f"{relative(READER_ACTION)} exposes mutable action state: "
                f"{legacy}")

    action_icon_text = ACTION_ICON_SET.read_text(encoding="utf-8")
    for marker in (
        "final class ActionIconSet",
        "private final Map<String, Integer> overrides",
        "Collections.unmodifiableMap(",
        "new HashMap<>(overrides)",
        "return override != null ? override : action.iconId",
    ):
        if marker not in action_icon_text:
            violations.append(
                f"{relative(ACTION_ICON_SET)} omits immutable icon marker: "
                f"{marker}")

    default_input_text = DEFAULT_INPUT_ACTIONS.read_text(encoding="utf-8")
    for marker in (
        "final class DefaultInputActions",
        "private final List<KeyAction> keyActions",
        "private final List<KeyAction> nookKeyActions",
        "private final List<TapAction> tapActions",
        "Set<String> userNookMappings = new HashSet<>()",
        "if (!userNookMappings.contains(action.propertyName))",
        "boolean hasAvailableMenuKey(boolean hasHardwareMenuKey)",
        "boolean hasMenuTap(java.util.Properties properties)",
    ):
        if marker not in default_input_text:
            violations.append(
                f"{relative(DEFAULT_INPUT_ACTIONS)} omits input default "
                f"marker: {marker}")

    toolbar_text = CR_TOOLBAR.read_text(encoding="utf-8")
    if "activity.getActionIconId(" not in toolbar_text:
        violations.append(
            f"{relative(CR_TOOLBAR)} does not resolve Activity-owned icons")
    if re.search(r"\b(?:item|action)\.iconId\b", toolbar_text):
        violations.append(
            f"{relative(CR_TOOLBAR)} bypasses the Activity icon snapshot")

    action_icon_test_text = ACTION_ICON_SET_TEST.read_text(encoding="utf-8")
    for marker in (
        "activitySnapshotsRemainIndependent",
        "availableActionsCannotBeReplacedThroughReturnedArray",
        "deviceSpecificActionIsAddedOnlyToRequestedSnapshot",
        "actionTypePropertiesHaveNoMutableArrayBacking",
    ):
        if marker not in action_icon_test_text:
            violations.append(
                f"{relative(ACTION_ICON_SET_TEST)} omits action regression: "
                f"{marker}")

    default_input_test_text = DEFAULT_INPUT_ACTIONS_TEST.read_text(
        encoding="utf-8")
    for marker in (
        "deviceFlagsSelectLegacyNavigationDefaults",
        "nookDefaultsOverrideOnlyConflictingGeneratedDefaults",
        "inaccessibleMenuForcesOnlyCentralTapFallback",
        "existingMenuTapPreventsCentralOverride",
    ):
        if marker not in default_input_test_text:
            violations.append(
                f"{relative(DEFAULT_INPUT_ACTIONS_TEST)} omits input "
                f"regression: {marker}")

    dictionaries_text = DICTIONARIES.read_text(encoding="utf-8")
    for marker in (
        "private static final DictionaryCatalog DICTIONARY_CATALOG",
        "public static final class DictInfo",
        "public final String dataKey",
        "return DICTIONARY_CATALOG.findById(id)",
        "return DICTIONARY_CATALOG.snapshot()",
        "DICTIONARY_CATALOG.entries()",
    ):
        if marker not in dictionaries_text:
            violations.append(
                f"{relative(DICTIONARIES)} omits immutable dictionary "
                f"marker: {marker}")
    for legacy in (
        "static final DictInfo dicts[]",
        "setDataKey(",
        "return dicts;",
    ):
        if legacy in dictionaries_text:
            violations.append(
                f"{relative(DICTIONARIES)} exposes mutable dictionary "
                f"state: {legacy}")

    dictionary_catalog_text = DICTIONARY_CATALOG.read_text(
        encoding="utf-8")
    for marker in (
        "final class DictionaryCatalog",
        "private final List<DictInfo> entries",
        "Collections.unmodifiableList(copy)",
        "dictionary IDs must be non-empty and unique",
        "entries.toArray(new DictInfo[0])",
        '"OnyxDictWindowed"',
        '"Wikipedia"',
    ):
        if marker not in dictionary_catalog_text:
            violations.append(
                f"{relative(DICTIONARY_CATALOG)} omits immutable catalog "
                f"marker: {marker}")

    dictionary_catalog_test_text = DICTIONARY_CATALOG_TEST.read_text(
        encoding="utf-8")
    for marker in (
        "legacyCatalogPreservesEveryIntegrationDefinition",
        "publicArrayApiReturnsIndependentSnapshots",
        "catalogLookupAndEntryListAreStable",
        "definitionsAreImmutableAndDuplicateIdsAreRejected",
        "assertEquals(19, catalog.entries().size())",
    ):
        if marker not in dictionary_catalog_test_text:
            violations.append(
                f"{relative(DICTIONARY_CATALOG_TEST)} omits dictionary "
                f"regression: {marker}")

    backlight_timeout_policy_text = BACKLIGHT_TIMEOUT_POLICY.read_text(
        encoding="utf-8")
    for marker in (
        "final class BacklightTimeoutPolicy",
        "durationMillis * 8L / 10",
        "inactiveMillis > durationMillis",
        "Math.max(1, durationMillis / 20)",
    ):
        if marker not in backlight_timeout_policy_text:
            violations.append(
                f"{relative(BACKLIGHT_TIMEOUT_POLICY)} omits timeout "
                f"marker: {marker}")

    backlight_timeout_policy_test_text = (
        BACKLIGHT_TIMEOUT_POLICY_TEST.read_text(encoding="utf-8")
    )
    for marker in (
        "dimmingStartsStrictlyAfterEightyPercent",
        "expiryStartsStrictlyAfterFullDuration",
        "thresholdArithmeticDoesNotOverflow",
        "Integer.MAX_VALUE",
    ):
        if marker not in backlight_timeout_policy_test_text:
            violations.append(
                f"{relative(BACKLIGHT_TIMEOUT_POLICY_TEST)} omits timeout "
                f"regression: {marker}")

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
    for marker in (
        "private final NookEpdControllerBindings bindings",
        "bindings.createController(activity)",
        "bindings.setMode(mEpdController, region, wave, mode)",
    ):
        if marker not in nook_controller_text:
            violations.append(
                f"{relative(NOOK_CONTROLLER)} omits instance-owned EPD "
                f"marker: {marker}")
    if re.search(
            r"\bstatic\s+(?:final\s+)?"
            r"(?:Method|Constructor<\?>|Object\[\]|String)\s+\w+",
            nook_controller_text):
        violations.append(
            f"{relative(NOOK_CONTROLLER)} retains mutable vendor reflection "
            "state")

    nook_bindings_text = NOOK_CONTROLLER_BINDINGS.read_text(
        encoding="utf-8")
    for marker in (
        "final class NookEpdControllerBindings",
        "private final Method setRegion",
        "private final Constructor<?> regionParamsConstructor",
        "private final Constructor<?> controllerConstructor",
        "private final Object[] waves",
        "private final Object[] regions",
        "private final Object[] modes",
        "if (!enabled)",
        "return values.clone()",
    ):
        if marker not in nook_bindings_text:
            violations.append(
                f"{relative(NOOK_CONTROLLER_BINDINGS)} omits immutable EPD "
                f"marker: {marker}")

    nook_bindings_test_text = NOOK_CONTROLLER_BINDINGS_TEST.read_text(
        encoding="utf-8")
    for marker in (
        "disabledBindingDoesNotResolveVendorClasses",
        "legacyControllerInvokesStaticVendorMethod",
        "nook120ControllerKeepsHostAndInvokesInstanceMethod",
    ):
        if marker not in nook_bindings_test_text:
            violations.append(
                f"{relative(NOOK_CONTROLLER_BINDINGS_TEST)} omits EPD "
                f"regression: {marker}")

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
        "private static final BackgroundTextureCatalog BUILT_IN_TEXTURES",
        "public static final BackgroundTextureInfo NO_TEXTURE",
        "public List<BackgroundTextureInfo> getAvailableTextures()",
        "return BUILT_IN_TEXTURES.withExternal(external)",
        "BUILT_IN_TEXTURES.findById(id)",
    ):
        if marker not in engine_text:
            violations.append(f"{relative(ENGINE)} omits marker: {marker}")
    for marker in (
        "private static File[] mountedRootsList",
        "private static Map<String, String> mountedRootsMap",
        "private static MountPathCorrector pathCorrector",
        "private static String[] mFonts",
        "private static HyphDict[] values",
        "BackgroundTextureInfo[] internalTextures",
        "BackgroundTextureInfo[] getAvailableTextures",
    ):
        if marker in engine_text:
            violations.append(
                f"{relative(ENGINE)} retains mutable process snapshot "
                f"state: {marker}")

    background_texture_info_text = BACKGROUND_TEXTURE_INFO.read_text(
        encoding="utf-8")
    for marker in (
        "public final class BackgroundTextureInfo",
        "private final String id",
        "private final String name",
        "private final int resourceId",
        "private final boolean tiled",
        "public String getId()",
        "public int getResourceId()",
        "public boolean isTiled()",
    ):
        if marker not in background_texture_info_text:
            violations.append(
                f"{relative(BACKGROUND_TEXTURE_INFO)} omits immutable texture "
                f"marker: {marker}")

    background_texture_catalog_text = BACKGROUND_TEXTURE_CATALOG.read_text(
        encoding="utf-8")
    for marker in (
        "final class BackgroundTextureCatalog",
        "private final List<BackgroundTextureInfo> entries",
        "private final Map<String, BackgroundTextureInfo> entriesById",
        "Collections.unmodifiableList(entryCopy)",
        "Collections.unmodifiableMap(index)",
        "static BackgroundTextureCatalog legacy()",
        "BackgroundTextureInfo findById(String id)",
        "List<BackgroundTextureInfo> withExternal(",
        "result.add(none())",
        "result.addAll(entries.subList(1, entries.size()))",
        "return Collections.unmodifiableList(result)",
        '"bg_paper1"',
        '"tx_stones_dark"',
    ):
        if marker not in background_texture_catalog_text:
            violations.append(
                f"{relative(BACKGROUND_TEXTURE_CATALOG)} omits immutable "
                f"texture catalog marker: {marker}")

    background_texture_catalog_test_text = (
        BACKGROUND_TEXTURE_CATALOG_TEST.read_text(encoding="utf-8")
    )
    for marker in (
        "legacyCatalogPreservesOrderLookupAndResources",
        "catalogAndMetadataHaveImmutablePrivateStorage",
        "externalTexturesRemainBetweenNoneAndBuiltIns",
        "invalidCatalogsAndTextureIdsAreRejected",
        "externalFileRecognitionPreservesLegacyRules",
        "assertEquals(34, combined.size())",
        "R.drawable.tx_stones_dark",
    ):
        if marker not in background_texture_catalog_test_text:
            violations.append(
                f"{relative(BACKGROUND_TEXTURE_CATALOG_TEST)} omits texture "
                f"regression: {marker}")

    document_format_text = DOCUMENT_FORMAT.read_text(encoding="utf-8")
    for marker in (
        "private final String[] extensions",
        "private final String[] mimeFormats",
        "this.extensions = extensions.clone()",
        "this.mimeFormats = mimeFormats.clone()",
        "return extensions.clone()",
        "return mimeFormats.clone()",
        "public String getPrimaryExtension()",
    ):
        if marker not in document_format_text:
            violations.append(
                f"{relative(DOCUMENT_FORMAT)} omits immutable format "
                f"metadata marker: {marker}")
    for legacy in (
        "return extensions;",
        "return mimeFormats;",
    ):
        if legacy in document_format_text:
            violations.append(
                f"{relative(DOCUMENT_FORMAT)} exposes mutable format "
                f"metadata: {legacy}")
    for field_name in (
            "canParseProperties",
            "canParseCoverpages"):
        if re.search(
                rf"^\s*final\s+boolean\s+{field_name}\b",
                document_format_text,
                re.MULTILINE):
            violations.append(
                f"{relative(DOCUMENT_FORMAT)} exposes package metadata field: "
                f"{field_name}")

    document_format_test_text = DOCUMENT_FORMAT_TEST.read_text(
        encoding="utf-8")
    for marker in (
        "extensionAndMimeArraysAreIndependentSnapshots",
        "emptyFormatHasNoPrimaryExtensionOrMimeType",
        "instanceMetadataFieldsArePrivateFinal",
        'extensions[0] = ".changed"',
        'mimeFormats[0] = "application/changed"',
    ):
        if marker not in document_format_test_text:
            violations.append(
                f"{relative(DOCUMENT_FORMAT_TEST)} omits format metadata "
                f"regression: {marker}")

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
        "final BlockingResult<T> result = new BlockingResult<>()",
        "result.complete(null)",
        "result.await()",
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
        "ReaderView.Sync",
        "new Sync<",
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

    delayed_executor_text = DELAYED_EXECUTOR.read_text(encoding="utf-8")
    for marker in (
        "public final class DelayedExecutor",
        "private final ReplaceableTaskSlot tasks",
        "public synchronized void postDelayed(",
        "public synchronized void cancel()",
        "tasks.replace(loggedTask)",
        "target.removeCallbacks(replacement.previous())",
        "if (!accepted)",
        "tasks.cancel()",
    ):
        if marker not in delayed_executor_text:
            violations.append(
                f"{relative(DELAYED_EXECUTOR)} omits replaceable callback "
                f"marker: {marker}")
    for legacy in (
        "private Runnable currentTask",
        "if (currentTask != null)",
    ):
        if legacy in delayed_executor_text:
            violations.append(
                f"{relative(DELAYED_EXECUTOR)} retains stale callback "
                f"check: {legacy}")

    replaceable_slot_text = REPLACEABLE_TASK_SLOT.read_text(
        encoding="utf-8")
    for marker in (
        "final class ReplaceableTaskSlot",
        "synchronized Replacement replace(Runnable task)",
        "synchronized Runnable cancel()",
        "private synchronized boolean claim(GuardedTask task)",
        "if (current != task)",
        "current = null",
        "if (claim(this))",
    ):
        if marker not in replaceable_slot_text:
            violations.append(
                f"{relative(REPLACEABLE_TASK_SLOT)} omits one-shot slot "
                f"marker: {marker}")

    replaceable_slot_test_text = REPLACEABLE_TASK_SLOT_TEST.read_text(
        encoding="utf-8")
    for marker in (
        "replacementInvalidatesOldWrapper",
        "claimedWrapperRunsOnlyOnceAndClearsSlot",
        "cancelInvalidatesPendingWrapperIdempotently",
        "runningDelegateCanScheduleNextGeneration",
        "first.current().run()",
        "wrapper.run();",
    ):
        if marker not in replaceable_slot_test_text:
            violations.append(
                f"{relative(REPLACEABLE_TASK_SLOT_TEST)} omits replaceable "
                f"task regression: {marker}")

    closeable_gate_text = CLOSEABLE_TASK_GATE.read_text(
        encoding="utf-8")
    for marker in (
        "final class CloseableTaskGate",
        "synchronized Token replace()",
        "synchronized void cancel()",
        "synchronized boolean complete(Token token)",
        "synchronized boolean close()",
        "synchronized boolean isActive(Token token)",
        "return !closed && token != null && current == token",
        "synchronized boolean isClosed()",
    ):
        if marker not in closeable_gate_text:
            violations.append(
                f"{relative(CLOSEABLE_TASK_GATE)} omits closeable task "
                f"marker: {marker}")

    closeable_gate_test_text = CLOSEABLE_TASK_GATE_TEST.read_text(
        encoding="utf-8")
    for marker in (
        "replacementInvalidatesOnlyThePreviousGeneration",
        "cancelIsIdempotentAndAllowsAnotherGeneration",
        "completionClearsOnlyItsExactGeneration",
        "nullIsNeverAnActiveGeneration",
        "closePermanentlyRejectsWork",
        "assertNull(gate.replace())",
    ):
        if marker not in closeable_gate_test_text:
            violations.append(
                f"{relative(CLOSEABLE_TASK_GATE_TEST)} omits closeable "
                f"task regression: {marker}")

    tts_toolbar_text = TTS_TOOLBAR.read_text(encoding="utf-8")
    for marker in (
        "private final CloseableTaskGate workLifecycle",
        "private final Handler audioBookPosHandler",
        "if (!workLifecycle.close())",
        "private void stopAudiobookWork()",
        "audioBookPosHandler.removeCallbacksAndMessages(null)",
        "wordTimingCalcHandlerThread.quit()",
        "CloseableTaskGate.Token token = workLifecycle.replace()",
        "finishAudiobookInitialization(",
        "scheduleAudiobookPositionPoll(token)",
        "workLifecycle.isActive(token)",
    ):
        if marker not in tts_toolbar_text:
            violations.append(
                f"{relative(TTS_TOOLBAR)} omits owned TTS work "
                f"marker: {marker}")
    for legacy in (
        "audioBookPosRunnable",
        "postDelayed(this, 500)",
        "private boolean mClosed",
    ):
        if legacy in tts_toolbar_text:
            violations.append(
                f"{relative(TTS_TOOLBAR)} retains unowned TTS lifecycle "
                f"marker: {legacy}")
    for marker in (
        "private MotionWatchdogHandler mMotionWatchdog",
        "private synchronized void startMotionWatchdog()",
        "private synchronized void stopMotionWatchdog()",
        "stopMotionWatchdog();",
        "mMotionWatchdog = new MotionWatchdogHandler(",
        "watchdog.close()",
    ):
        if marker not in tts_toolbar_text:
            violations.append(
                f"{relative(TTS_TOOLBAR)} omits owned motion watchdog "
                f"marker: {marker}")
    for legacy in (
        "private HandlerThread mMotionWatchdog",
        "mMotionWatchdog.interrupt()",
        "new MotionWatchdogHandler(this, mCoolReader",
    ):
        if legacy in tts_toolbar_text:
            violations.append(
                f"{relative(TTS_TOOLBAR)} retains detached motion "
                f"watchdog marker: {legacy}")

    motion_watchdog_text = MOTION_WATCHDOG.read_text(
        encoding="utf-8")
    for marker in (
        "public final class MotionWatchdogHandler",
        "super(handlerThread.getLooper())",
        "private final HandlerThread mHandlerThread",
        "private final AtomicBoolean mClosing",
        "public void close()",
        "mClosing.compareAndSet(false, true)",
        "mSensorManager.unregisterListener(this)",
        "removeCallbacksAndMessages(null)",
        "postAtFrontOfQueue(this::finishClose)",
        "mHandlerThread.quitSafely()",
        "BackgroundThread.instance().postGUI(",
        "mTTSToolbarDlg::stopAndClose",
    ):
        if marker not in motion_watchdog_text:
            violations.append(
                f"{relative(MOTION_WATCHDOG)} omits owned background "
                f"watchdog marker: {marker}")
    for legacy in (
        "Thread.sleep(",
        "mHandlerThread.interrupt()",
        "mHandlerThread.isInterrupted()",
        "private HandlerThread mHandlerThread",
    ):
        if legacy in motion_watchdog_text:
            violations.append(
                f"{relative(MOTION_WATCHDOG)} retains UI/interrupt-driven "
                f"watchdog marker: {legacy}")

    motion_fade_text = MOTION_WATCHDOG_FADE_STATE.read_text(
        encoding="utf-8")
    for marker in (
        "final class MotionWatchdogFadeState",
        "private final int originalVolume",
        "private int currentVolume",
        "originalVolume = Math.max(0, observedVolume)",
        "boolean isSilent()",
        "if (currentVolume > 0)",
        "currentVolume--",
    ):
        if marker not in motion_fade_text:
            violations.append(
                f"{relative(MOTION_WATCHDOG_FADE_STATE)} omits bounded "
                f"volume fade marker: {marker}")

    motion_fade_test_text = (
        MOTION_WATCHDOG_FADE_STATE_TEST.read_text(
            encoding="utf-8")
    )
    for marker in (
        "fadeReachesZeroWithoutUnderflow",
        "zeroVolumeIsAlreadySilent",
        "malformedNegativeVolumeIsClamped",
        "Integer.MIN_VALUE",
        "assertEquals(0, state.step())",
    ):
        if marker not in motion_fade_test_text:
            violations.append(
                f"{relative(MOTION_WATCHDOG_FADE_STATE_TEST)} omits "
                f"volume fade regression: {marker}")

    repeat_touch_text = REPEAT_ON_TOUCH_LISTENER.read_text(
        encoding="utf-8")
    for marker in (
        "implements OnTouchListener, View.OnAttachStateChangeListener",
        "private final Handler handler",
        "new Handler(Looper.getMainLooper())",
        "private final ReplaceableTaskSlot repeatTasks",
        "motionEvent.getActionMasked()",
        "private void scheduleRepeat(View view, int delayMillis)",
        "repeatTasks.replace(() -> repeat(view))",
        "handler.removeCallbacks(replacement.previous())",
        "private void stopRepeating()",
        "Runnable pending = repeatTasks.cancel()",
        "view.removeOnAttachStateChangeListener(this)",
        "public void onViewDetachedFromWindow(View view)",
    ):
        if marker not in repeat_touch_text:
            violations.append(
                f"{relative(REPEAT_ON_TOUCH_LISTENER)} omits owned repeat "
                f"touch marker: {marker}")
    for legacy in (
        "private View touchedView",
        "handlerRunnable",
        "new Handler()",
        "motionEvent.getAction()",
    ):
        if legacy in repeat_touch_text:
            violations.append(
                f"{relative(REPEAT_ON_TOUCH_LISTENER)} retains stale "
                f"repeat touch marker: {legacy}")

    blocking_result_text = BLOCKING_RESULT.read_text(encoding="utf-8")
    for marker in (
        "final class BlockingResult<T>",
        "synchronized void complete(T value)",
        "synchronized T await()",
        "while (!completed)",
        "notifyAll()",
        "Thread.currentThread().interrupt()",
    ):
        if marker not in blocking_result_text:
            violations.append(
                f"{relative(BLOCKING_RESULT)} omits blocking handoff "
                f"marker: {marker}")

    blocking_result_test_text = BLOCKING_RESULT_TEST.read_text(
        encoding="utf-8")
    for marker in (
        "awaitBlocksUntilValueIsPublished",
        "completionReleasesEveryWaiter",
        "interruptedWaitStillReceivesResultAndRestoresFlag",
        "resultCanOnlyCompleteOnce",
    ):
        if marker not in blocking_result_test_text:
            violations.append(
                f"{relative(BLOCKING_RESULT_TEST)} omits blocking handoff "
                f"regression: {marker}")

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
    for marker in (
        "BacklightOptions.nearestIndex(",
        "BacklightOptions.size()",
        "BacklightOptions.valueAt(",
        "BacklightOptions.titleAt(",
    ):
        if marker not in reader_view_text:
            violations.append(
                f"{relative(READER_VIEW)} omits backlight owner marker: "
                f"{marker}")
    if "OptionsDialog.mBacklightLevels" in reader_view_text:
        violations.append(
            f"{relative(READER_VIEW)} mutates shared dialog backlight state")
    if "PageFlipGeometry.tableIndex(" not in reader_view_text:
        violations.append(
            f"{relative(READER_VIEW)} does not use bounded page-flip "
            "table indexing")
    for marker in (
        "TapZoneGeometry.zoneAt(",
        "TapZoneGeometry.boundsAt(",
    ):
        if marker not in reader_view_text:
            violations.append(
                f"{relative(READER_VIEW)} omits shared tap-zone geometry: "
                f"{marker}")
    for legacy in (
        "int x1 = dx / 3",
        "int x2 = dx * 2 / 3",
        "(maxX + 2) / 3",
        "(maxY + 2) / 3",
    ):
        if legacy in reader_view_text:
            violations.append(
                f"{relative(READER_VIEW)} retains divergent tap-zone "
                f"arithmetic: {legacy}")
    for marker in (
        "private final EinkRefreshLeaseTracker einkRefreshLeases",
        "einkRefreshLeases.acquire(",
        "einkRefreshLeases.release(",
        "einkRefreshLeases.isActive()",
    ):
        if marker not in reader_view_text:
            violations.append(
                f"{relative(READER_VIEW)} omits E-Ink refresh lease owner: "
                f"{marker}")
    for legacy in (
        "savedEinkUpdateInterval",
        "einkModeClients",
    ):
        if legacy in reader_view_text:
            violations.append(
                f"{relative(READER_VIEW)} retains inline E-Ink refresh "
                f"state: {legacy}")
    for marker in (
        "private volatile BatteryStatus batteryStatus",
        "public void setBatteryStatus(BatteryStatus status)",
        "private void applyBatteryStatusToDocument()",
    ):
        if marker not in reader_view_text:
            violations.append(
                f"{relative(READER_VIEW)} omits atomic battery snapshot: "
                f"{marker}")
    for legacy in (
        "mBatteryState",
        "mBatteryChargingConn",
        "mBatteryChargeLevel",
    ):
        if legacy in reader_view_text:
            violations.append(
                f"{relative(READER_VIEW)} retains parallel battery field: "
                f"{legacy}")
    for marker in (
        "private final ReaderProgressState progressState",
        "ReaderProgressState.Snapshot progress =",
        "progressState.show(",
        "progressState.hide()",
    ):
        if marker not in reader_view_text:
            violations.append(
                f"{relative(READER_VIEW)} omits atomic progress owner: "
                f"{marker}")
    for legacy in (
        "currentProgressPosition",
        "currentProgressTitleId",
        "currentProgressTitle",
    ):
        if legacy in reader_view_text:
            violations.append(
                f"{relative(READER_VIEW)} retains parallel progress field: "
                f"{legacy}")
    for marker in (
        "private volatile ViewAnimationControl currentAnimation",
        "private final Object animationUpdateLock",
        "private final DelayedExecutor animationScheduler",
        "private final AutoScrollSessionState<AutoScrollAnimation>",
        "private final DelayedExecutor autoScrollScheduler",
        "private final CloseableTaskGate swapTaskLifecycle",
        "private final DelayedExecutor swapTaskScheduler",
        "private final TapHighlightState tapHighlightState",
        "private final DelayedExecutor tapHighlightScheduler",
        "private final ViewportResizeState viewportResizeState",
        "private final DelayedExecutor resizeScheduler",
        "private final CloseableTaskGate positionSaveLifecycle",
        "private final DelayedExecutor positionSaveScheduler",
        "private final CloseableTaskGate selectionUpdateLifecycle",
        "private volatile int autoScrollSpeed",
        "private final DelayedExecutor gcTask",
        "private void cancelDelayedReaderWork()",
        "animationScheduler.cancel()",
        "autoScrollScheduler.cancel()",
        "autoScrollSessions.close()",
        "private void closeSwapTasks()",
        "swapTaskLifecycle.close()",
        "swapTaskScheduler.cancel()",
        "private void closeTapHighlight()",
        "tapHighlightState.close()",
        "tapHighlightScheduler.cancel()",
        "viewportResizeState.close()",
        "resizeScheduler.cancel()",
        "private void closePositionSave()",
        "positionSaveLifecycle.close()",
        "positionSaveScheduler.cancel()",
        "private void closeSelectionUpdates()",
        "selectionUpdateLifecycle.close()",
        "gcTask.cancel()",
        "synchronized (animationUpdateLock)",
        "autoScrollSessions.beginInitialization(this)",
        "autoScrollSessions.markReady(this)",
        "autoScrollSessions.readySession()",
        "autoScrollScheduler.postDelayed(",
        "new AutoscrollTimerTask(interval).schedule()",
        "swapTaskLifecycle.replace()",
        "swapTaskLifecycle.isActive(owner)",
        "swapTaskScheduler.postDelayed(",
        "swapTaskLifecycle.complete(owner)",
        "tapHighlightState.requestShow(",
        "tapHighlightState.requestOwnedHide(owner)",
        "tapHighlightState.applyShow(show)",
        "tapHighlightState.applyHide(hide)",
        "tapHighlightScheduler.postDelayed(",
        "drawTapHighlightTransition(transition)",
        "viewportResizeState.request(width, height)",
        "viewportResizeState.requestCurrent()",
        "viewportResizeState.isCurrent(request)",
        "viewportResizeState.complete(request)",
        "resizeScheduler.postDelayed(task, delay)",
        "positionSaveLifecycle.replace()",
        "positionSaveLifecycle.complete(owner)",
        "positionSaveScheduler.postDelayed(",
        "private void applyPositionSave(",
        "mBookInfo != bookInfo",
        "savePositionBookmark(bookInfo, bookmark)",
        "selectionUpdateLifecycle.replace()",
        "selectionUpdateLifecycle.isActive(owner)",
        "selectionUpdateLifecycle.complete(owner)",
    ):
        if marker not in reader_view_text:
            violations.append(
                f"{relative(READER_VIEW)} omits reader-owned delayed work "
                f"marker: {marker}")
    if "synchronized (AnimationUpdate.class)" in reader_view_text:
        violations.append(
            f"{relative(READER_VIEW)} coordinates reader generations on "
            "the process-wide AnimationUpdate class monitor")
    for legacy in (
        "currentAutoScrollAnimation",
        "currentSwapTask",
        "nextHiliteId",
        "hiliteRect",
        "lastResizeTaskId",
        "requestedWidth",
        "requestedHeight",
        "lastSavePositionTaskId",
        "nextUpdateId",
        "BackgroundThread.instance().postGUI("
        "AutoscrollTimerTask.this",
    ):
        if legacy in reader_view_text:
            violations.append(
                f"{relative(READER_VIEW)} retains unowned autoscroll "
                f"lifecycle marker: {legacy}")

    auto_scroll_state_text = AUTO_SCROLL_SESSION_STATE.read_text(
        encoding="utf-8")
    for marker in (
        "final class AutoScrollSessionState<T>",
        "private T current",
        "private boolean ready",
        "private boolean initialized",
        "private boolean closed",
        "synchronized boolean requestStart(T session)",
        "synchronized boolean beginInitialization(T session)",
        "synchronized boolean markReady(T session)",
        "synchronized boolean isCurrent(T session)",
        "synchronized boolean isReady(T session)",
        "synchronized boolean isInitialized(T session)",
        "synchronized T readySession()",
        "synchronized boolean stop(T session)",
        "synchronized T stopCurrent()",
        "synchronized T close()",
    ):
        if marker not in auto_scroll_state_text:
            violations.append(
                f"{relative(AUTO_SCROLL_SESSION_STATE)} omits exact "
                f"autoscroll ownership marker: {marker}")

    auto_scroll_state_test_text = (
        AUTO_SCROLL_SESSION_STATE_TEST.read_text(encoding="utf-8")
    )
    for marker in (
        "sessionIsNotRenderableUntilInitializationCompletes",
        "stoppedInitializationCannotResurrectItsSession",
        "staleOwnerCannotStopReplacementSession",
        "initializationTemporarilySuppressesRendering",
        "stopCurrentReturnsItsExactOwnerOnce",
        "nullIsNeverTreatedAsAnOwner",
        "closePermanentlyRejectsStaleAndNewSessions",
    ):
        if marker not in auto_scroll_state_test_text:
            violations.append(
                f"{relative(AUTO_SCROLL_SESSION_STATE_TEST)} omits "
                f"autoscroll lifecycle regression: {marker}")

    tap_highlight_state_text = TAP_HIGHLIGHT_STATE.read_text(
        encoding="utf-8")
    for marker in (
        "final class TapHighlightState",
        "private Object current",
        "private Show visible",
        "private boolean closed",
        "synchronized Show requestShow(",
        "synchronized Hide requestHideAll()",
        "synchronized Hide requestOwnedHide(Show owner)",
        "synchronized Transition applyShow(Show show)",
        "synchronized Transition applyHide(Hide hide)",
        "synchronized boolean isVisible(Show show)",
        "synchronized void invalidate()",
        "synchronized boolean close()",
        "static final class Show",
        "static final class Hide",
        "static final class Transition",
        "private final Show previous",
        "private final Show current",
    ):
        if marker not in tap_highlight_state_text:
            violations.append(
                f"{relative(TAP_HIGHLIGHT_STATE)} omits atomic tap "
                f"highlight marker: {marker}")

    tap_highlight_state_test_text = (
        TAP_HIGHLIGHT_STATE_TEST.read_text(encoding="utf-8")
    )
    for marker in (
        "latestShowWinsAndPublishesOnlyWhenApplied",
        "replacementTransitionCarriesBothDirtyBounds",
        "ownedHideClearsPriorVisibleAndPendingOwner",
        "staleTimerCannotHideReplacement",
        "globalHideIsOneShotAndCanCancelPendingShow",
        "invalidationRejectsQueuedShowAndClearsVisible",
        "invalidBoundsAreNotScheduled",
        "nullBoundsAreRejected",
        "closePermanentlyRejectsQueuedAndNewWork",
    ):
        if marker not in tap_highlight_state_test_text:
            violations.append(
                f"{relative(TAP_HIGHLIGHT_STATE_TEST)} omits tap "
                f"highlight lifecycle regression: {marker}")

    viewport_resize_state_text = VIEWPORT_RESIZE_STATE.read_text(
        encoding="utf-8")
    for marker in (
        "final class ViewportResizeState",
        "private volatile Size size",
        "private Request current",
        "private boolean closed",
        "synchronized Request request(int width, int height)",
        "synchronized Request requestCurrent()",
        "synchronized boolean isCurrent(Request request)",
        "synchronized boolean complete(Request request)",
        "synchronized boolean close()",
        "private static Size normalizedSize(",
        "static final class Request",
        "private final Size size",
        "static final class Size",
        "private final int width",
        "private final int height",
    ):
        if marker not in viewport_resize_state_text:
            violations.append(
                f"{relative(VIEWPORT_RESIZE_STATE)} omits immutable "
                f"viewport resize marker: {marker}")

    viewport_resize_state_test_text = (
        VIEWPORT_RESIZE_STATE_TEST.read_text(encoding="utf-8")
    )
    for marker in (
        "latestRequestWinsByIdentityWithItsOwnSize",
        "currentSizeCanBeRescheduledWithoutParallelFields",
        "invalidDimensionsUseStablePositiveFallbacks",
        "completionClearsOnlyItsExactRequest",
        "closePermanentlyRejectsQueuedAndNewRequests",
        "Integer.MIN_VALUE",
        "Integer.MAX_VALUE",
    ):
        if marker not in viewport_resize_state_test_text:
            violations.append(
                f"{relative(VIEWPORT_RESIZE_STATE_TEST)} omits viewport "
                f"resize regression: {marker}")
    for marker in (
        "FontFaceSwitcher.select(",
        "if (selected == null)",
        "saveSetting(PROP_FONT_FACE, selected)",
    ):
        if marker not in reader_view_text:
            violations.append(
                f"{relative(READER_VIEW)} omits bounded font switcher "
                f"marker: {marker}")
    if "mFontFaces[index]" in reader_view_text:
        violations.append(
            f"{relative(READER_VIEW)} indexes the native font catalog "
            "without an empty-list boundary")
    if "props.getPercent() / 100" not in reader_view_text:
        violations.append(
            f"{relative(READER_VIEW)} does not reuse bounded document "
            "percentage for go-to input")
    if "props.y * 100 / props.fullHeight" in reader_view_text:
        violations.append(
            f"{relative(READER_VIEW)} retains divide-by-zero-prone "
            "go-to percentage arithmetic")
    for marker in (
        "DocumentPositionPolicy.formatPercent(",
        "DocumentPositionPolicy.displayPageNumber(",
        "DocumentPositionPolicy.pageIndexForPercent(",
    ):
        if marker not in reader_view_text:
            violations.append(
                f"{relative(READER_VIEW)} omits document position policy "
                f"marker: {marker}")
    for legacy in (
        "10000 * (long) props.y / props.fullHeight",
        "10000 * (long) prop.y / prop.fullHeight",
        "pos.pageCount * percent / 100",
        "prop.pageNumber + 1",
    ):
        if legacy in reader_view_text:
            violations.append(
                f"{relative(READER_VIEW)} retains inline document position "
                f"arithmetic: {legacy}")
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
    for marker in (
        "private final VMRuntimeHack runtime = new VMRuntimeHack()",
        "private final BitmapFactory factory = new BitmapFactory(runtime)",
        "private long hackMemorySize",
        "bmp.getRowBytes(), bmp.getHeight()",
        "BitmapMemoryAccounting.surfaceBytes(width, height)",
    ):
        if marker not in reader_view_text:
            violations.append(
                f"{relative(READER_VIEW)} omits reader-owned bitmap marker: "
                f"{marker}")
    if re.search(
            r"\bstatic\s+(?:final\s+)?VMRuntimeHack\s+\w+",
            reader_view_text):
        violations.append(
            f"{relative(READER_VIEW)} shares VMRuntime accounting between "
            "reader generations")
    for marker in (
        "private final GestureAcceleration gestureAcceleration",
        "gestureAcceleration.apply(start, end, value)",
    ):
        if marker not in reader_view_text:
            violations.append(
                f"{relative(READER_VIEW)} omits gesture acceleration "
                f"owner marker: {marker}")
    if "accelerationShape" in reader_view_text:
        violations.append(
            f"{relative(READER_VIEW)} retains a mutable acceleration array")

    gesture_acceleration_text = GESTURE_ACCELERATION.read_text(
        encoding="utf-8")
    for marker in (
        "final class GestureAcceleration",
        "private final int[] shape",
        "this.shape = shape.clone()",
        "if (end <= start)",
        "long range = (long) end - start",
        "long position = INTERPOLATION_STEPS",
        "long result = start",
    ):
        if marker not in gesture_acceleration_text:
            violations.append(
                f"{relative(GESTURE_ACCELERATION)} omits immutable widened "
                f"acceleration marker: {marker}")

    gesture_acceleration_test_text = GESTURE_ACCELERATION_TEST.read_text(
        encoding="utf-8")
    for marker in (
        "legacyCurveMatchesKnownSamples",
        "inputIsClampedAndDegenerateRangeIsStable",
        "fullIntegerRangeUsesWidenedArithmetic",
        "constructorCopiesAndValidatesShape",
        "Integer.MIN_VALUE",
    ):
        if marker not in gesture_acceleration_test_text:
            violations.append(
                f"{relative(GESTURE_ACCELERATION_TEST)} omits gesture "
                f"regression: {marker}")

    for marker in (
        "private final AnimationTiming animationTiming",
        "animationTiming.hasSamples()",
        "animationTiming.resetSamples(",
        "animationTiming.averageDrawDuration()",
        "animationTiming.recordDrawDuration(duration)",
        "AnimationTiming.scrollStep(",
        "AnimationTiming.autoscrollProgress(",
    ):
        if marker not in reader_view_text:
            violations.append(
                f"{relative(READER_VIEW)} omits animation timing marker: "
                f"{marker}")
    for legacy in (
        "class RingBuffer",
        "mAvgDrawAnimationStats",
        "this.progress = i/steps",
        "60000 * charCount",
    ):
        if legacy in reader_view_text:
            violations.append(
                f"{relative(READER_VIEW)} retains inline animation timing: "
                f"{legacy}")

    animation_timing_text = ANIMATION_TIMING.read_text(encoding="utf-8")
    for marker in (
        "final class AnimationTiming",
        "private final long[] samples",
        "private final long initialAverage",
        "duration * samples.length",
        "double progress = (double) clampedStep / steps",
        "MILLIS_PER_MINUTE",
        "* characterCount",
        "elapsedMillis >= estimatedDuration",
    ):
        if marker not in animation_timing_text:
            violations.append(
                f"{relative(ANIMATION_TIMING)} omits scoped/widened timing "
                f"marker: {marker}")

    animation_timing_test_text = ANIMATION_TIMING_TEST.read_text(
        encoding="utf-8")
    for marker in (
        "rollingAveragePreservesLegacySamplingRules",
        "persistedAverageResetsTheWholeWindowSafely",
        "linearScrollStepsUseFractionalProgress",
        "acceleratedScrollStepsAreBoundedAndMonotonic",
        "autoscrollProgressMatchesNormalTimingAndClamps",
        "autoscrollWidensCharacterDurationMultiplication",
        "15_000_000L",
        "100_000",
    ):
        if marker not in animation_timing_test_text:
            violations.append(
                f"{relative(ANIMATION_TIMING_TEST)} omits animation "
                f"regression: {marker}")

    for marker in (
        "private final ReadingTimeTracker readingTimeTracker",
        "readingTimeTracker.start(",
        "readingTimeTracker.stop(",
        "readingTimeTracker.elapsed(",
        "readingTimeTracker.setElapsed(timeElapsed)",
    ):
        if marker not in reader_view_text:
            violations.append(
                f"{relative(READER_VIEW)} omits reading-time owner marker: "
                f"{marker}")
    for legacy in (
        "statStartTime",
        "statTimeElapsed",
    ):
        if legacy in reader_view_text:
            violations.append(
                f"{relative(READER_VIEW)} retains inline reading-time state: "
                f"{legacy}")

    reading_time_text = READING_TIME_TRACKER.read_text(encoding="utf-8")
    for marker in (
        "final class ReadingTimeTracker",
        "private static final long STOPPED = -1L",
        "private long startedAt = STOPPED",
        "private long accumulated",
        "synchronized boolean start(long timestamp)",
        "synchronized boolean stop(long timestamp)",
        "synchronized long elapsed(long timestamp)",
        "synchronized void setElapsed(long elapsed)",
        "elapsedSinceStart(timestamp)",
        "Math.max(0L, elapsed)",
        "Long.MAX_VALUE - first < second",
    ):
        if marker not in reading_time_text:
            violations.append(
                f"{relative(READING_TIME_TRACKER)} omits scoped/safe time "
                f"marker: {marker}")

    reading_time_test_text = READING_TIME_TRACKER_TEST.read_text(
        encoding="utf-8")
    for marker in (
        "pausedReadsAreIdempotent",
        "repeatedLifecycleSignalsDoNotDoubleCount",
        "persistedBaselineCanBeReplacedDuringActiveSession",
        "clockRegressionDoesNotSubtractReadingTime",
        "elapsedTimeSaturatesInsteadOfOverflowing",
        "zeroTimestampIsValidAndNegativeStartIsRejected",
        "Long.MAX_VALUE",
    ):
        if marker not in reading_time_test_text:
            violations.append(
                f"{relative(READING_TIME_TRACKER_TEST)} omits reading-time "
                f"regression: {marker}")

    vm_runtime_text = VM_RUNTIME_HACK.read_text(encoding="utf-8")
    for marker in (
        "public final class VMRuntimeHack",
        "private final Object runtime",
        "private final Method trackAllocation",
        "private final Method trackFree",
        "private long totalSize",
        "public synchronized boolean trackAlloc(long size)",
        "public synchronized boolean trackFree(long size)",
        "if (runtime == null || size < 0)",
        "if (!invoke(trackAllocation, size))",
        "if (!invoke(trackFree, size))",
    ):
        if marker not in vm_runtime_text:
            violations.append(
                f"{relative(VM_RUNTIME_HACK)} omits scoped accounting "
                f"marker: {marker}")
    if re.search(
            r"\bstatic\s+(?:final\s+)?(?:Object|Method|long|int)\s+\w+",
            vm_runtime_text):
        violations.append(
            f"{relative(VM_RUNTIME_HACK)} retains process-wide mutable state")

    bitmap_accounting_text = BITMAP_MEMORY_ACCOUNTING.read_text(
        encoding="utf-8")
    for marker in (
        "final class BitmapMemoryAccounting",
        "return (long) first * second",
        "dimensions must be non-negative",
    ):
        if marker not in bitmap_accounting_text:
            violations.append(
                f"{relative(BITMAP_MEMORY_ACCOUNTING)} omits widened bitmap "
                f"marker: {marker}")

    vm_runtime_test_text = VM_RUNTIME_HACK_TEST.read_text(encoding="utf-8")
    for marker in (
        "unavailableTrackerIsANoop",
        "successfulCallsUseLongAccountingAndStayInstanceOwned",
        "rejectedOrThrowingCallsDoNotChangeAccounting",
        "concurrentCallsCannotLoseAccountingUpdates",
    ):
        if marker not in vm_runtime_test_text:
            violations.append(
                f"{relative(VM_RUNTIME_HACK_TEST)} omits VMRuntime "
                f"regression: {marker}")

    bitmap_accounting_test_text = BITMAP_MEMORY_ACCOUNTING_TEST.read_text(
        encoding="utf-8")
    for marker in (
        "bitmapAccountingUsesActualRowStride",
        "surfaceAccountingWidensBeforeMultiplication",
        "negativeDimensionsAreRejected",
        "Integer.MAX_VALUE",
    ):
        if marker not in bitmap_accounting_test_text:
            violations.append(
                f"{relative(BITMAP_MEMORY_ACCOUNTING_TEST)} omits bitmap "
                f"regression: {marker}")

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

    tap_zone_geometry_text = TAP_ZONE_GEOMETRY.read_text(encoding="utf-8")
    for marker in (
        "final class TapZoneGeometry",
        "static int zoneAt(int x, int y, int width, int height)",
        "static Bounds boundsAt(int x, int y, int width, int height)",
        "private static int segmentAt(int coordinate, int size)",
        "(long) size * segment / SEGMENT_COUNT",
        "private final int left",
        "private final int bottom",
    ):
        if marker not in tap_zone_geometry_text:
            violations.append(
                f"{relative(TAP_ZONE_GEOMETRY)} omits shared geometry "
                f"marker: {marker}")

    tap_zone_geometry_test_text = TAP_ZONE_GEOMETRY_TEST.read_text(
        encoding="utf-8")
    for marker in (
        "zonesFollowRowMajorThreeByThreeLayout",
        "highlightBoundsMatchEveryPixelForNonDivisibleSize",
        "coordinatesClampAndInvalidSurfacesUseSafeFallbacks",
        "boundaryArithmeticWidensBeforeMultiplication",
        "Integer.MAX_VALUE",
    ):
        if marker not in tap_zone_geometry_test_text:
            violations.append(
                f"{relative(TAP_ZONE_GEOMETRY_TEST)} omits tap-zone "
                f"regression: {marker}")

    position_properties_text = POSITION_PROPERTIES.read_text(
        encoding="utf-8")
    for marker in (
        "long scrollableHeight = (long) fullHeight - pageHeight",
        "long percent = (long) y * 10000 / scrollableHeight",
        "Math.max(0, Math.min(percent, 10000))",
        "(long) y < scrollableHeight",
        "(long) pageNumber < (long) pageCount - pageMode",
    ):
        if marker not in position_properties_text:
            violations.append(
                f"{relative(POSITION_PROPERTIES)} omits widened position "
                f"marker: {marker}")

    position_properties_test_text = POSITION_PROPERTIES_TEST.read_text(
        encoding="utf-8")
    for marker in (
        "percentUsesScrollableHeight",
        "emptyAndNegativeRangesStartAtZero",
        "percentClampsBeforeAndAfterDocument",
        "percentWidensMultiplicationAndRangeSubtraction",
        "scrollMovementUsesTheSameWidenedRange",
        "Integer.MIN_VALUE",
        "Integer.MAX_VALUE",
    ):
        if marker not in position_properties_test_text:
            violations.append(
                f"{relative(POSITION_PROPERTIES_TEST)} omits position "
                f"regression: {marker}")

    document_position_text = DOCUMENT_POSITION_POLICY.read_text(
        encoding="utf-8")
    for marker in (
        "final class DocumentPositionPolicy",
        "static int displayPageNumber(int pageIndex, int pageCount)",
        "long display = (long) pageIndex + 1",
        "static int pageIndexForPercent(int pageCount, int percent)",
        "if (boundedPercent == 100)",
        "(long) pageCount * boundedPercent / 100",
        "static String formatPercent(int percent)",
        "(bounded / 10 % 10)",
    ):
        if marker not in document_position_text:
            violations.append(
                f"{relative(DOCUMENT_POSITION_POLICY)} omits document "
                f"position policy marker: {marker}")

    document_position_test_text = DOCUMENT_POSITION_POLICY_TEST.read_text(
        encoding="utf-8")
    for marker in (
        "displayPageIsOneBasedAndClamped",
        "percentMapsToValidZeroBasedPage",
        "percentPageMappingWidensBeforeMultiplication",
        "percentFormattingUsesOneDecimalAndClamps",
        "Integer.MAX_VALUE",
        '"100.0%"',
    ):
        if marker not in document_position_test_text:
            violations.append(
                f"{relative(DOCUMENT_POSITION_POLICY_TEST)} omits document "
                f"position policy regression: {marker}")

    eink_lease_text = EINK_REFRESH_LEASE_TRACKER.read_text(
        encoding="utf-8")
    for marker in (
        "final class EinkRefreshLeaseTracker",
        "private final Set<Integer> clients",
        "private Integer savedInterval",
        "synchronized boolean acquire(",
        "synchronized Integer release(",
        "synchronized boolean isActive()",
        "if (!clients.remove(clientId) || !clients.isEmpty())",
    ):
        if marker not in eink_lease_text:
            violations.append(
                f"{relative(EINK_REFRESH_LEASE_TRACKER)} omits E-Ink "
                f"lease marker: {marker}")

    eink_lease_test_text = EINK_REFRESH_LEASE_TRACKER_TEST.read_text(
        encoding="utf-8")
    for marker in (
        "firstAcquireDisablesAndLastReleaseRestores",
        "overlappingClientsRestoreOnlyAfterLastRelease",
        "duplicateAndUnmatchedTransitionsAreNoOps",
        "negativeIntervalsAreValuesRatherThanSentinels",
        "tracker.acquire(1, -1)",
    ):
        if marker not in eink_lease_test_text:
            violations.append(
                f"{relative(EINK_REFRESH_LEASE_TRACKER_TEST)} omits E-Ink "
                f"lease regression: {marker}")

    battery_status_text = BATTERY_STATUS.read_text(encoding="utf-8")
    for marker in (
        "public final class BatteryStatus",
        "private final int state",
        "private final int chargingConnection",
        "private final int chargeLevel",
        "public static BatteryStatus unavailable()",
        "public static BatteryStatus fromRawLevel(",
        "(long) rawLevel * 100 / scale",
        "Math.min(percent, 100)",
    ):
        if marker not in battery_status_text:
            violations.append(
                f"{relative(BATTERY_STATUS)} omits immutable battery "
                f"snapshot marker: {marker}")

    battery_status_test_text = BATTERY_STATUS_TEST.read_text(
        encoding="utf-8")
    for marker in (
        "rawLevelIsNormalizedAgainstProviderScale",
        "invalidAndOutOfRangeLevelsAreSafelyClamped",
        "unavailableStatusMatchesNativeNoBatteryContract",
        "statusIsAnImmutableComparableSnapshot",
        "level(Integer.MAX_VALUE, 1)",
        "Modifier.isFinal(BatteryStatus.class.getModifiers())",
    ):
        if marker not in battery_status_test_text:
            violations.append(
                f"{relative(BATTERY_STATUS_TEST)} omits battery snapshot "
                f"regression: {marker}")

    cool_reader_battery_text = COOL_READER.read_text(encoding="utf-8")
    for marker in (
        "private BatteryStatus initialBatteryStatus",
        "BatteryManager.EXTRA_SCALE",
        "BatteryStatus.fromRawLevel(",
        "mReaderView.setBatteryStatus(",
    ):
        if marker not in cool_reader_battery_text:
            violations.append(
                f"{relative(COOL_READER)} omits battery boundary marker: "
                f"{marker}")
    for legacy in (
        "initialBatteryState",
        "initialBatteryChargeConn",
        "initialBatteryLevel",
    ):
        if legacy in cool_reader_battery_text:
            violations.append(
                f"{relative(COOL_READER)} retains parallel initial battery "
                f"field: {legacy}")

    reader_progress_text = READER_PROGRESS_STATE.read_text(
        encoding="utf-8")
    for marker in (
        "final class ReaderProgressState",
        "private volatile Snapshot snapshot = Snapshot.HIDDEN",
        "synchronized Change show(",
        "synchronized boolean hide()",
        "return previous.active ? Change.UPDATE : Change.FIRST",
        "private final boolean active",
        "private final int position",
        "private final String title",
    ):
        if marker not in reader_progress_text:
            violations.append(
                f"{relative(READER_PROGRESS_STATE)} omits atomic progress "
                f"marker: {marker}")

    reader_progress_test_text = READER_PROGRESS_STATE_TEST.read_text(
        encoding="utf-8")
    for marker in (
        "initialStateIsHiddenAndComplete",
        "firstShowIsActiveEvenAtZeroPosition",
        "duplicateShowIsNoOpAndUpdatesPublishNewSnapshot",
        "hideIsIdempotentAndNextShowIsFirstAgain",
        "nullTitleIsRejected",
        "state.show(0, 10, \"Loading\")",
    ):
        if marker not in reader_progress_test_text:
            violations.append(
                f"{relative(READER_PROGRESS_STATE_TEST)} omits progress "
                f"regression: {marker}")

    progress_dialog_text = PROGRESS_DIALOG.read_text(
        encoding="utf-8")
    for marker in (
        "private TextView mProgressNumber",
        "private TextView mProgressPercent",
        "private final Context mContext",
        "R.id.progress_number",
        "R.id.progress_percent",
        "ProgressDisplayState.format(",
        "mProgressNumber.setText(display.number())",
        "mProgressPercent.setText(display.percent())",
    ):
        if marker not in progress_dialog_text:
            violations.append(
                f"{relative(PROGRESS_DIALOG)} omits synchronous progress "
                f"display marker: {marker}")
    for legacy in (
        "mViewUpdateHandler",
        "new Handler()",
        "sendEmptyMessage(",
        "import android.os.Message",
    ):
        if legacy in progress_dialog_text:
            violations.append(
                f"{relative(PROGRESS_DIALOG)} retains empty async "
                f"progress marker: {legacy}")

    progress_display_text = PROGRESS_DISPLAY_STATE.read_text(
        encoding="utf-8")
    for marker in (
        "final class ProgressDisplayState",
        "static Snapshot format(",
        "int boundedMax = Math.max(0, max)",
        "Math.max(0, Math.min(progress, boundedMax))",
        "NumberFormat.getPercentInstance(locale)",
        "percentFormat.setMaximumFractionDigits(0)",
        "static final class Snapshot",
        "private final String number",
        "private final String percent",
    ):
        if marker not in progress_display_text:
            violations.append(
                f"{relative(PROGRESS_DISPLAY_STATE)} omits bounded "
                f"progress formatting marker: {marker}")

    progress_display_test_text = (
        PROGRESS_DISPLAY_STATE_TEST.read_text(encoding="utf-8")
    )
    for marker in (
        "normalProgressFormatsNumberAndPercent",
        "progressIsClampedToValidBounds",
        "invalidMaximumHasStableEmptyDisplay",
        "nullLocaleIsRejected",
        "Integer.MIN_VALUE",
        "Integer.MAX_VALUE",
        '"4325/10000"',
        '"43%"',
    ):
        if marker not in progress_display_test_text:
            violations.append(
                f"{relative(PROGRESS_DISPLAY_STATE_TEST)} omits "
                f"progress display regression: {marker}")

    progress_ui_text = PROGRESS_UI_STATE.read_text(
        encoding="utf-8")
    for marker in (
        "final class ProgressUiState",
        "synchronized Token requestShow()",
        "synchronized Token requestHideAll()",
        "synchronized OwnedHide requestOwnedHide(Token owner)",
        "current != owner",
        "synchronized boolean markVisible(Token request)",
        "synchronized void markShowFailed(Token request)",
        "synchronized boolean markDismissed(Token owner)",
        "synchronized boolean applyHideAll(Token request)",
        "synchronized boolean applyOwnedHide(OwnedHide hide)",
        "visible != hide.owner",
        "synchronized boolean close()",
        "static final class Token",
        "static final class OwnedHide",
    ):
        if marker not in progress_ui_text:
            violations.append(
                f"{relative(PROGRESS_UI_STATE)} omits identity-owned "
                f"progress marker: {marker}")

    progress_ui_test_text = PROGRESS_UI_STATE_TEST.read_text(
        encoding="utf-8")
    for marker in (
        "latestShowRequestWinsByIdentity",
        "globalHideInvalidatesPendingShowAndClearsVisible",
        "failedLatestShowClearsDismissedPreviousState",
        "ownerCanCancelItsPendingShowWithoutDismissingAnother",
        "ownerCannotHideAReplacementGeneration",
        "visibleOwnerCanHideExactlyItself",
        "dismissCallbackClearsOnlyItsVisibleOwner",
        "closePermanentlyRejectsNewUiWork",
    ):
        if marker not in progress_ui_test_text:
            violations.append(
                f"{relative(PROGRESS_UI_STATE_TEST)} omits progress "
                f"ownership regression: {marker}")

    for marker in (
        "private final ProgressUiState progressUiState",
        "private ProgressUiState.Token requestProgressShow(",
        "progressUiState.requestShow()",
        "applyProgressShow(",
        "progressUiState.markVisible(request)",
        "progressUiState.requestHideAll()",
        "progressUiState.requestOwnedHide(owner)",
        "private ProgressUiState.Token owner",
        "public synchronized void cancel()",
        "hideImmediately = cancelled",
        "progressUiState.close()",
        "this::dismissProgressDialog",
    ):
        if marker not in engine_text:
            violations.append(
                f"{relative(ENGINE)} omits identity-owned progress "
                f"integration marker: {marker}")
    for legacy in (
        "nextProgressId",
        "progressShown",
        "enable_progress",
        "mProgressMessage",
        "mProgressPos",
        "private volatile boolean cancelled",
        "private volatile boolean shown",
    ):
        if legacy in engine_text:
            violations.append(
                f"{relative(ENGINE)} retains numeric/parallel progress "
                f"state: {legacy}")

    font_switcher_text = FONT_FACE_SWITCHER.read_text(encoding="utf-8")
    for marker in (
        "final class FontFaceSwitcher",
        "if (available == null || available.length == 0)",
        "int step = Integer.compare(direction, 0)",
        "if (currentIndex < 0)",
        "Math.floorMod(currentIndex + step, available.length)",
    ):
        if marker not in font_switcher_text:
            violations.append(
                f"{relative(FONT_FACE_SWITCHER)} omits bounded font "
                f"selection marker: {marker}")

    font_switcher_test_text = FONT_FACE_SWITCHER_TEST.read_text(
        encoding="utf-8")
    for marker in (
        "emptyAndMissingCatalogsAreNoOps",
        "singletonAndZeroDirectionRemainStable",
        "knownFaceMovesAndWrapsInBothDirections",
        "unknownFaceStartsAtDirectionalEdge",
        "directionMagnitudeCannotOverflowSelection",
        "Integer.MIN_VALUE",
    ):
        if marker not in font_switcher_test_text:
            violations.append(
                f"{relative(FONT_FACE_SWITCHER_TEST)} omits font switch "
                f"regression: {marker}")

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
        "return ReadingTimeFormatter.format(timeElapsed)",
        "private static final AudioFileSelector AUDIO_FILE_SELECTOR",
        "private static final FileNameTranscriber FILE_NAME_TRANSCRIBER",
        "return AUDIO_FILE_SELECTOR.findAlternative(original)",
        "return FILE_NAME_TRANSCRIBER.transcribe(fileName)",
        "return FILE_NAME_TRANSCRIBER.transcribeWithLimit(str, maxLen)",
    ):
        if marker not in utils_text:
            violations.append(f"{relative(UTILS)} omits marker: {marker}")
    for legacy in (
        "AUDIO_FILE_EXTS",
        "substTables",
        "OPDSUtil.SubstTable",
    ):
        if legacy in utils_text:
            violations.append(
                f"{relative(UTILS)} retains mutable/coupled lookup storage: "
                f"{legacy}")
    if re.search(r"\(int\)\s*timeElapsed", utils_text):
        violations.append(
            f"{relative(UTILS)} narrows persisted reading time before "
            "formatting")

    audio_file_selector_text = AUDIO_FILE_SELECTOR.read_text(
        encoding="utf-8")
    for marker in (
        "final class AudioFileSelector",
        "private final List<String> extensions",
        "Collections.unmodifiableList(copy)",
        "copy.add(extension.toLowerCase(Locale.ROOT))",
        '"flac", "wav", "m4a", "ogg", "mp3"',
        "File findAlternative(File original)",
    ):
        if marker not in audio_file_selector_text:
            violations.append(
                f"{relative(AUDIO_FILE_SELECTOR)} omits immutable audio "
                f"lookup marker: {marker}")

    audio_file_selector_test_text = AUDIO_FILE_SELECTOR_TEST.read_text(
        encoding="utf-8")
    for marker in (
        "legacyPrioritySelectsPreferredExistingSibling",
        "existingOriginalAndSingleFallbackArePreserved",
        "extensionPriorityIsCopiedAndCannotBeMutated",
        "invalidExtensionPriorityIsRejected",
    ):
        if marker not in audio_file_selector_test_text:
            violations.append(
                f"{relative(AUDIO_FILE_SELECTOR_TEST)} omits audio lookup "
                f"regression: {marker}")

    file_name_transcriber_text = FILE_NAME_TRANSCRIBER.read_text(
        encoding="utf-8")
    for marker in (
        "final class FileNameTranscriber",
        "private final List<SubstitutionTable> tables",
        "Collections.unmodifiableList(",
        "private final String[] replacements",
        "this.replacements = replacements.clone()",
        "String transcribeWithLimit(String fileName, int maximumLength)",
    ):
        if marker not in file_name_transcriber_text:
            violations.append(
                f"{relative(FILE_NAME_TRANSCRIBER)} omits immutable "
                f"transliteration marker: {marker}")

    file_name_transcriber_test_text = FILE_NAME_TRANSCRIBER_TEST.read_text(
        encoding="utf-8")
    for marker in (
        "legacyCyrillicAndAsciiMappingIsPreserved",
        "limitIsAppliedAfterTransliterationExpansion",
        "storageIsPrivateFinalAndInstanceOwned",
        "invalidInputIsRejected",
        '"Privet_mir_txt"',
        '"Schuka"',
    ):
        if marker not in file_name_transcriber_test_text:
            violations.append(
                f"{relative(FILE_NAME_TRANSCRIBER_TEST)} omits filename "
                f"transliteration regression: {marker}")

    for path in (
            WORD_TIMING_AUDIOBOOK_MATCHER,
            TTS_CONTROL_SERVICE):
        text = path.read_text(encoding="utf-8")
        if "Utils.getAlternativeAudioFile(" not in text:
            violations.append(
                f"{relative(path)} bypasses the immutable audio lookup owner")
        if "Utils.AUDIO_FILE_EXTS" in text:
            violations.append(
                f"{relative(path)} still reads a mutable extension array")

    reading_time_formatter_text = READING_TIME_FORMATTER.read_text(
        encoding="utf-8")
    for marker in (
        "final class ReadingTimeFormatter",
        "private static final long MILLIS_PER_MINUTE = 60_000L",
        "private static final long MILLIS_PER_HOUR",
        "Math.max(0L, elapsedMillis)",
        "long hours = duration / MILLIS_PER_HOUR",
        "duration % MILLIS_PER_HOUR / MILLIS_PER_MINUTE",
        'String.format(locale, "%d:%02d", hours, minutes)',
    ):
        if marker not in reading_time_formatter_text:
            violations.append(
                f"{relative(READING_TIME_FORMATTER)} omits widened "
                f"formatting marker: {marker}")

    reading_time_formatter_test_text = READING_TIME_FORMATTER_TEST.read_text(
        encoding="utf-8")
    for marker in (
        "normalDurationsPreserveHoursAndPaddedMinutes",
        "durationsBeyondIntMillisecondsRemainAccurate",
        "longMaximumDoesNotNarrowOrOverflow",
        "negativePersistedDurationIsClampedToZero",
        "missingLocaleIsRejected",
        "formatterRetainsOnlyPrimitiveConstants",
        '"2562047788015:12"',
        "Long.MAX_VALUE",
    ):
        if marker not in reading_time_formatter_test_text:
            violations.append(
                f"{relative(READING_TIME_FORMATTER_TEST)} omits formatting "
                f"regression: {marker}")

    for path in (ABOUT_DIALOG, OPTIONS_DIALOG):
        text = path.read_text(encoding="utf-8")
        if re.search(r"\bServices\.", text):
            violations.append(
                f"{relative(path)} still uses the static service locator")
    options_text = OPTIONS_DIALOG.read_text(encoding="utf-8")
    for marker in (
        "private final Engine mEngine",
        "OptionsDialog(BaseActivity activity, Engine engine",
        "private final int[] mBacklightLevels",
        "private final String[] mBacklightLevelsTitles",
        "private final int[] mMotionTimeouts",
        "private final String[] mMotionTimeoutsTitles",
        "private final int[] mPagesPerFullSwipe",
        "private final String[] mPagesPerFullSwipeTitles",
        "private boolean isTextFormat",
        "private boolean isEpubFormat",
        "private boolean isFormatWithEmbeddedStyle",
        "PROP_APP_SETTINGS_SHOW_ICONS, true",
        "private static final StyleOptionCatalog STYLE_OPTION_CATALOG",
        "STYLE_OPTION_CATALOG.entries()",
        "createStyleEditor(style.code(), style.titleId())",
        "mActivity.getInterfaceThemes().themes()",
    ):
        if marker not in options_text:
            violations.append(
                f"{relative(OPTIONS_DIALOG)} omits marker: {marker}")
    for marker in (
        "private static boolean showIcons",
        "private static boolean isTextFormat",
        "private static boolean isEpubFormat",
        "private static boolean isFormatWithEmbeddedStyle",
        "private static boolean isHtmlFormat",
        "public static int[] mMotionTimeouts",
        "public static String[] mMotionTimeoutsTitles",
        "public static int[] mPagesPerFullSwipe",
        "public static String[] mPagesPerFullSwipeTitles",
        "public static final int[] mBacklightLevels",
        "public static final String[] mBacklightLevelsTitles",
        "styleCodes",
        "styleTitles",
        "InterfaceTheme.allThemes",
    ):
        if marker in options_text:
            violations.append(
                f"{relative(OPTIONS_DIALOG)} retains process UI state: "
                f"{marker}")

    interface_theme_text = INTERFACE_THEME.read_text(encoding="utf-8")
    for marker in (
        "public final class InterfaceTheme",
        "private final Visuals visuals",
        "static InterfaceTheme create(",
        "private static final class Visuals",
        "private final int toolbarButtonAlpha",
    ):
        if marker not in interface_theme_text:
            violations.append(
                f"{relative(INTERFACE_THEME)} omits immutable theme "
                f"marker: {marker}")
    for legacy in (
        "InterfaceTheme[] allThemes",
        "setRootDelimiter(",
        "setBackgrounds(",
        "setToolbarButtonAlpha(",
        "DeviceInfo.EINK_SCREEN",
    ):
        if legacy in interface_theme_text:
            violations.append(
                f"{relative(INTERFACE_THEME)} retains mutable/process theme "
                f"state: {legacy}")

    interface_theme_catalog_text = INTERFACE_THEME_CATALOG.read_text(
        encoding="utf-8")
    for marker in (
        "final class InterfaceThemeCatalog",
        "private final List<InterfaceTheme> themes",
        "Collections.unmodifiableList(",
        "new ArrayList<>(themes)",
        "static InterfaceThemeCatalog create(boolean einkScreen)",
        "List<InterfaceTheme> themes()",
        "InterfaceTheme findByCode(String code)",
        '"BLACK"',
        '"HICONTRAST2"',
    ):
        if marker not in interface_theme_catalog_text:
            violations.append(
                f"{relative(INTERFACE_THEME_CATALOG)} omits generation-owned "
                f"theme marker: {marker}")

    interface_theme_test_text = INTERFACE_THEME_TEST.read_text(
        encoding="utf-8")
    for marker in (
        "catalogPreservesLegacyOrderAndLookup",
        "catalogCannotBeMutated",
        "definitionsHaveOnlyFinalPrivateStorage",
        "visualValuesMatchLegacyDefinitions",
        "einkCatalogForcesOpaqueToolbarButtons",
        "assertEquals(8, catalog.themes().size())",
    ):
        if marker not in interface_theme_test_text:
            violations.append(
                f"{relative(INTERFACE_THEME_TEST)} omits theme regression: "
                f"{marker}")

    style_catalog_text = STYLE_OPTION_CATALOG.read_text(encoding="utf-8")
    for marker in (
        "final class StyleOptionCatalog",
        "private final List<Entry> entries",
        "Collections.unmodifiableList(",
        "new ArrayList<>(entries)",
        'new Entry("def", R.string.options_css_def)',
        '"annotation", R.string.options_css_annotation',
        "static final class Entry",
        "private final String code",
        "private final int titleId",
    ):
        if marker not in style_catalog_text:
            violations.append(
                f"{relative(STYLE_OPTION_CATALOG)} omits immutable typed "
                f"style marker: {marker}")

    style_catalog_test_text = STYLE_OPTION_CATALOG_TEST.read_text(
        encoding="utf-8")
    for marker in (
        "legacyCatalogPreservesAllTypedPairsInOrder",
        "entryListCannotBeMutated",
        "assertEquals(13, catalog.entries().size())",
        '"footnote-link"',
    ):
        if marker not in style_catalog_test_text:
            violations.append(
                f"{relative(STYLE_OPTION_CATALOG_TEST)} omits style "
                f"regression: {marker}")

    backlight_options_text = BACKLIGHT_OPTIONS.read_text(encoding="utf-8")
    for marker in (
        "final class BacklightOptions",
        "private static final int[] VALUES",
        "Math.abs((long) VALUES[i] - value)",
        "return VALUES.clone()",
        "static String[] titles(String defaultTitle)",
    ):
        if marker not in backlight_options_text:
            violations.append(
                f"{relative(BACKLIGHT_OPTIONS)} omits immutable backlight "
                f"marker: {marker}")

    backlight_options_test_text = BACKLIGHT_OPTIONS_TEST.read_text(
        encoding="utf-8")
    for marker in (
        "valuesAreIndependentSnapshots",
        "nearestIndexUsesStableEdgesAndFirstTie",
        "titlesAreLocalizedWithoutSharedMutation",
        "Integer.MIN_VALUE",
        "Integer.MAX_VALUE",
    ):
        if marker not in backlight_options_test_text:
            violations.append(
                f"{relative(BACKLIGHT_OPTIONS_TEST)} omits backlight "
                f"regression: {marker}")

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
        "FeedTimestampParser.parse(ts)",
    ):
        if marker not in opds_text:
            violations.append(f"{relative(OPDS_UTIL)} omits marker: {marker}")
    if re.search(
            r"\bstatic\s+(?:final\s+)?SimpleDateFormat\b",
            opds_text):
        violations.append(
            f"{relative(OPDS_UTIL)} retains a shared date formatter")
    if re.search(r"\bclass\s+SubstTable\b", opds_text):
        violations.append(
            f"{relative(OPDS_UTIL)} retains filename transliteration storage")

    feed_timestamp_parser_text = FEED_TIMESTAMP_PARSER.read_text(
        encoding="utf-8")
    for marker in (
        "final class FeedTimestampParser",
        "new SimpleDateFormat(",
        "Locale.US",
        "TimeZone.getTimeZone(\"UTC\")",
        "parser.setLenient(false)",
        "value.substring(23)",
        "position.getIndex() != value.length()",
    ):
        if marker not in feed_timestamp_parser_text:
            violations.append(
                f"{relative(FEED_TIMESTAMP_PARSER)} omits feed timestamp "
                f"marker: {marker}")

    feed_timestamp_parser_test_text = FEED_TIMESTAMP_PARSER_TEST.read_text(
        encoding="utf-8")
    for marker in (
        "utcTimestampIgnoresDefaultTimezone",
        "colonAndCompactOffsetsAreEquivalent",
        "malformedTimestampIsRejected",
        "concurrentParsingHasNoSharedFormatterState",
        "GMT+09:00",
    ):
        if marker not in feed_timestamp_parser_test_text:
            violations.append(
                f"{relative(FEED_TIMESTAMP_PARSER_TEST)} omits timestamp "
                f"regression: {marker}")

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
