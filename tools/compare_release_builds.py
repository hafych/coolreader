#!/usr/bin/env python3
"""Require two independently produced release artifact trees to match."""

from __future__ import annotations

import argparse
import hashlib
import struct
from pathlib import Path


EOCD_SIGNATURE = b"PK\x05\x06"
APK_SIGNING_MAGIC = b"APK Sig Block 42"


def parse_args() -> argparse.Namespace:
    parser = argparse.ArgumentParser()
    parser.add_argument("--left", type=Path, required=True)
    parser.add_argument("--right", type=Path, required=True)
    parser.add_argument("--label", required=True)
    return parser.parse_args()


def digest(path: Path) -> str:
    value = hashlib.sha256()
    with path.open("rb") as stream:
        while chunk := stream.read(1024 * 1024):
            value.update(chunk)
    return value.hexdigest()


def files(root: Path) -> dict[str, Path]:
    if not root.is_dir():
        raise RuntimeError(f"release artifact tree not found: {root}")
    return {
        path.relative_to(root).as_posix(): path
        for path in sorted(root.rglob("*"))
        if path.is_file()
    }


def apk_payload(path: Path) -> bytes:
    data = path.read_bytes()
    search_start = max(0, len(data) - 65_557)
    eocd = data.rfind(EOCD_SIGNATURE, search_start)
    if eocd < 0 or eocd + 22 > len(data):
        raise RuntimeError(f"{path}: ZIP end-of-central-directory not found")
    comment_size = struct.unpack_from("<H", data, eocd + 20)[0]
    if eocd + 22 + comment_size != len(data):
        raise RuntimeError(f"{path}: malformed ZIP end record")
    directory_offset = struct.unpack_from("<I", data, eocd + 16)[0]
    footer = directory_offset - 24
    if footer < 8 or data[footer + 8 : directory_offset] != APK_SIGNING_MAGIC:
        raise RuntimeError(f"{path}: APK Signing Block not found")
    block_size = struct.unpack_from("<Q", data, footer)[0]
    block_start = directory_offset - block_size - 8
    if block_start < 0:
        raise RuntimeError(f"{path}: invalid APK Signing Block size")
    if struct.unpack_from("<Q", data, block_start)[0] != block_size:
        raise RuntimeError(f"{path}: APK Signing Block size fields disagree")
    payload = bytearray(data[:block_start] + data[directory_offset:])
    payload_eocd = eocd - (directory_offset - block_start)
    struct.pack_into("<I", payload, payload_eocd + 16, block_start)
    return bytes(payload)


def main() -> None:
    args = parse_args()
    left = files(args.left)
    right = files(args.right)
    violations: list[str] = []
    if left.keys() != right.keys():
        violations.append(
            "file set drift; "
            f"left-only={sorted(left.keys() - right.keys())}, "
            f"right-only={sorted(right.keys() - left.keys())}"
        )
    accepted_apk_variance: list[str] = []
    for name in sorted(left.keys() & right.keys()):
        left_size = left[name].stat().st_size
        right_size = right[name].stat().st_size
        left_digest = digest(left[name])
        right_digest = digest(right[name])
        if left_size != right_size or left_digest != right_digest:
            if name.endswith(".apk"):
                if apk_payload(left[name]) == apk_payload(right[name]):
                    accepted_apk_variance.append(name)
                else:
                    violations.append(
                        f"{name}: payload outside APK Signing Block differs"
                    )
            else:
                violations.append(
                    f"{name}: left={left_size}:{left_digest}, "
                    f"right={right_size}:{right_digest}"
                )
    if violations:
        raise RuntimeError(
            f"{args.label} release is not reproducible:\n"
            + "\n".join(violations)
        )
    if not left:
        raise RuntimeError(f"{args.label} release artifact tree is empty")
    print(
        f"{args.label} release reproducible: "
        f"{len(left) - len(accepted_apk_variance)} byte-identical files"
        + (
            f"; equivalent randomized signing blocks in "
            f"{', '.join(accepted_apk_variance)}"
            if accepted_apk_variance
            else ""
        )
    )


if __name__ == "__main__":
    main()
