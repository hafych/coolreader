#!/usr/bin/env python3
"""Create deterministic desktop release archives from a CMake install tree."""

from __future__ import annotations

import argparse
import datetime as dt
import gzip
import json
import os
import stat
import tarfile
import zipfile
from pathlib import Path


ROOT = Path(__file__).resolve().parents[1]
METADATA_FILES = (
    ROOT / "release-version.json",
    ROOT / "CHANGELOG.md",
    ROOT / "FORK_DELTA.md",
    ROOT / "LICENSE",
)


def parse_args() -> argparse.Namespace:
    parser = argparse.ArgumentParser()
    parser.add_argument("--input-dir", type=Path, required=True)
    parser.add_argument("--output-dir", type=Path, required=True)
    parser.add_argument("--platform", required=True)
    parser.add_argument(
        "--format", choices=("tar.gz", "zip"), required=True
    )
    return parser.parse_args()


def version_name() -> str:
    with (ROOT / "release-version.json").open(encoding="utf-8") as stream:
        return json.load(stream)["versionName"]


def epoch() -> int:
    value = int(os.environ.get("SOURCE_DATE_EPOCH", "315532800"))
    return max(value, 315532800)


def archive_root(platform: str) -> str:
    return f"coolreader-{version_name()}-{platform}"


def installed_paths(input_dir: Path) -> list[Path]:
    paths = sorted(
        input_dir.rglob("*"),
        key=lambda path: path.relative_to(input_dir).as_posix(),
    )
    if not paths:
        raise RuntimeError(f"empty install tree: {input_dir}")
    return paths


def normalized_mode(path: Path) -> int:
    mode = stat.S_IMODE(path.lstat().st_mode)
    if path.is_dir():
        return 0o755
    if path.is_symlink():
        return 0o777
    return 0o755 if mode & 0o111 else 0o644


def add_tar_path(
    archive: tarfile.TarFile,
    source: Path,
    name: str,
    timestamp: int,
) -> None:
    info = archive.gettarinfo(str(source), arcname=name)
    info.uid = 0
    info.gid = 0
    info.uname = "root"
    info.gname = "root"
    info.mtime = timestamp
    info.mode = normalized_mode(source)
    if info.isfile():
        with source.open("rb") as stream:
            archive.addfile(info, stream)
    else:
        archive.addfile(info)


def write_tar(input_dir: Path, output: Path, root_name: str) -> None:
    timestamp = epoch()
    with output.open("wb") as raw:
        with gzip.GzipFile(
            filename="", mode="wb", fileobj=raw, mtime=timestamp
        ) as compressed:
            with tarfile.open(fileobj=compressed, mode="w") as archive:
                root = tarfile.TarInfo(root_name + "/")
                root.type = tarfile.DIRTYPE
                root.mode = 0o755
                root.uid = root.gid = 0
                root.uname = root.gname = "root"
                root.mtime = timestamp
                archive.addfile(root)
                for path in installed_paths(input_dir):
                    relative = path.relative_to(input_dir).as_posix()
                    add_tar_path(
                        archive, path, f"{root_name}/{relative}", timestamp
                    )
                for path in METADATA_FILES:
                    add_tar_path(
                        archive,
                        path,
                        f"{root_name}/release-metadata/{path.name}",
                        timestamp,
                    )


def zip_info(name: str, mode: int, timestamp: int) -> zipfile.ZipInfo:
    date = tuple(
        dt.datetime.fromtimestamp(
            timestamp, tz=dt.timezone.utc
        ).timetuple()[:6]
    )
    info = zipfile.ZipInfo(name, date_time=date)
    info.create_system = 3
    info.external_attr = mode << 16
    info.compress_type = zipfile.ZIP_DEFLATED
    return info


def write_zip(input_dir: Path, output: Path, root_name: str) -> None:
    timestamp = epoch()
    with zipfile.ZipFile(
        output, mode="w", compression=zipfile.ZIP_DEFLATED, compresslevel=9
    ) as archive:
        archive.writestr(
            zip_info(root_name + "/", stat.S_IFDIR | 0o755, timestamp),
            b"",
        )
        for path in installed_paths(input_dir):
            relative = path.relative_to(input_dir).as_posix()
            name = f"{root_name}/{relative}"
            mode = normalized_mode(path)
            if path.is_symlink():
                archive.writestr(
                    zip_info(name, stat.S_IFLNK | mode, timestamp),
                    os.readlink(path).encode("utf-8"),
                )
            elif path.is_dir():
                archive.writestr(
                    zip_info(name + "/", stat.S_IFDIR | mode, timestamp),
                    b"",
                )
            else:
                archive.writestr(
                    zip_info(name, stat.S_IFREG | mode, timestamp),
                    path.read_bytes(),
                )
        for path in METADATA_FILES:
            name = f"{root_name}/release-metadata/{path.name}"
            archive.writestr(
                zip_info(name, stat.S_IFREG | 0o644, timestamp),
                path.read_bytes(),
            )


def main() -> None:
    args = parse_args()
    if not args.input_dir.is_dir():
        raise RuntimeError(f"install tree does not exist: {args.input_dir}")
    args.output_dir.mkdir(parents=True, exist_ok=True)
    root_name = archive_root(args.platform)
    output = args.output_dir / f"{root_name}.{args.format}"
    if args.format == "tar.gz":
        write_tar(args.input_dir, output, root_name)
    else:
        write_zip(args.input_dir, output, root_name)
    print(f"Desktop release archive written to {output}")


if __name__ == "__main__":
    main()
