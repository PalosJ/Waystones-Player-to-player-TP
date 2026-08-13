#!/usr/bin/env python3
"""Build one minimum release JAR and exercise its audited runtime profiles."""

from __future__ import annotations

import argparse
import json
from pathlib import Path
import subprocess
import sys
from typing import Dict, List, Tuple


ROOT = Path(__file__).resolve().parents[1]
MATRIX_PATH = ROOT / "gradle" / "targets.json"


def parse_args() -> argparse.Namespace:
    parser = argparse.ArgumentParser(
        description="Run the minimum/key/current binary smoke matrix for one target."
    )
    parser.add_argument("--target", required=True)
    parser.add_argument(
        "--profiles",
        nargs="+",
        default=("minimum", "key", "current"),
        choices=("minimum", "key", "current"),
    )
    parser.add_argument(
        "--sides",
        nargs="+",
        default=("server", "client"),
        choices=("server", "client"),
    )
    parser.add_argument("--timeout", type=int, default=300)
    parser.add_argument("--skip-build", action="store_true")
    parser.add_argument("--xvfb", action="store_true")
    parser.add_argument("--verbose", action="store_true")
    parser.add_argument("--fail-fast", action="store_true")
    return parser.parse_args()


def load_target(target_id: str) -> Dict[str, object]:
    matrix = json.loads(MATRIX_PATH.read_text(encoding="utf-8"))
    target = next((entry for entry in matrix["targets"] if entry["id"] == target_id), None)
    if target is None:
        raise ValueError(f"Unknown target ID: {target_id}")
    if target["branch"] not in ("main", f"{target['loader']}/1.21.x"):
        raise ValueError(f"Unexpected branch mapping for {target_id}: {target['branch']}")
    return target


def build_minimum_jar(target: Dict[str, object]) -> int:
    if target["branch"] == "main":
        stack = target["minimum"]
        command = [
            str(ROOT / "gradlew"),
            "clean",
            "test",
            "build",
            "-PdependencyStack=minimum",
            f"-Pneo_version={stack['neoforge']}",
            f"-Pwaystones_version={stack['waystones']}",
            f"-Pbalm_version={stack['balm']}",
            "--no-build-cache",
            "--no-daemon",
            "--console=plain",
            "--warning-mode=all",
        ]
    else:
        command = [
            str(ROOT / "gradlew"),
            ":core:test",
            f":targets:{target['id']}:clean",
            f":targets:{target['id']}:build",
            "-PdependencyStack=minimum",
            "--no-build-cache",
            "--no-daemon",
            "--console=plain",
            "--warning-mode=all",
        ]
    print("BUILD " + " ".join(command), flush=True)
    return subprocess.run(command, cwd=ROOT).returncode


def runtime_cases(target: Dict[str, object], profiles: List[str],
                  sides: List[str]) -> List[Tuple[str, str, str]]:
    cases = []
    for minecraft in target["minecraft"]:
        runtime_entry = next(
            (
                entry
                for entry in target.get("runtimeStacks", [])
                if entry["minecraft"] == minecraft
            ),
            None,
        )
        for profile in profiles:
            if profile not in target and (runtime_entry is None or profile not in runtime_entry):
                continue
            for side in sides:
                cases.append((minecraft, profile, side))
    return cases


def run_case(target: Dict[str, object], minecraft: str, profile: str, side: str,
             timeout: int, xvfb: bool, verbose: bool) -> int:
    command = [
        sys.executable,
        str(ROOT / "scripts" / "runtime-smoke.py"),
        "--target",
        target["id"],
        "--minecraft",
        minecraft,
        "--stack",
        profile,
        "--side",
        side,
        "--timeout",
        str(timeout),
    ]
    if xvfb and side == "client":
        command.append("--xvfb")
    if verbose:
        command.append("--verbose")
    return subprocess.run(command, cwd=ROOT).returncode


def main() -> int:
    args = parse_args()
    try:
        target = load_target(args.target)
        if not args.skip_build:
            build_result = build_minimum_jar(target)
            if build_result != 0:
                return build_result

        cases = runtime_cases(target, list(args.profiles), list(args.sides))
        if not cases:
            raise ValueError("No runtime cases matched the requested profiles and sides")

        results = []
        for minecraft, profile, side in cases:
            result = run_case(
                target, minecraft, profile, side, args.timeout, args.xvfb, args.verbose
            )
            results.append((minecraft, profile, side, result))
            if result != 0 and args.fail_fast:
                break

        print("\nRuntime matrix summary:")
        for minecraft, profile, side, result in results:
            status = "PASS" if result == 0 else f"FAIL ({result})"
            print(f"  {status:10} Minecraft {minecraft:7} {profile:7} {side}")
        return 0 if all(result == 0 for _, _, _, result in results) else 1
    except (OSError, ValueError, KeyError, json.JSONDecodeError) as error:
        print(f"runtime-matrix: {error}", file=sys.stderr)
        return 2


if __name__ == "__main__":
    raise SystemExit(main())
