#!/usr/bin/env python3
"""Validate the fork classification contract in a pull request body."""

from __future__ import annotations

import argparse
import os
import re
from pathlib import Path


CLASSIFICATIONS = ("upstreamable", "temporary-delta", "fork-only")
DELTA_IMPACTS = ("no-delta-change", "delta-updated")


def selected_markers(body: str, markers: tuple[str, ...]) -> tuple[list[str], list[str]]:
    selected: list[str] = []
    errors: list[str] = []
    for marker in markers:
        matches = re.findall(
            rf"(?mi)^\s*-\s*\[([ xX])\]\s+`{re.escape(marker)}`(?:\s|—|$)",
            body,
        )
        if len(matches) != 1:
            errors.append(
                f"expected exactly one `{marker}` checkbox, found {len(matches)}"
            )
        elif matches[0].casefold() == "x":
            selected.append(marker)
    return selected, errors


def field_value(body: str, label: str) -> str | None:
    match = re.search(
        rf"(?mi)^[ \t]*{re.escape(label)}:[ \t]*(\S.*)$",
        body,
    )
    return match.group(1).strip() if match else None


def validate(body: str, changed_files: set[str] | None = None) -> list[str]:
    body = re.sub(r"<!--.*?-->", "", body, flags=re.DOTALL)
    classifications, errors = selected_markers(body, CLASSIFICATIONS)
    impacts, impact_errors = selected_markers(body, DELTA_IMPACTS)
    errors.extend(impact_errors)
    if len(classifications) != 1:
        errors.append(
            "select exactly one fork classification: "
            + ", ".join(CLASSIFICATIONS)
        )
    if len(impacts) != 1:
        errors.append(
            "select exactly one fork delta impact: "
            + ", ".join(DELTA_IMPACTS)
        )
    if field_value(body, "Upstream issue/PR or non-upstream reason") is None:
        errors.append("provide the upstream issue/PR or non-upstream reason")
    if (
        classifications == ["temporary-delta"]
        and field_value(body, "Temporary-delta removal condition") is None
    ):
        errors.append("temporary-delta requires a removal condition")
    if changed_files is not None and len(impacts) == 1:
        delta_changed = "FORK_DELTA.md" in changed_files
        if impacts[0] == "delta-updated" and not delta_changed:
            errors.append(
                "`delta-updated` selected but FORK_DELTA.md is unchanged"
            )
        if impacts[0] == "no-delta-change" and delta_changed:
            errors.append(
                "FORK_DELTA.md changed; select `delta-updated`"
            )
    return errors


def parse_args() -> argparse.Namespace:
    parser = argparse.ArgumentParser()
    parser.add_argument(
        "--body-env",
        default="PR_BODY",
        help="environment variable containing the pull request body",
    )
    parser.add_argument("--changed-files-file", type=Path)
    return parser.parse_args()


def main() -> None:
    args = parse_args()
    body = os.environ.get(args.body_env, "")
    changed_files = None
    if args.changed_files_file is not None:
        changed_files = set(
            args.changed_files_file.read_text(encoding="utf-8").splitlines()
        )
    errors = validate(body, changed_files)
    if errors:
        raise RuntimeError(
            "Pull request classification is invalid:\n"
            + "\n".join(f"- {error}" for error in errors)
        )
    print("Pull request classification and fork delta impact are valid")


if __name__ == "__main__":
    main()
