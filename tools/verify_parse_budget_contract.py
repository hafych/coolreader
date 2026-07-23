#!/usr/bin/env python3
"""Verify the mirrored Java/native ParseBudget wire contract."""

from __future__ import annotations

import ast
import re
from pathlib import Path


ROOT = Path(__file__).resolve().parents[1]
CPP_PATH = ROOT / "crengine/include/parsebudget.h"
JAVA_PATH = ROOT / "android/src/org/coolreader/crengine/ParseBudget.java"


def parse_integer_expression(expression: str) -> int:
    cleaned = re.sub(r"(?<=\d)(?:ULL|LL|UL|L|U)\b", "", expression)
    tree = ast.parse(cleaned.strip(), mode="eval")

    def evaluate(node: ast.AST) -> int:
        if isinstance(node, ast.Expression):
            return evaluate(node.body)
        if isinstance(node, ast.Constant) and isinstance(node.value, int):
            return node.value
        if isinstance(node, ast.BinOp) and isinstance(node.op, ast.Mult):
            return evaluate(node.left) * evaluate(node.right)
        raise ValueError(f"unsupported integer expression: {expression}")

    return evaluate(tree)


def require_match(pattern: str, text: str, label: str) -> re.Match[str]:
    match = re.search(pattern, text, re.MULTILINE)
    if match is None:
        raise RuntimeError(f"cannot find {label}")
    return match


def main() -> None:
    cpp = CPP_PATH.read_text(encoding="utf-8")
    java = JAVA_PATH.read_text(encoding="utf-8")

    cpp_codes = {
        name: int(code)
        for name, code in re.findall(
            r"^\s*PARSE_BUDGET_([A-Z_]+)\s*=\s*(\d+),?$", cpp, re.MULTILINE
        )
        if name != "OK"
    }
    cpp_names = dict(
        re.findall(
            r"case\s+PARSE_BUDGET_([A-Z_]+):\s*return\s+\"([^\"]+)\";", cpp
        )
    )
    cpp_names.pop("OK", None)
    java_entries = {
        name: (int(code), wire_name)
        for name, code, wire_name in re.findall(
            r"^\s*([A-Z_]+)\((\d+),\s*\"([^\"]+)\"\)[,;]$",
            java,
            re.MULTILINE,
        )
    }

    if set(cpp_codes) != set(java_entries):
        raise RuntimeError(
            "ParseBudget code sets differ: "
            f"native-only={sorted(set(cpp_codes) - set(java_entries))}, "
            f"java-only={sorted(set(java_entries) - set(cpp_codes))}"
        )
    for name, code in cpp_codes.items():
        java_code, java_wire_name = java_entries[name]
        if code != java_code or cpp_names.get(name) != java_wire_name:
            raise RuntimeError(
                f"ParseBudget mismatch for {name}: "
                f"native=({code}, {cpp_names.get(name)!r}), "
                f"java=({java_code}, {java_wire_name!r})"
            )

    cpp_input = require_match(
        r"limits\.maxInputBytes\s*=\s*([^;]+);",
        cpp,
        "native input byte limit",
    ).group(1)
    java_input = require_match(
        r"MAX_DOCUMENT_INPUT_BYTES\s*=\s*([^;]+);",
        java,
        "Java input byte limit",
    ).group(1)
    if parse_integer_expression(cpp_input) != parse_integer_expression(java_input):
        raise RuntimeError("Java/native document input byte limits differ")

    print(
        f"ParseBudget contract OK: {len(cpp_codes)} codes, "
        f"{parse_integer_expression(cpp_input)} input bytes"
    )


if __name__ == "__main__":
    main()
