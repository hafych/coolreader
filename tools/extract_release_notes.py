#!/usr/bin/env python3
"""Extract the current Unreleased changelog section for a draft release."""

from __future__ import annotations

import argparse
import re
from pathlib import Path


ROOT = Path(__file__).resolve().parents[1]


def main() -> None:
    parser = argparse.ArgumentParser()
    parser.add_argument("--output", type=Path, required=True)
    args = parser.parse_args()
    changelog = (ROOT / "CHANGELOG.md").read_text(encoding="utf-8")
    match = re.search(
        r"^## Unreleased\s*\n(.*?)(?=^## |\Z)",
        changelog,
        flags=re.MULTILINE | re.DOTALL,
    )
    if not match or not match.group(1).strip():
        raise RuntimeError("CHANGELOG.md has no Unreleased release notes")
    args.output.write_text(match.group(1).strip() + "\n", encoding="utf-8")
    print(f"Release notes written to {args.output}")


if __name__ == "__main__":
    main()
