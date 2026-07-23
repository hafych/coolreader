#!/usr/bin/env python3
"""Reject Android production code that bypasses the shared HTTP policy."""

from __future__ import annotations

import re
from pathlib import Path


ROOT = Path(__file__).resolve().parents[1]
SOURCE_ROOT = ROOT / "android/src"
POLICY_FILE = SOURCE_ROOT / "org/coolreader/crengine/SecureHttp.java"
FORBIDDEN = {
    r"\.openConnection\s*\(": "direct URLConnection opening",
    r"setInstanceFollowRedirects\s*\(\s*true\s*\)": "automatic redirects",
    r"setDefaultHostnameVerifier\s*\(": "global hostname verifier replacement",
    r"setHostnameVerifier\s*\(": "hostname verifier replacement",
    r"setDefaultSSLSocketFactory\s*\(": "global TLS factory replacement",
    r"setSSLSocketFactory\s*\(": "TLS factory replacement",
    r'setRequestProperty\s*\(\s*"Authorization"': "direct authorization header",
    r'setRequestProperty\s*\(\s*"Referer"': "direct referrer header",
    r"\.setConnectTimeout\s*\(": "per-call connect timeout",
    r"\.setReadTimeout\s*\(": "per-call read timeout",
}


def main() -> None:
    violations: list[str] = []
    for path in sorted(SOURCE_ROOT.rglob("*.java")):
        if path == POLICY_FILE:
            continue
        text = path.read_text(encoding="utf-8")
        for pattern, description in FORBIDDEN.items():
            for match in re.finditer(pattern, text):
                line = text.count("\n", 0, match.start()) + 1
                violations.append(
                    f"{path.relative_to(ROOT)}:{line}: {description}"
                )
    if violations:
        raise RuntimeError(
            "Android HTTP policy bypasses found:\n" + "\n".join(violations)
        )
    print("Android HTTP policy OK: all production connections use SecureHttp")


if __name__ == "__main__":
    main()
