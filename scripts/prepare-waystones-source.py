#!/usr/bin/env python3
"""Build the missing Waystones 26.1.1 artifact from one immutable upstream commit."""

from __future__ import annotations

import argparse
import hashlib
import json
import os
from pathlib import Path
import re
import shutil
import subprocess
from typing import Dict, List


ROOT = Path(__file__).resolve().parents[1]
MATRIX_PATH = ROOT / "gradle" / "targets.json"
SUPPORTED_LOADERS = ("neoforge", "fabric")


def sha256(path: Path) -> str:
    digest = hashlib.sha256()
    with path.open("rb") as binary:
        for chunk in iter(lambda: binary.read(1024 * 1024), b""):
            digest.update(chunk)
    return digest.hexdigest()


def build_path(requested: str) -> Path:
    path = (ROOT / requested).resolve()
    build_root = (ROOT / "build").resolve()
    try:
        path.relative_to(build_root)
    except ValueError as error:
        raise ValueError("generated upstream paths must stay under the ignored build/ directory") from error
    return path


def run(command: List[str], cwd: Path) -> str:
    completed = subprocess.run(
        command,
        cwd=cwd,
        text=True,
        stdout=subprocess.PIPE,
        stderr=subprocess.STDOUT,
        check=True,
    )
    if completed.stdout:
        print(completed.stdout, end="")
    return completed.stdout


def java_executable() -> str:
    java_home = os.environ.get("JAVA_HOME", "").strip()
    return str(Path(java_home) / "bin" / "java") if java_home else "java"


def verify_java_25() -> None:
    completed = subprocess.run(
        [java_executable(), "-version"],
        text=True,
        stdout=subprocess.PIPE,
        stderr=subprocess.STDOUT,
        check=True,
    )
    first_line = completed.stdout.splitlines()[0] if completed.stdout else ""
    if re.search(r'\bversion "25(?:\.|\")', first_line) is None:
        raise ValueError(f"Waystones 26.1.1 source build requires JDK 25, got {first_line!r}")


def catalog_versions(path: Path) -> Dict[str, str]:
    versions: Dict[str, str] = {}
    in_versions = False
    for raw_line in path.read_text(encoding="utf-8").splitlines():
        line = raw_line.strip()
        if line.startswith("["):
            in_versions = line == "[versions]"
            continue
        if not in_versions or not line or line.startswith("#"):
            continue
        match = re.fullmatch(r'([A-Za-z][A-Za-z0-9]*)\s*=\s*"([^"]+)"', line)
        if match:
            versions[match.group(1)] = match.group(2)
    return versions


def expected_catalog(matrix: Dict[str, object]) -> Dict[str, str]:
    tools = matrix["buildTools"]["26"]
    return {
        "balm": "26.1.1.1",
        "journeyMapApi": "2.0.0-26.1",
        "unbreakables": "26.1.0.1",
        "shogi": "26.1.0.1",
        "shogiApi": tools["sourceShogiApi"],
    }


def target_for(matrix: Dict[str, object], loader: str) -> Dict[str, object]:
    target_id = f"{loader}-26.1.1"
    matches = [target for target in matrix["targets"] if target["id"] == target_id]
    if len(matches) != 1:
        raise ValueError(f"matrix must contain exactly one {target_id}")
    return matches[0]


def verify_source_identity(target: Dict[str, object]) -> Dict[str, object]:
    source = target.get("waystonesSource")
    if not isinstance(source, dict):
        raise ValueError(f"{target['id']} has no fixed Waystones source declaration")
    patch = ROOT / str(source["patch"])
    if not patch.is_file():
        raise ValueError(f"fixed Waystones patch is missing: {patch}")
    actual_patch_sha = sha256(patch)
    if actual_patch_sha != source["patchSha256"]:
        raise ValueError(
            f"fixed Waystones patch SHA-256 mismatch: {actual_patch_sha} != {source['patchSha256']}"
        )
    return source


def write_maven_artifact(
        built_jar: Path,
        output_root: Path,
        loader: str,
        version: str) -> Path:
    artifact = f"waystones-{loader}"
    coordinate_root = output_root / "net" / "blay09" / "mods" / artifact / version
    coordinate_root.mkdir(parents=True, exist_ok=True)
    destination = coordinate_root / f"{artifact}-{version}.jar"
    shutil.copyfile(built_jar, destination)
    pom = coordinate_root / f"{artifact}-{version}.pom"
    pom.write_text(
        "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"
        "<project xmlns=\"http://maven.apache.org/POM/4.0.0\" "
        "xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" "
        "xsi:schemaLocation=\"http://maven.apache.org/POM/4.0.0 "
        "https://maven.apache.org/xsd/maven-4.0.0.xsd\">\n"
        "  <modelVersion>4.0.0</modelVersion>\n"
        "  <groupId>net.blay09.mods</groupId>\n"
        f"  <artifactId>{artifact}</artifactId>\n"
        f"  <version>{version}</version>\n"
        "</project>\n",
        encoding="utf-8",
    )
    return destination


def parse_args() -> argparse.Namespace:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--loader", choices=SUPPORTED_LOADERS, required=True)
    parser.add_argument("--work-root", default="build/upstream-work")
    parser.add_argument("--output-root", default="build/upstream-maven")
    parser.add_argument("--provenance-root", default="build/upstream-provenance")
    parser.add_argument("--expected-sha256", help="Reject a rebuilt upstream JAR with a different SHA-256.")
    return parser.parse_args()


def main() -> int:
    args = parse_args()
    verify_java_25()
    matrix = json.loads(MATRIX_PATH.read_text(encoding="utf-8"))
    target = target_for(matrix, args.loader)
    source = verify_source_identity(target)
    work_root = build_path(args.work_root)
    output_root = build_path(args.output_root)
    provenance_root = build_path(args.provenance_root)
    checkout = work_root / f"waystones-26.1.1-{args.loader}"
    if checkout.exists():
        shutil.rmtree(checkout)
    checkout.mkdir(parents=True)

    run(["git", "init", "--quiet"], checkout)
    run(["git", "remote", "add", "origin", str(source["repository"])], checkout)
    run(["git", "fetch", "--depth=1", "origin", str(source["commit"])], checkout)
    run(["git", "checkout", "--quiet", "--detach", "FETCH_HEAD"], checkout)
    actual_commit = run(["git", "rev-parse", "HEAD"], checkout).strip()
    if actual_commit != source["commit"]:
        raise ValueError(f"fetched Waystones commit mismatch: {actual_commit} != {source['commit']}")
    run(["git", "apply", str(ROOT / str(source["patch"]))], checkout)

    actual_catalog = catalog_versions(checkout / "gradle" / "libs.versions.toml")
    expected_versions = expected_catalog(matrix)
    for key, expected in expected_versions.items():
        if actual_catalog.get(key) != expected:
            raise ValueError(f"fixed Waystones catalog {key} mismatch: {actual_catalog.get(key)!r} != {expected!r}")

    other_loader = "fabric" if args.loader == "neoforge" else "neoforge"
    run([
        str(checkout / "gradlew"),
        f":{args.loader}:build",
        f"-Pinclude_{args.loader}=true",
        f"-Pinclude_{other_loader}=false",
        "-Pinclude_forge=false",
        "--no-daemon",
        "--console=plain",
        "--warning-mode=all",
    ], checkout)

    built_name = f"waystones-{args.loader}-26.1.1-{source['version']}.jar"
    built_jar = checkout / args.loader / "build" / "libs" / built_name
    if not built_jar.is_file():
        raise FileNotFoundError(f"fixed Waystones source build did not produce {built_jar}")
    upstream_sha = sha256(built_jar)
    if args.expected_sha256 and upstream_sha != args.expected_sha256:
        raise ValueError(
            f"rebuilt Waystones SHA-256 mismatch: {upstream_sha} != {args.expected_sha256}"
        )

    installed = write_maven_artifact(built_jar, output_root, args.loader, str(source["version"]))
    provenance_root.mkdir(parents=True, exist_ok=True)
    provenance_path = provenance_root / f"waystones-{args.loader}-26.1.1.json"
    provenance = {
        "schemaVersion": 1,
        "loader": args.loader,
        "coordinate": f"net.blay09.mods:waystones-{args.loader}:{source['version']}",
        "sourceRepository": source["repository"],
        "sourceCommit": source["commit"],
        "sourcePatch": source["patch"],
        "sourcePatchSha256": source["patchSha256"],
        "sourceDependencies": expected_versions,
        "artifactFile": installed.name,
        "artifactSha256": upstream_sha,
    }
    provenance_path.write_text(
        json.dumps(provenance, ensure_ascii=False, indent=2) + "\n", encoding="utf-8"
    )
    print(f"UPSTREAM-JAR={installed.relative_to(ROOT)}")
    print(f"UPSTREAM-SHA256={upstream_sha}")
    print(f"UPSTREAM-PROVENANCE={provenance_path.relative_to(ROOT)}")
    return 0


if __name__ == "__main__":
    try:
        raise SystemExit(main())
    except (OSError, ValueError, KeyError, json.JSONDecodeError, subprocess.CalledProcessError) as error:
        raise SystemExit(f"prepare-waystones-source: {error}")
