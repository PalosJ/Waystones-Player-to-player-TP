#!/usr/bin/env python3
"""Check duplicated target IDs in settings and workflow matrices."""

from __future__ import annotations

import argparse
import json
from pathlib import Path
import re
import subprocess
from typing import List


ROOT = Path(__file__).resolve().parents[1]
MATRIX_PATH = ROOT / "gradle" / "targets.json"


def parse_args() -> argparse.Namespace:
    parser = argparse.ArgumentParser(description="Verify target IDs against targets.json.")
    parser.add_argument("--branch", help="Branch name; defaults to the checked-out branch.")
    return parser.parse_args()


def branch_name() -> str:
    return subprocess.check_output(
        ["git", "branch", "--show-current"], cwd=ROOT, text=True
    ).strip()


def settings_targets(text: str, variable: str) -> List[str]:
    match = re.search(rf"def {re.escape(variable)}\s*=\s*\[(.*?)\]", text, re.DOTALL)
    if match is None:
        raise ValueError(f"settings.gradle has no {variable} list")
    return re.findall(r"['\"]([^'\"]+)['\"]", match.group(1))


def workflow_target_blocks(text: str) -> List[List[str]]:
    blocks = re.findall(
        r"(?m)^        target:\n((?:^          - [^\n]+\n)+)", text
    )
    return [[line.strip()[2:] for line in block.splitlines()] for block in blocks]


def require_equal(label: str, actual: List[str], expected: List[str]) -> None:
    if actual != expected:
        raise ValueError(f"{label} differs from targets.json: {actual!r} != {expected!r}")


def main() -> int:
    args = parse_args()
    branch = args.branch or branch_name()
    matrix = json.loads(MATRIX_PATH.read_text(encoding="utf-8"))
    expected = [target["id"] for target in matrix["targets"] if target["branch"] == branch]
    if not expected:
        raise ValueError(f"no matrix targets belong to branch {branch!r}")

    build_workflow = (ROOT / ".github" / "workflows" / "build.yml").read_text(encoding="utf-8")
    runtime_workflow = (ROOT / ".github" / "workflows" / "runtime-smoke.yml").read_text(encoding="utf-8")
    build_blocks = workflow_target_blocks(build_workflow)
    runtime_blocks = workflow_target_blocks(runtime_workflow)

    if branch == "main":
        neo = [target["id"] for target in matrix["targets"] if target["loader"] == "neoforge" and target["branch"] != "main"]
        fabric = [target["id"] for target in matrix["targets"] if target["loader"] == "fabric"]
        if len(runtime_blocks) != 2:
            raise ValueError("main runtime workflow must contain NeoForge and Fabric target lists")
        require_equal("main runtime NeoForge targets", runtime_blocks[0], neo)
        require_equal("main runtime Fabric targets", runtime_blocks[1], fabric)
    else:
        loader = "neoforge" if branch == "neoforge/1.21.x" else "fabric"
        variable = "neoForgeTargets" if loader == "neoforge" else "fabricTargets"
        settings = (ROOT / "settings.gradle").read_text(encoding="utf-8")
        require_equal("settings.gradle targets", settings_targets(settings, variable), expected)
        if len(build_blocks) != 1 or len(runtime_blocks) != 1:
            raise ValueError("unified build/runtime workflows must each contain one target list")
        require_equal("build workflow targets", build_blocks[0], expected)
        require_equal("runtime workflow targets", runtime_blocks[0], expected)

    print(f"Verified target matrix for {branch}: {len(expected)} matrix target(s).")
    return 0


if __name__ == "__main__":
    try:
        raise SystemExit(main())
    except (OSError, ValueError, KeyError, json.JSONDecodeError, subprocess.CalledProcessError) as error:
        raise SystemExit(f"verify-target-matrix: {error}")
