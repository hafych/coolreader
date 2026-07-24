#!/usr/bin/env python3
"""Write deterministic SHA-256 checksums for a flat release artifact set."""

from __future__ import annotations

import argparse
import hashlib
from pathlib import Path


def parse_args() -> argparse.Namespace:
    parser = argparse.ArgumentParser()
    parser.add_argument("--directory", type=Path, required=True)
    return parser.parse_args()


def digest(path: Path) -> str:
    value = hashlib.sha256()
    with path.open("rb") as stream:
        while chunk := stream.read(1024 * 1024):
            value.update(chunk)
    return value.hexdigest()


def main() -> None:
    args = parse_args()
    if not args.directory.is_dir():
        raise RuntimeError(f"artifact directory not found: {args.directory}")
    checksum_file = args.directory / "SHA256SUMS"
    artifacts = sorted(
        path
        for path in args.directory.iterdir()
        if path.is_file() and path.name != checksum_file.name
    )
    if not artifacts:
        raise RuntimeError("no release artifacts to checksum")
    checksum_file.write_text(
        "".join(f"{digest(path)}  {path.name}\n" for path in artifacts),
        encoding="utf-8",
    )
    print(f"Wrote {len(artifacts)} checksums to {checksum_file}")


if __name__ == "__main__":
    main()
