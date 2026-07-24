#!/usr/bin/env bash
set -euo pipefail

repo_root="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
gradlew="${repo_root}/android/gradlew"
runner="org.coolreader.test/androidx.test.runner.AndroidJUnitRunner"
report_dir="${repo_root}/android/app/build/outputs/androidTest-results/manual"

run_instrumentation() {
    local report_name="$1"
    local test_name="$2"
    shift 2
    local output
    output="$(adb shell am instrument -w -r "$@" -e class "${test_name}" "${runner}")"
    printf '%s\n' "${output}" | tee "${report_dir}/${report_name}.txt"
    if ! grep -Eq 'OK \(1 test\)|OK \(1 test, 0 failures\)' <<<"${output}"; then
        echo "Instrumentation phase failed: ${test_name}" >&2
        return 1
    fi
}

install_test_apks() {
    "${gradlew}" -p "${repo_root}/android" --no-daemon \
        :app:installDebug :app:installDebugAndroidTest --stacktrace
}

mkdir -p "${report_dir}"
install_test_apks
run_instrumentation \
    'phase0-open-ordinary-file' \
    'org.coolreader.AndroidSmokeInstrumentedTest#ordinaryFileOpensFromGenericMimeIntent'
run_instrumentation \
    'phase0-library-root-management' \
    'org.coolreader.crengine.LibraryRootStoreInstrumentedTest#rootsCanBeReplacedAndRemovedWithoutFileOperations'
run_instrumentation \
    'phase1-persist-grant-and-position' \
    'org.coolreader.PersistedUriRestartInstrumentedTest#phase1_establishPersistedGrantAndPosition'

adb shell am force-stop org.coolreader

run_instrumentation \
    'phase2-restore-after-process-restart' \
    'org.coolreader.PersistedUriRestartInstrumentedTest#phase2_restoreAfterProcessRestartWithoutNewGrant'
run_instrumentation \
    'phase3-tts-notification-actions' \
    'org.coolreader.AndroidSmokeInstrumentedTest#ttsNotificationActionsReachPrivateServiceReceiver'

adb uninstall org.coolreader.test >/dev/null || true
adb uninstall org.coolreader >/dev/null || true

install_test_apks
run_instrumentation \
    'phase4-clean-reinstall' \
    'org.coolreader.ReinstallStateInstrumentedTest#reinstallDoesNotAssumeOldGrantOrRestorePrivateState' \
    -e cleanReinstall true
