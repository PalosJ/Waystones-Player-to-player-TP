#!/usr/bin/env python3
"""Write a deterministic provenance manifest for the minimum-built artifacts.

The manifest is deliberately written under the ignored build directory by default;
it is release evidence, not a repository source file or a replacement for JAR gates.
"""

from __future__ import annotations

import argparse
import hashlib
import json
import os
from pathlib import Path
import subprocess
from typing import Dict, List


ROOT = Path(__file__).resolve().parents[1]
MATRIX_PATH = ROOT / "gradle" / "targets.json"


def parse_args() -> argparse.Namespace:
    parser = argparse.ArgumentParser(description="Create a release JAR provenance manifest.")
    parser.add_argument("--branch", help="Branch name; defaults to the checked-out branch.")
    parser.add_argument(
        "--output",
        default="build/release-manifest.json",
        help="Output path relative to the repository root (default: build/release-manifest.json)",
    )
    return parser.parse_args()


def checked_out_branch() -> str:
    checked_out = subprocess.check_output(
        ["git", "branch", "--show-current"], cwd=ROOT, text=True
    ).strip()
    if checked_out:
        return checked_out
    for environment_key in ("GITHUB_HEAD_REF", "GITHUB_REF_NAME"):
        value = os.environ.get(environment_key, "").strip()
        if value:
            return value
    raise ValueError(
        "cannot infer branch from detached HEAD; pass --branch or set GITHUB_HEAD_REF/GITHUB_REF_NAME"
    )


def sha256(path: Path) -> str:
    digest = hashlib.sha256()
    with path.open("rb") as binary:
        for chunk in iter(lambda: binary.read(1024 * 1024), b""):
            digest.update(chunk)
    return digest.hexdigest()


def artifact_path(target: Dict[str, object]) -> Path:
    if target["branch"] == "main":
        return ROOT / "neoforge" / "build" / "libs" / target["artifactFile"]
    return ROOT / "targets" / target["id"] / "build" / "libs" / target["artifactFile"]


def target_entry(target: Dict[str, object], branch: str, commit: str) -> Dict[str, object]:
    path = artifact_path(target)
    if not path.is_file():
        raise FileNotFoundError(f"missing minimum-built artifact: {path}")
    return {
        "artifactFile": target["artifactFile"],
        "size": path.stat().st_size,
        "sha256": sha256(path),
        "target": target["id"],
        "branch": branch,
        "loader": target["loader"],
        "minecraft": target["minecraft"],
        "releaseVersion": target["releaseVersion"],
        "buildStack": "minimum",
        "minimum": target["minimum"],
        "current": target["current"],
        "runtimeStacks": target.get("runtimeStacks", []),
        "commit": commit,
    }


def main() -> int:
    args = parse_args()
    branch = args.branch or checked_out_branch()
    matrix = json.loads(MATRIX_PATH.read_text(encoding="utf-8"))
    targets: List[Dict[str, object]] = [
        target for target in matrix["targets"] if target["branch"] == branch
    ]
    if not targets:
        raise ValueError(f"no matrix targets belong to branch {branch!r}")
    commit = subprocess.check_output(
        ["git", "rev-parse", "HEAD"], cwd=ROOT, text=True
    ).strip()
    manifest = {
        "schemaVersion": 1,
        "modVersion": matrix["modVersion"],
        "branch": branch,
        "commit": commit,
        "iconSha256": matrix["iconSha256"],
        "artifacts": [target_entry(target, branch, commit) for target in targets],
    }
    output = (ROOT / args.output).resolve()
    output.parent.mkdir(parents=True, exist_ok=True)
    output.write_text(json.dumps(manifest, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")
    print(f"Wrote {output.relative_to(ROOT)} ({len(targets)} artifacts)")
    return 0


if __name__ == "__main__":
    try:
        raise SystemExit(main())
    except (OSError, ValueError, KeyError, json.JSONDecodeError, subprocess.CalledProcessError) as error:
        raise SystemExit(f"release-manifest: {error}")
