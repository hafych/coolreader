#!/usr/bin/env python3
"""Validate performance budgets and optionally evaluate measured results."""

from __future__ import annotations

import argparse
import json
import math
from pathlib import Path
from typing import Any


ROOT = Path(__file__).resolve().parents[1]
DEFAULT_BUDGETS = ROOT / "performance-budgets.json"
REQUIRED_SCENARIOS = {
    "cold_start",
    "open_large_book",
    "page_turn_lcd",
    "page_turn_eink",
    "search_large_book",
    "scan_10000_books",
    "peak_pss_large_book",
}
ALLOWED_UNITS = {"ms", "MiB"}
ALLOWED_STATISTICS = {"median", "p95", "max"}


def parse_args() -> argparse.Namespace:
    parser = argparse.ArgumentParser()
    parser.add_argument("--budgets", type=Path, default=DEFAULT_BUDGETS)
    parser.add_argument(
        "--results",
        type=Path,
        help="Optional measurement JSON to evaluate against hard limits",
    )
    parser.add_argument(
        "--strict-targets",
        action="store_true",
        help="Also fail when a result misses its target",
    )
    return parser.parse_args()


def read_json(path: Path) -> Any:
    with path.open(encoding="utf-8") as stream:
        return json.load(stream)


def percentile(values: list[float], percent: float) -> float:
    ordered = sorted(values)
    rank = max(1, math.ceil(percent * len(ordered)))
    return ordered[rank - 1]


def statistic(values: list[float], name: str) -> float:
    if name == "median":
        return percentile(values, 0.5)
    if name == "p95":
        return percentile(values, 0.95)
    if name == "max":
        return max(values)
    raise ValueError(f"unsupported statistic: {name}")


def validate_budgets(document: dict[str, Any]) -> tuple[
    list[str], dict[str, dict[str, Any]]
]:
    violations: list[str] = []
    if document.get("schemaVersion") != 1:
        violations.append("unsupported performance budget schema")
    environment = document.get("referenceEnvironment", {})
    for field in ("id", "device", "apiLevel", "buildType", "storage"):
        if not environment.get(field):
            violations.append(f"referenceEnvironment.{field} is required")
    policy = document.get("measurementPolicy", {})
    minimum_runs = policy.get("minimumRuns")
    if not isinstance(minimum_runs, int) or minimum_runs < 5:
        violations.append("measurementPolicy.minimumRuns must be at least 5")
    regression = policy.get("allowedRegressionPercent")
    if not isinstance(regression, (int, float)) or not 0 < regression <= 25:
        violations.append(
            "allowedRegressionPercent must be greater than 0 and at most 25"
        )

    scenarios: dict[str, dict[str, Any]] = {}
    for entry in document.get("scenarios", []):
        identifier = entry.get("id")
        if not identifier:
            violations.append("scenario without id")
            continue
        if identifier in scenarios:
            violations.append(f"duplicate scenario: {identifier}")
            continue
        scenarios[identifier] = entry
        for field in ("description", "metric", "fixture"):
            if not entry.get(field):
                violations.append(f"{identifier}.{field} is required")
        if entry.get("unit") not in ALLOWED_UNITS:
            violations.append(f"{identifier} has unsupported unit")
        if entry.get("statistic") not in ALLOWED_STATISTICS:
            violations.append(f"{identifier} has unsupported statistic")
        target = entry.get("target")
        hard_limit = entry.get("hardLimit")
        if not isinstance(target, (int, float)) or target <= 0:
            violations.append(f"{identifier}.target must be positive")
        if not isinstance(hard_limit, (int, float)) or hard_limit <= 0:
            violations.append(f"{identifier}.hardLimit must be positive")
        if isinstance(target, (int, float)) and isinstance(
            hard_limit, (int, float)
        ) and target >= hard_limit:
            violations.append(
                f"{identifier}.target must be below hardLimit"
            )
    missing = REQUIRED_SCENARIOS - scenarios.keys()
    extra = scenarios.keys() - REQUIRED_SCENARIOS
    if missing:
        violations.append(f"missing required scenarios: {sorted(missing)}")
    if extra:
        violations.append(f"unreviewed scenarios: {sorted(extra)}")
    return violations, scenarios


def evaluate_results(
    document: dict[str, Any],
    scenarios: dict[str, dict[str, Any]],
    results: dict[str, Any],
    strict_targets: bool,
) -> list[str]:
    violations: list[str] = []
    expected_environment = document["referenceEnvironment"]["id"]
    if results.get("environmentId") != expected_environment:
        violations.append(
            "result environment mismatch; expected "
            f"{expected_environment}, got {results.get('environmentId')}"
        )
    if not results.get("commit"):
        violations.append("results.commit is required")
    minimum_runs = document["measurementPolicy"]["minimumRuns"]
    runs = results.get("runs", {})
    missing = scenarios.keys() - runs.keys()
    extra = runs.keys() - scenarios.keys()
    if missing:
        violations.append(f"results missing scenarios: {sorted(missing)}")
    if extra:
        violations.append(f"results contain unknown scenarios: {sorted(extra)}")
    for identifier, scenario in scenarios.items():
        values = runs.get(identifier)
        if values is None:
            continue
        if not isinstance(values, list) or len(values) < minimum_runs:
            violations.append(
                f"{identifier} needs at least {minimum_runs} measured runs"
            )
            continue
        if any(
            not isinstance(value, (int, float)) or value < 0
            for value in values
        ):
            violations.append(
                f"{identifier} contains a negative or non-numeric result"
            )
            continue
        observed = statistic(values, scenario["statistic"])
        if observed > scenario["hardLimit"]:
            violations.append(
                f"{identifier} {scenario['statistic']}={observed:g} "
                f"{scenario['unit']} exceeds hard limit "
                f"{scenario['hardLimit']:g}"
            )
        elif strict_targets and observed > scenario["target"]:
            violations.append(
                f"{identifier} {scenario['statistic']}={observed:g} "
                f"{scenario['unit']} misses target {scenario['target']:g}"
            )
        else:
            print(
                f"{identifier}: {scenario['statistic']}={observed:g} "
                f"{scenario['unit']}"
            )
    return violations


def main() -> None:
    args = parse_args()
    document = read_json(args.budgets)
    violations, scenarios = validate_budgets(document)
    if args.results:
        results = read_json(args.results)
        violations.extend(
            evaluate_results(
                document, scenarios, results, args.strict_targets
            )
        )
    if violations:
        raise RuntimeError(
            "Performance budget violations found:\n" + "\n".join(violations)
        )
    suffix = " and results" if args.results else ""
    print(
        f"Performance budgets{suffix} OK: {len(scenarios)} required "
        "scenarios are explicit"
    )


if __name__ == "__main__":
    main()
