#!/usr/bin/env python3
"""Check duplicated target IDs in settings and workflow matrices."""

from __future__ import annotations

import argparse
import hashlib
import json
import os
from pathlib import Path
import re
import subprocess
from typing import Dict, List


ROOT = Path(__file__).resolve().parents[1]
MATRIX_PATH = ROOT / "gradle" / "targets.json"
BRANCH_LOADERS = {
    "neoforge/1.21.x": "neoforge",
    "fabric/1.21.x": "fabric",
    "neoforge/26.x": "neoforge",
    "fabric/26.x": "fabric",
}


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


def parse_target_properties(path: Path) -> Dict[str, str]:
    properties: Dict[str, str] = {}
    for line_number, raw_line in enumerate(path.read_text(encoding="utf-8").splitlines(), 1):
        line = raw_line.strip()
        if not line or line.startswith("#"):
            continue
        if "=" not in line:
            raise ValueError(f"{path}: line {line_number} is not key=value")
        key, value = line.split("=", 1)
        key = key.strip()
        if key in properties:
            raise ValueError(f"{path}: duplicate key {key!r}")
        properties[key] = value.strip()
    return properties


def comma_values(value: str) -> List[str]:
    return [item.strip() for item in value.split(",") if item.strip()]


def expected_adapter_roots(target: Dict[str, object]) -> List[str]:
    loader = target["loader"]
    families = target["families"]
    expected: List[str] = []

    balm_family = families["balm"]
    if balm_family == "runnable-21.3":
        expected.append(f"loader/{loader}-runnable-21.3")
    elif balm_family == "platform-module":
        expected.extend([
            "balm/platform-1.21.11",
            f"loader/{loader}-platform-1.21.11",
        ])
    elif balm_family == "load-context-26":
        expected.append(f"loader/{loader}-load-context-26")
    elif balm_family != "legacy-module":
        raise ValueError(f"{target['id']}: unknown Balm family {balm_family!r}")

    teleport_family = families["teleport"]
    if teleport_family in {"legacy-21.3-21.9", "context-optional-hand-21.10"}:
        expected.extend([
            "teleport/legacy-21.3",
            "teleport/context-optional-hand-21.3-21.10",
        ])
    elif teleport_family == "identifier-21.11":
        expected.append("teleport/identifier-1.21.11")
    elif teleport_family == "shogi-26":
        expected.append("teleport/shogi-26")
    elif teleport_family != "legacy-1.21.1":
        raise ValueError(f"{target['id']}: unknown teleport family {teleport_family!r}")

    screen_family = families["screen"]
    if screen_family in {"legacy-input", "event-input"} and "1.21.1" not in target["minecraft"]:
        expected.append("screen/legacy-1.21.3-1.21.10")
    elif screen_family == "identifier-skin":
        expected.append("screen/platform-1.21.11")
    elif screen_family == "graphics-extractor-26":
        expected.append("screen/graphics-extractor-26")
    elif screen_family not in {"legacy-input", "event-input"}:
        raise ValueError(f"{target['id']}: unknown screen family {screen_family!r}")
    return expected


def validate_target_properties(
        root: Path,
        branch: str,
        targets: List[Dict[str, object]]) -> None:
    loader = BRANCH_LOADERS.get(branch)
    require(loader is not None, f"unsupported target-properties branch {branch!r}")
    loader_excludes_key = "neoForgeExcludes" if loader == "neoforge" else "fabricExcludes"
    allowed_keys = {"targetId", "commonExcludes", loader_excludes_key, "adapterRoots"}
    for target in targets:
        target_id = target["id"]
        path = root / "targets" / target_id / "target.properties"
        require(path.is_file(), f"{target_id}: missing target.properties")
        properties = parse_target_properties(path)
        unknown_keys = sorted(set(properties).difference(allowed_keys))
        require(not unknown_keys, f"{target_id}: unknown target.properties keys {unknown_keys}")
        require(properties.get("targetId") == target_id,
                f"{target_id}: target.properties targetId mismatch")

        adapter_roots = comma_values(properties.get("adapterRoots", ""))
        require(len(adapter_roots) == len(set(adapter_roots)),
                f"{target_id}: duplicate adapterRoots")
        for adapter_root in adapter_roots:
            java_root = root / "adapters" / adapter_root / "java"
            require(java_root.is_dir() and any(java_root.rglob("*.java")),
                    f"{target_id}: adapter root {adapter_root!r} has no Java sources")

        required_roots = expected_adapter_roots(target)
        for expected_root in required_roots:
            require(expected_root in adapter_roots,
                    f"{target_id}: {target['families']} requires adapter root {expected_root!r}")
        expected_loader_roots = {
            root_name for root_name in required_roots if root_name.startswith("loader/")
        }
        actual_loader_roots = {
            root_name for root_name in adapter_roots if root_name.startswith("loader/")
        }
        require(actual_loader_roots == expected_loader_roots,
                f"{target_id}: loader adapter roots do not match its Balm family")

        teleport_family = target["families"]["teleport"]
        common_excludes = comma_values(properties.get("commonExcludes", ""))
        locked_context = "com/palosj/waystonesplayer/compat/LockedWaystoneTeleportContext.java"
        if teleport_family in {"legacy-21.3-21.9", "context-optional-hand-21.10"}:
            require(locked_context in common_excludes,
                    f"{target_id}: optional-hand family must replace the common locked context")
            require("teleport/context-no-hand-21.3-21.9" not in adapter_roots,
                    f"{target_id}: obsolete no-hand context root is forbidden")

        source_directories = {
            "commonExcludes": root / "common" / "src" / "main" / "java",
            loader_excludes_key: root / loader / "src" / "main" / "java",
        }
        for key, source_directory in source_directories.items():
            values = comma_values(properties.get(key, ""))
            require(len(values) == len(set(values)), f"{target_id}: duplicate {key}")
            for relative_path in values:
                require(not any(character in relative_path for character in "?*[]"),
                        f"{target_id}: {key} must use exact paths")
                require((source_directory / relative_path).is_file(),
                        f"{target_id}: {key} references missing source {relative_path!r}")


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
    build_tools = matrix.get("buildTools")
    require(isinstance(build_tools, dict), "targets.json must contain buildTools")
    require(
        build_tools.get("1.21") == {
            "java": 21,
            "neoforgeGradle": "9.2.1",
            "fabricGradle": "9.5.1",
            "modDevGradle": "2.0.143",
            "loom": "1.17.19",
        },
        "targets.json 1.21 build tool profile changed",
    )
    require(
        build_tools.get("26") == {
            "java": 25,
            "neoforgeGradle": "9.2.1",
            "fabricGradle": "9.5.1",
            "modDevGradle": "2.0.143",
            "loom": "1.17.19",
            "sourceGradle": "9.3.1",
            "sourceLoom": "1.14-20251223.202653-9",
            "sourceShogiApi": "26.1.0.1-20260324.181500-45",
        },
        "targets.json 26 build tool profile changed",
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

        require(branch in {"main", *BRANCH_LOADERS},
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
        elif branch == "fabric/1.21.x":
            require(loader == "fabric", f"{target_id}: Fabric unified branch must use Fabric loader")
        else:
            require(loader == BRANCH_LOADERS[branch],
                    f"{target_id}: 26.x branch loader mismatch")
            require(minecraft[0].startswith("26."),
                    f"{target_id}: 26.x branch must contain a 26.x target")
            require(target.get("families") == {
                "balm": "load-context-26",
                "screen": "graphics-extractor-26",
                "teleport": "shogi-26",
            }, f"{target_id}: invalid 26.x adapter family")
            for stack_name in ("minimum", "current"):
                stack = target.get(stack_name)
                require(isinstance(stack, dict) and all(stack.get(key) for key in ("waystones", "balm", "shogi")),
                        f"{target_id}: {stack_name} must declare Waystones, Balm and Shogi")

            source = target.get("waystonesSource")
            if minecraft == ["26.1.1"]:
                require(source == {
                    "repository": "https://github.com/TwelveIterations/Waystones.git",
                    "commit": "795bb9ac93e73a0df8e5678ba6746dfbf8b055a3",
                    "version": "26.1.1.0",
                    "patch": "scripts/upstream/waystones-26.1.1.patch",
                    "patchSha256": "77707c33069f6f1def1b4262b6961b1851ab97915019138039f9c2ce587a42bd",
                }, f"{target_id}: invalid fixed Waystones source")
                patch_path = ROOT / source["patch"]
                require(patch_path.is_file(), f"{target_id}: fixed Waystones patch is missing")
                require(hashlib.sha256(patch_path.read_bytes()).hexdigest() == source["patchSha256"],
                        f"{target_id}: fixed Waystones patch SHA-256 mismatch")
            else:
                require(source is None, f"{target_id}: only 26.1.1 may use a source-built Waystones")

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
    expected_26 = ["26.1", "26.1.1", "26.1.2", "26.2"]
    for branch in ("neoforge/26.x", "fabric/26.x"):
        actual = [version for target in targets if target.get("branch") == branch
                  for version in target.get("minecraft", [])]
        require(actual == expected_26,
                f"{branch} matrix must cover 26.1, 26.1.1, 26.1.2 and 26.2 exactly")


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
        runtime_branches = (
            "neoforge/1.21.x",
            "fabric/1.21.x",
            "neoforge/26.x",
            "fabric/26.x",
        )
        expected_blocks = [
            [target["id"] for target in matrix["targets"] if target["branch"] == runtime_branch]
            for runtime_branch in runtime_branches
        ]
        if len(runtime_blocks) != len(expected_blocks):
            raise ValueError("main runtime workflow must contain all four unified target lists")
        for runtime_branch, actual, expected_block in zip(runtime_branches, runtime_blocks, expected_blocks):
            require_equal(f"main runtime {runtime_branch} targets", actual, expected_block)
    else:
        loader = BRANCH_LOADERS.get(branch)
        if loader is None:
            raise ValueError(f"unsupported branch {branch!r}")
        variable = "neoForgeTargets" if loader == "neoforge" else "fabricTargets"
        settings = (ROOT / "settings.gradle").read_text(encoding="utf-8")
        require_equal("settings.gradle targets", settings_targets(settings, variable), expected)
        branch_targets = [target for target in matrix["targets"] if target["branch"] == branch]
        validate_target_properties(ROOT, branch, branch_targets)
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
