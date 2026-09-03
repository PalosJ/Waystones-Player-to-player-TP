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
import re
import subprocess
from typing import Dict, List, Set
import zipfile


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
    parser.add_argument(
        "--artifact-root",
        help="Find every branch artifact by exact filename below this build/ directory.",
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


def artifact_path(target: Dict[str, object], artifact_root: Path | None = None) -> Path:
    if artifact_root is not None:
        matches = sorted(
            path for path in artifact_root.rglob(str(target["artifactFile"])) if path.is_file()
        )
        if len(matches) != 1:
            raise ValueError(
                f"expected exactly one {target['artifactFile']} below {artifact_root}, "
                f"found {len(matches)}"
            )
        return matches[0]
    if target["branch"] == "main":
        return ROOT / "neoforge" / "build" / "libs" / target["artifactFile"]
    return ROOT / "targets" / target["id"] / "build" / "libs" / target["artifactFile"]


def manifest_attributes(archive: zipfile.ZipFile) -> Dict[str, str]:
    try:
        raw = archive.read("META-INF/MANIFEST.MF").decode("utf-8")
    except KeyError as error:
        raise ValueError("artifact has no META-INF/MANIFEST.MF") from error
    unfolded: List[str] = []
    for line in raw.replace("\r\n", "\n").split("\n"):
        if line.startswith(" ") and unfolded:
            unfolded[-1] += line[1:]
        else:
            unfolded.append(line)
    return dict(line.split(": ", 1) for line in unfolded if ": " in line)


def inspect_artifact(
        path: Path,
        target: Dict[str, object],
        commit: str,
        mod_version: str,
        icon_sha256: str,
        mod_id: str = "waystonesptpt") -> Dict[str, str]:
    required_entries: Set[str] = {
        "META-INF/MANIFEST.MF",
        f"{mod_id}.png",
        f"{mod_id}.mixins.json",
        f"{mod_id}.network.json",
        f"assets/{mod_id}/lang/en_us.json",
        f"assets/{mod_id}/lang/zh_cn.json",
    }
    forbidden_prefixes = (
        "com/mojang/",
        "net/blay09/",
        "net/fabricmc/",
        "net/minecraft/",
        "net/neoforged/",
        "org/spongepowered/",
    )
    with zipfile.ZipFile(path) as archive:
        names = archive.namelist()
        if len(names) != len(set(names)):
            raise ValueError(f"{path.name}: artifact contains duplicate ZIP entries")
        missing = sorted(required_entries.difference(names))
        if missing:
            raise ValueError(f"{path.name}: missing required entries: {', '.join(missing)}")
        if any(name.lower().endswith(".jar") for name in names):
            raise ValueError(f"{path.name}: artifact contains a nested JAR")
        if any(name.startswith(forbidden_prefixes) for name in names):
            raise ValueError(f"{path.name}: artifact bundles game, loader, or dependency classes")
        if any(name == "waystonesplayer.png" or name.startswith("waystonesplayer.")
               or name.startswith("assets/waystonesplayer/")
               or name.startswith("com/palosj/waystonesplayer/") for name in names):
            raise ValueError(f"{path.name}: artifact contains legacy identity entries")

        attributes = manifest_attributes(archive)
        expected = {
            "Implementation-Version": mod_version,
            "WaystonesPTPT-Target": target["id"],
            "WaystonesPTPT-Build-Stack": "minimum",
            "WaystonesPTPT-Source-Commit": commit,
        }
        for key, expected_value in expected.items():
            if attributes.get(key) != expected_value:
                raise ValueError(
                    f"{path.name}: manifest {key} mismatch: "
                    f"{attributes.get(key)!r} != {expected_value!r}"
                )
        icon_bytes = archive.read(f"{mod_id}.png")
        if hashlib.sha256(icon_bytes).hexdigest() != icon_sha256:
            raise ValueError(f"{path.name}: approved icon SHA-256 mismatch")
        source = target.get("waystonesSource")
        source_keys = {
            "WaystonesPTPT-Upstream-Waystones-Commit",
            "WaystonesPTPT-Upstream-Waystones-Patch-SHA256",
            "WaystonesPTPT-Upstream-Waystones-JAR-SHA256",
        }
        if source:
            source_expected = {
                "WaystonesPTPT-Upstream-Waystones-Commit": source["commit"],
                "WaystonesPTPT-Upstream-Waystones-Patch-SHA256": source["patchSha256"],
            }
            for key, expected_value in source_expected.items():
                if attributes.get(key) != expected_value:
                    raise ValueError(
                        f"{path.name}: manifest {key} mismatch: "
                        f"{attributes.get(key)!r} != {expected_value!r}"
                    )
            upstream_sha = attributes.get("WaystonesPTPT-Upstream-Waystones-JAR-SHA256", "")
            if re.fullmatch(r"[0-9a-f]{64}", upstream_sha) is None:
                raise ValueError(f"{path.name}: manifest has no valid upstream Waystones JAR SHA-256")
        elif any(key in attributes for key in source_keys):
            raise ValueError(f"{path.name}: unexpected fixed-source Waystones provenance")
        return attributes


def target_entry(
        target: Dict[str, object],
        branch: str,
        commit: str,
        mod_id: str,
        mod_version: str,
        icon_sha256: str,
        artifact_root: Path | None = None) -> Dict[str, object]:
    path = artifact_path(target, artifact_root)
    if not path.is_file():
        raise FileNotFoundError(f"missing minimum-built artifact: {path}")
    attributes = inspect_artifact(path, target, commit, mod_version, icon_sha256, mod_id)
    entry = {
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
    if target.get("waystonesSource"):
        entry["waystonesSource"] = {
            **target["waystonesSource"],
            "artifactSha256": attributes["WaystonesPTPT-Upstream-Waystones-JAR-SHA256"],
        }
    return entry


def resolve_output_path(requested: str) -> Path:
    output = (ROOT / requested).resolve()
    build_root = (ROOT / "build").resolve()
    try:
        output.relative_to(build_root)
    except ValueError as error:
        raise ValueError("release manifest output must stay under the ignored build/ directory") from error
    return output


def resolve_artifact_root(requested: str) -> Path:
    artifact_root = (ROOT / requested).resolve()
    build_root = (ROOT / "build").resolve()
    try:
        artifact_root.relative_to(build_root)
    except ValueError as error:
        raise ValueError("artifact root must stay under the ignored build/ directory") from error
    if not artifact_root.is_dir():
        raise ValueError(f"artifact root is not a directory: {artifact_root}")
    return artifact_root


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
    artifact_root = resolve_artifact_root(args.artifact_root) if args.artifact_root else None
    manifest = {
        "schemaVersion": 2,
        "modId": matrix["modId"],
        "modVersion": matrix["modVersion"],
        "branch": branch,
        "commit": commit,
        "iconSha256": matrix["iconSha256"],
        "artifacts": [
            target_entry(
                target,
                branch,
                commit,
                matrix["modId"],
                matrix["modVersion"],
                matrix["iconSha256"],
                artifact_root,
            )
            for target in targets
        ],
    }
    output = resolve_output_path(args.output)
    output.parent.mkdir(parents=True, exist_ok=True)
    output.write_text(json.dumps(manifest, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")
    print(f"Wrote {output.relative_to(ROOT)} ({len(targets)} artifacts)")
    return 0


if __name__ == "__main__":
    try:
        raise SystemExit(main())
    except (OSError, ValueError, KeyError, json.JSONDecodeError, subprocess.CalledProcessError) as error:
        raise SystemExit(f"release-manifest: {error}")
