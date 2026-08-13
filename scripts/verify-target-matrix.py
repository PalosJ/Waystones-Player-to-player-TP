#!/usr/bin/env python3
"""Check duplicated target IDs in settings and workflow matrices."""

from __future__ import annotations

import argparse
import json
import os
from pathlib import Path
import re
import subprocess
from typing import Dict, List


ROOT = Path(__file__).resolve().parents[1]
MATRIX_PATH = ROOT / "gradle" / "targets.json"


def parse_args() -> argparse.Namespace:
    parser = argparse.ArgumentParser(description="Verify target IDs against targets.json.")
    parser.add_argument("--branch", help="Branch name; defaults to the checked-out branch.")
    return parser.parse_args()


def branch_name() -> str:
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


def require(condition: bool, message: str) -> None:
    if not condition:
        raise ValueError(message)


def validate_matrix_shape(matrix: Dict[str, object]) -> None:
    """Reject structurally ambiguous targets before Gradle expands any metadata."""
    version = matrix.get("modVersion")
    require(version == "1.0.0", f"targets.json must keep modVersion=1.0.0, got {version!r}")

    targets = matrix.get("targets")
    require(isinstance(targets, list) and targets, "targets.json must contain a non-empty targets list")
    target_ids = [target.get("id") for target in targets if isinstance(target, dict)]
    require(len(target_ids) == len(targets), "every target must be an object")
    require(len(set(target_ids)) == len(target_ids), "targets.json contains duplicate target IDs")

    artifact_files = [target.get("artifactFile") for target in targets]
    require(len(set(artifact_files)) == len(artifact_files), "targets.json contains duplicate artifact filenames")
    require(
        isinstance(matrix.get("iconSha256"), str)
        and re.fullmatch(r"[0-9a-f]{64}", matrix["iconSha256"]) is not None,
        "targets.json iconSha256 must be a lowercase SHA-256 digest",
    )

    for target in targets:
        target_id = target.get("id")
        branch = target.get("branch")
        loader = target.get("loader")
        minecraft = target.get("minecraft")
        compile_data = target.get("compile")
        compile_target = compile_data.get("minecraft") if isinstance(compile_data, dict) else None
        release_version = target.get("releaseVersion")
        artifact_file = target.get("artifactFile")

        require(branch in {"main", "neoforge/1.21.x", "fabric/1.21.x"},
                f"{target_id}: unsupported branch {branch!r}")
        require(loader in {"neoforge", "fabric"}, f"{target_id}: unsupported loader {loader!r}")
        require(isinstance(minecraft, list) and minecraft, f"{target_id}: minecraft must be a non-empty list")
        require(len(set(minecraft)) == len(minecraft), f"{target_id}: duplicate Minecraft versions")
        require(isinstance(compile_target, str) and compile_target in minecraft,
                f"{target_id}: compile.minecraft must be one of minecraft")
        require(release_version == version,
                f"{target_id}: releaseVersion must match matrix modVersion {version!r}")
        require(
            artifact_file == f"waystonesplayer-{loader}-{'-'.join(minecraft)}-{version}.jar",
            f"{target_id}: artifactFile does not match loader, Minecraft list, and version",
        )
        require(target_id == f"{loader}-{'-'.join(minecraft)}",
                f"{target_id}: target ID does not match loader and Minecraft list")

        if branch == "main":
            require(loader == "neoforge" and minecraft == ["1.21.1"],
                    f"{target_id}: main must remain NeoForge 1.21.1")
        elif branch == "neoforge/1.21.x":
            require(loader == "neoforge" and "1.21.1" not in minecraft,
                    f"{target_id}: NeoForge unified branch cannot contain 1.21.1")
        else:
            require(loader == "fabric", f"{target_id}: Fabric unified branch must use Fabric loader")

        runtime_stacks = target.get("runtimeStacks")
        if len(minecraft) == 2:
            require(minecraft == ["1.21.2", "1.21.3"],
                    f"{target_id}: only the 1.21.2/1.21.3 pair may share one artifact")
            require(isinstance(runtime_stacks, list),
                    f"{target_id}: shared target must declare runtimeStacks")
            require(all(isinstance(stack, dict) for stack in runtime_stacks),
                    f"{target_id}: every runtime stack must be an object")
            runtime_versions = [stack.get("minecraft") for stack in runtime_stacks]
            require(runtime_versions == minecraft,
                    f"{target_id}: runtimeStacks must cover each supported Minecraft version once")
            for stack in runtime_stacks:
                require(isinstance(stack.get("minimum"), dict) and isinstance(stack.get("current"), dict),
                        f"{target_id}/{stack.get('minecraft')}: runtime stack needs minimum and current")
        else:
            require(not runtime_stacks,
                    f"{target_id}: single-version target must not declare runtimeStacks")

    main_targets = [target for target in targets if target.get("branch") == "main"]
    require(len(main_targets) == 1 and main_targets[0].get("id") == "neoforge-1.21.1",
            "targets.json must contain exactly one canonical main target")

    versions_by_branch = {
        branch: [version for target in targets if target.get("branch") == branch
                 for version in target.get("minecraft", [])]
        for branch in ("neoforge/1.21.x", "fabric/1.21.x")
    }
    minecraft_sort_key = lambda version: tuple(int(part) for part in version.split("."))
    require(
        sorted(versions_by_branch["neoforge/1.21.x"], key=minecraft_sort_key) ==
        sorted([f"1.21.{minor}" for minor in range(2, 12)], key=minecraft_sort_key),
        "NeoForge unified matrix must cover Minecraft 1.21.2 through 1.21.11 exactly",
    )
    require(
        sorted(versions_by_branch["fabric/1.21.x"], key=minecraft_sort_key) ==
        sorted([f"1.21.{minor}" for minor in range(1, 12)], key=minecraft_sort_key),
        "Fabric unified matrix must cover Minecraft 1.21.1 through 1.21.11 exactly",
    )


def main() -> int:
    args = parse_args()
    branch = args.branch or branch_name()
    matrix = json.loads(MATRIX_PATH.read_text(encoding="utf-8"))
    validate_matrix_shape(matrix)
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
