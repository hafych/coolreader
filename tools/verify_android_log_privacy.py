#!/usr/bin/env python3
"""Enforce the Android diagnostic privacy boundary."""

from pathlib import Path
import re
import sys


ROOT = Path(__file__).resolve().parents[1]
ANDROID_ROOT = ROOT / "android"
ANDROID_SOURCE = ROOT / "android" / "src"
ANDROID_MAIN = ROOT / "android" / "app" / "src" / "main"
JAVA_LOG_WRAPPER = (
    ANDROID_SOURCE / "org" / "coolreader" / "crengine" / "Log.java"
)


def fail(message: str) -> None:
    print(f"Android log privacy policy violation: {message}", file=sys.stderr)
    raise SystemExit(1)


def production_java_files() -> list[Path]:
    source_roots = [ANDROID_SOURCE]
    source_roots.extend(
        path
        for path in ANDROID_ROOT.glob("*/src/*")
        if path.is_dir() and path.name not in {"test", "androidTest"}
    )
    return sorted(
        {
            path
            for source_root in source_roots
            for path in source_root.rglob("*.java")
        }
    )


def relative(path: Path) -> str:
    return str(path.relative_to(ROOT))


def verify_java_boundary() -> None:
    forbidden = (
        (
            re.compile(
                r"\bimport\s+(?:static\s+)?android\.util\.Log(?:\.|\s*;)"
            ),
            "imports android.util.Log directly",
        ),
        (
            re.compile(r"\.printStackTrace\s*\("),
            "prints an unredacted exception stack",
        ),
        (
            re.compile(r"\bSystem\.(?:out|err)\."),
            "writes directly to stdout/stderr",
        ),
    )
    for path in production_java_files():
        text = path.read_text(encoding="utf-8")
        for pattern, reason in forbidden:
            if pattern.search(text):
                fail(f"{relative(path)} {reason}")
        if path != JAVA_LOG_WRAPPER and "android.util.Log." in text:
            fail(f"{relative(path)} calls android.util.Log directly")

    wrapper = JAVA_LOG_WRAPPER.read_text(encoding="utf-8")
    for required in (
        "LogRedactor.redact(message)",
        "LogRedactor.describeThrowable(error)",
        "android.util.Log.println",
    ):
        if required not in wrapper:
            fail(f"Java Log wrapper is missing {required}")


def verify_crash_and_artifact_paths() -> None:
    manifests = (
        ROOT / "android" / "app" / "src" / "main" / "AndroidManifest.xml",
        ROOT / "android" / "AndroidManifest.xml",
    )
    for manifest_path in manifests:
        manifest = manifest_path.read_text(encoding="utf-8")
        if 'android:name=".CoolReaderApplication"' not in manifest:
            fail(
                f"{relative(manifest_path)} does not install "
                "CoolReaderApplication"
            )

    crash_privacy = (
        ANDROID_SOURCE
        / "org"
        / "coolreader"
        / "crengine"
        / "CrashPrivacy.java"
    ).read_text(encoding="utf-8")
    if "LogRedactor.sanitizeThrowable(error)" not in crash_privacy:
        fail("uncaught Java exceptions are not detached from private data")
    if "previous.uncaughtException(thread, safe)" not in crash_privacy:
        fail("the platform crash handler does not receive the safe throwable")

    logcat_saver = (
        ANDROID_SOURCE
        / "org"
        / "coolreader"
        / "crengine"
        / "LogcatSaver.java"
    ).read_text(encoding="utf-8")
    if "LogRedactor.redactArtifact(diagnostic)" not in logcat_saver:
        fail("exported logcat data is not redacted")


def verify_native_boundary() -> None:
    secure_sink = ROOT / "android" / "jni" / "secure_log.cpp"
    for path in sorted((ROOT / "android" / "jni").glob("*")):
        if path == secure_sink or path.suffix not in {".c", ".cpp", ".h"}:
            continue
        text = path.read_text(encoding="utf-8")
        if "__android_log_" in text:
            fail(f"{relative(path)} bypasses the secure native log sink")
        if re.search(r"\bfprintf\s*\(\s*stderr|\bprintf\s*\(", text):
            fail(f"{relative(path)} writes a native diagnostic directly")

    sink_text = secure_sink.read_text(encoding="utf-8")
    if "CRRedactLogMessage" not in sink_text:
        fail("secure native log sink does not redact messages")

    macros = (ROOT / "android" / "jni" / "cr3java.h").read_text(
        encoding="utf-8"
    )
    if "cr3_secure_log_print" not in macros:
        fail("native LOG macros do not use the secure sink")

    engine = (ROOT / "android" / "jni" / "cr3engine.cpp").read_text(
        encoding="utf-8"
    )
    if "cr3_secure_log_write(level, LOG_TAG, buffer)" not in engine:
        fail("CRLog Android adapter does not use the secure sink")


def verify_ci_gate() -> None:
    workflow = (ROOT / ".github" / "workflows" / "build.yml").read_text(
        encoding="utf-8"
    )
    if "python3 tools/verify_android_log_privacy.py" not in workflow:
        fail("GitHub Actions does not run this policy gate")


def main() -> None:
    verify_java_boundary()
    verify_crash_and_artifact_paths()
    verify_native_boundary()
    verify_ci_gate()
    print("Android log privacy policy verified.")


if __name__ == "__main__":
    main()
