#!/usr/bin/env python3
"""Generate deterministic, license-safe performance fixtures outside git."""

from __future__ import annotations

import argparse
import hashlib
import json
from pathlib import Path


HEADER = (
    '<?xml version="1.0" encoding="utf-8"?>\n'
    '<FictionBook xmlns="http://www.gribuser.ru/xml/fictionbook/2.0">\n'
    "<description><title-info><genre>fiction</genre>"
    "<author><first-name>Performance</first-name>"
    "<last-name>Fixture</last-name></author>"
    "<book-title>{title}</book-title><lang>en</lang>"
    "</title-info></description><body><section>\n"
)
FOOTER = "</section></body></FictionBook>\n"
SEARCH_NEEDLE = "COOLREADER_PERFORMANCE_NEEDLE_7F31A9"


def parse_args() -> argparse.Namespace:
    parser = argparse.ArgumentParser()
    parser.add_argument("--output", type=Path, required=True)
    parser.add_argument("--book-mib", type=int, default=50)
    parser.add_argument("--library-count", type=int, default=10_000)
    return parser.parse_args()


def hash_file(path: Path) -> str:
    digest = hashlib.sha256()
    with path.open("rb") as stream:
        while chunk := stream.read(1024 * 1024):
            digest.update(chunk)
    return digest.hexdigest()


def write_large_book(path: Path, size_mib: int) -> None:
    target = size_mib * 1024 * 1024
    header = HEADER.format(title="Large deterministic fixture").encode()
    footer = FOOTER.encode()
    with path.open("wb") as stream:
        stream.write(header)
        index = 0
        while stream.tell() + len(footer) + 256 < target:
            paragraph = (
                f"<p>{index:09d} Deterministic reader performance text. "
                "Alpha beta gamma delta epsilon. "
                "The content is synthetic and contains no private book data."
                "</p>\n"
            ).encode()
            stream.write(paragraph)
            index += 1
        stream.write(f"<p>{SEARCH_NEEDLE}</p>\n".encode())
        stream.write(footer)


def write_library(directory: Path, count: int) -> None:
    directory.mkdir(parents=True, exist_ok=True)
    for index in range(count):
        path = directory / f"book-{index:05d}.fb2"
        content = (
            HEADER.format(title=f"Synthetic book {index:05d}")
            + f"<p>Generated metadata fixture {index:05d}.</p>\n"
            + FOOTER
        )
        path.write_text(content, encoding="utf-8")


def main() -> None:
    args = parse_args()
    if not 1 <= args.book_mib <= 512:
        raise ValueError("--book-mib must be between 1 and 512")
    if not 1 <= args.library_count <= 100_000:
        raise ValueError("--library-count must be between 1 and 100000")
    args.output.mkdir(parents=True, exist_ok=True)
    book = args.output / "large-book.fb2"
    library = args.output / f"library-{args.library_count}"
    write_large_book(book, args.book_mib)
    write_library(library, args.library_count)
    manifest = {
        "schemaVersion": 1,
        "largeBook": {
            "path": book.name,
            "bytes": book.stat().st_size,
            "sha256": hash_file(book),
            "searchNeedle": SEARCH_NEEDLE,
        },
        "library": {
            "path": library.name,
            "bookCount": args.library_count,
        },
    }
    manifest_path = args.output / "manifest.json"
    manifest_path.write_text(
        json.dumps(manifest, indent=2, sort_keys=True) + "\n",
        encoding="utf-8",
    )
    print(
        f"Generated {args.book_mib} MiB book and "
        f"{args.library_count}-book library in {args.output}"
    )


if __name__ == "__main__":
    main()
