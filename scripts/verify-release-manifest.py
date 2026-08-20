#!/usr/bin/env python3
"""Verify that a downloaded minimum JAR matches its Build provenance manifest."""

from __future__ import annotations

import argparse
import hashlib
import json
from pathlib import Path
from typing import Dict


def sha256(path: Path) -> str:
    digest = hashlib.sha256()
    with path.open("rb") as binary:
        for chunk in iter(lambda: binary.read(1024 * 1024), b""):
            digest.update(chunk)
    return digest.hexdigest()


def verify(
        manifest_path: Path,
        binary_path: Path,
        target: str,
        branch: str,
        commit: str,
        expected_sha256: str) -> Dict[str, object]:
    manifest = json.loads(manifest_path.read_text(encoding="utf-8"))
    if manifest.get("schemaVersion") != 2 or manifest.get("modVersion") != "1.0.0":
        raise ValueError("release manifest has an unsupported schema or mod version")
    if manifest.get("branch") != branch or manifest.get("commit") != commit:
        raise ValueError("release manifest branch or commit does not match the Build run")

    entries = [entry for entry in manifest.get("artifacts", []) if entry.get("target") == target]
    if len(entries) != 1:
        raise ValueError(f"release manifest must contain exactly one entry for {target}")
    entry = entries[0]
    actual_sha256 = sha256(binary_path)
    expected_values = {
        "artifactFile": binary_path.name,
        "branch": branch,
        "buildStack": "minimum",
        "commit": commit,
        "releaseVersion": "1.0.0",
        "sha256": expected_sha256,
        "target": target,
    }
    for key, expected in expected_values.items():
        if entry.get(key) != expected:
            raise ValueError(
                f"release manifest {target} {key} mismatch: {entry.get(key)!r} != {expected!r}"
            )
    if actual_sha256 != expected_sha256:
        raise ValueError(
            f"downloaded binary SHA-256 mismatch: {actual_sha256} != {expected_sha256}"
        )
    return entry


def parse_args() -> argparse.Namespace:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--manifest", type=Path, required=True)
    parser.add_argument("--binary", type=Path, required=True)
    parser.add_argument("--target", required=True)
    parser.add_argument("--branch", required=True)
    parser.add_argument("--commit", required=True)
    parser.add_argument("--sha256", required=True)
    return parser.parse_args()


def main() -> int:
    args = parse_args()
    verify(
        args.manifest,
        args.binary,
        args.target,
        args.branch,
        args.commit,
        args.sha256,
    )
    print(f"Verified {args.binary.name} against {args.manifest}")
    return 0


if __name__ == "__main__":
    try:
        raise SystemExit(main())
    except (OSError, ValueError, KeyError, json.JSONDecodeError) as error:
        raise SystemExit(f"verify-release-manifest: {error}")
