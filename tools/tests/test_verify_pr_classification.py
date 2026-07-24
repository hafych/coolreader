from __future__ import annotations

import sys
import unittest
from pathlib import Path


sys.path.insert(0, str(Path(__file__).resolve().parents[1]))
from verify_pr_classification import validate  # noqa: E402


def body(
    *,
    classification: str,
    impact: str,
    upstream_reason: str = "Standalone parser fix for upstream",
    removal: str = "",
) -> str:
    classifications = ("upstreamable", "temporary-delta", "fork-only")
    impacts = ("no-delta-change", "delta-updated")
    lines = ["## Fork classification", ""]
    lines.extend(
        f"- [{'x' if item == classification else ' '}] `{item}` — detail"
        for item in classifications
    )
    lines.extend(
        [
            "",
            f"Upstream issue/PR or non-upstream reason: {upstream_reason}",
            f"Temporary-delta removal condition: {removal}",
            "",
        ]
    )
    lines.extend(
        f"- [{'x' if item == impact else ' '}] `{item}` — detail"
        for item in impacts
    )
    return "\n".join(lines)


class PullRequestClassificationTest(unittest.TestCase):
    def test_upstreamable_without_delta_change(self) -> None:
        errors = validate(
            body(
                classification="upstreamable",
                impact="no-delta-change",
            ),
            {"crengine/src/example.cpp"},
        )
        self.assertEqual([], errors)

    def test_temporary_delta_with_updated_ledger(self) -> None:
        errors = validate(
            body(
                classification="temporary-delta",
                impact="delta-updated",
                removal="Remove after upstream API migration",
            ),
            {"FORK_DELTA.md", "android/src/Bridge.java"},
        )
        self.assertEqual([], errors)

    def test_temporary_delta_requires_removal_condition(self) -> None:
        errors = validate(
            body(
                classification="temporary-delta",
                impact="no-delta-change",
            ),
            {"android/src/Bridge.java"},
        )
        self.assertIn(
            "temporary-delta requires a removal condition",
            errors,
        )

    def test_delta_declaration_must_match_changed_files(self) -> None:
        errors = validate(
            body(
                classification="fork-only",
                impact="no-delta-change",
            ),
            {"FORK_DELTA.md"},
        )
        self.assertIn(
            "FORK_DELTA.md changed; select `delta-updated`",
            errors,
        )

    def test_template_placeholders_do_not_count(self) -> None:
        text = body(
            classification="fork-only",
            impact="no-delta-change",
            upstream_reason="<!-- required -->",
        )
        self.assertIn(
            "provide the upstream issue/PR or non-upstream reason",
            validate(text, {"README.md"}),
        )


if __name__ == "__main__":
    unittest.main()
