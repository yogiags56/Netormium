#!/usr/bin/env python3
"""Safely import the pinned Netormium 1.0 RC20 source archive into the repo root."""
from __future__ import annotations

import hashlib
import shutil
import sys
import zipfile
from pathlib import Path, PurePosixPath

ARCHIVE = Path("release/Netormium_1.0_RC20_Browser_Only_Development.zip")
EXPECTED_SHA256 = "6d99e5b18cda308737fb05e399229af0ddacec9564e728f71dbc7f5056ef0da6"
EXPECTED_ROOT = "Netormium_1.0_RC20/"
EXPECTED_FILES = 68
REQUIRED = (
    "gateway/server.js",
    "gateway/package.json",
    "src/com/netormium/client/NetormiumMIDlet.java",
    ".github/workflows/browser-build.yml",
    ".devcontainer/devcontainer.json",
    "build.xml",
)

def fail(message: str) -> None:
    print(f"RC20 IMPORT ERROR: {message}", file=sys.stderr)
    raise SystemExit(1)

def main() -> None:
    if not ARCHIVE.is_file():
        fail(f"archive not found: {ARCHIVE}")

    digest = hashlib.sha256(ARCHIVE.read_bytes()).hexdigest()
    if digest != EXPECTED_SHA256:
        fail(f"SHA-256 mismatch: {digest}")

    temp_root = Path(".rc20-import-tmp")
    if temp_root.exists():
        shutil.rmtree(temp_root)

    try:
        with zipfile.ZipFile(ARCHIVE) as archive:
            bad = archive.testzip()
            if bad:
                fail(f"corrupt ZIP member: {bad}")

            files = [item for item in archive.infolist() if not item.is_dir()]
            if len(files) != EXPECTED_FILES:
                fail(f"expected {EXPECTED_FILES} files, found {len(files)}")

            for item in files:
                path = PurePosixPath(item.filename)
                if path.is_absolute() or ".." in path.parts:
                    fail(f"unsafe archive path: {item.filename}")
                if not item.filename.startswith(EXPECTED_ROOT):
                    fail(f"unexpected archive root: {item.filename}")
                # Reject Unix symbolic links; only ordinary files are expected.
                mode = (item.external_attr >> 16) & 0o170000
                if mode == 0o120000:
                    fail(f"symbolic link not allowed: {item.filename}")

            archive.extractall(temp_root)

        source = temp_root / "Netormium_1.0_RC20"
        if not source.is_dir():
            fail("expected top-level source directory is missing")

        for relative in REQUIRED:
            if not (source / relative).is_file():
                fail(f"required source file missing: {relative}")

        shutil.copytree(source, Path("."), dirs_exist_ok=True)
        if Path("android/termux").exists():
            fail("legacy Termux directory unexpectedly exists after import")

        for relative in REQUIRED:
            if not Path(relative).is_file():
                fail(f"required file missing after copy: {relative}")

        print(f"RC20 IMPORT OK: {len(files)} files copied into repository root.")
        print(f"SHA-256: {digest}")
    finally:
        if temp_root.exists():
            shutil.rmtree(temp_root)

if __name__ == "__main__":
    main()
