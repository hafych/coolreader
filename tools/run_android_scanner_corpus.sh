#!/usr/bin/env bash
set -euo pipefail

repo_root="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
gradlew="${repo_root}/android/gradlew"
runner="org.coolreader.test/androidx.test.runner.AndroidJUnitRunner"
report_profile="${ANDROID_TEST_REPORT_PROFILE:-scanner-corpus}"
if [[ ! "${report_profile}" =~ ^[A-Za-z0-9._-]+$ ]]; then
    echo "Invalid ANDROID_TEST_REPORT_PROFILE: ${report_profile}" >&2
    exit 2
fi
report_dir="${repo_root}/android/app/build/outputs/androidTest-results/${report_profile}"
report_file="${report_dir}/scanner-corpus-20000.txt"
test_name="org.coolreader.LibraryScannerCorpusInstrumentedTest#scansTwentyThousandBooksAndReportsTimeAndMemory"

mkdir -p "${report_dir}"
"${gradlew}" -p "${repo_root}/android" --no-daemon \
    :app:installDebug :app:installDebugAndroidTest --stacktrace

output="$(adb shell am instrument -w -r \
    -e class "${test_name}" "${runner}")"
printf '%s\n' "${output}" | tee "${report_file}"
if ! grep -Eq 'OK \(1 test\)|OK \(1 test, 0 failures\)' <<<"${output}"; then
    echo "Scanner corpus instrumentation failed" >&2
    exit 1
fi

for metric in \
    corpus_book_count \
    initial_scan_elapsed_ms \
    unchanged_scan_elapsed_ms \
    peak_pss_kib \
    peak_java_heap_bytes
do
    if ! grep -Eq "INSTRUMENTATION_(STATUS|RESULT): ${metric}=[0-9]+" \
        <<<"${output}"
    then
        echo "Scanner corpus metric is missing: ${metric}" >&2
        exit 1
    fi
done

echo "Scanner corpus metrics: ${report_file}"
