#!/usr/bin/env python3
"""Run an isolated binary-only Minecraft startup smoke test.

The runtime projects deliberately contain no mod sources. They load the unchanged
minimum-built release JAR and the dependency stack selected from targets.json.
"""

from __future__ import annotations

import argparse
import hashlib
import json
import os
from pathlib import Path
import queue
import re
import shutil
import signal
import subprocess
import sys
import threading
import time
from typing import Dict, List, Optional, TextIO, Tuple
import zipfile


ROOT = Path(__file__).resolve().parents[1]
MATRIX_PATH = ROOT / "gradle" / "targets.json"
SUCCESS_GRACE_SECONDS = 3.0
STOP_TIMEOUT_SECONDS = 20.0

FATAL_PATTERNS = tuple(
    re.compile(pattern, re.IGNORECASE)
    for pattern in (
        r"Mixin apply failed",
        r"MixinTransformerError",
        r"InvalidMixinException",
        r"NoClassDefFoundError",
        # NeoForge may probe optional Log4j context selectors and explicitly fall
        # back after ClassNotFoundException. Actual mod linkage failures are
        # covered by NoClassDefFoundError and the loader/entrypoint signatures.
        r"ExceptionInInitializerError",
        r"Could not execute entrypoint",
        r"Failed to create mod instance",
        r"ModLoadingException",
        r"LoadingFailedException",
        r"FAILED TO BIND TO PORT",
        r"java\.net\.BindException",
        r"java\.lang\.LinkageError",
        r"\[[^\n]*ERROR[^\n]*\][^\n]*(?:waystonesplayer|com\.palosj)",
    )
)


def parse_args() -> argparse.Namespace:
    parser = argparse.ArgumentParser(
        description="Start a minimum-built release JAR against a selected runtime stack."
    )
    parser.add_argument("--target", required=True, help="Target ID from gradle/targets.json")
    parser.add_argument(
        "--minecraft",
        help="Minecraft version; required only when a target supports more than one version",
    )
    parser.add_argument("--stack", default="minimum", help="Runtime stack name")
    parser.add_argument("--side", required=True, choices=("server", "client"))
    parser.add_argument("--timeout", type=int, default=240, help="Startup timeout in seconds")
    parser.add_argument(
        "--xvfb",
        action="store_true",
        help="Run the Gradle client task under xvfb-run (Linux CI only)",
    )
    parser.add_argument(
        "--verbose", action="store_true", help="Mirror the complete Gradle/game log to stdout"
    )
    parser.add_argument(
        "--binary",
        help="Explicit minimum-built JAR to run; relative paths are resolved from the repository root",
    )
    parser.add_argument("--expected-sha256", help="Required SHA-256 for an explicit binary")
    parser.add_argument("--expected-commit", help="Required source commit for an explicit binary")
    return parser.parse_args()


def load_target(target_id: str) -> Tuple[Dict[str, object], Dict[str, object]]:
    matrix = json.loads(MATRIX_PATH.read_text(encoding="utf-8"))
    target = next((entry for entry in matrix["targets"] if entry["id"] == target_id), None)
    if target is None:
        raise ValueError(f"Unknown target ID: {target_id}")
    return matrix, target


def select_minecraft(target: Dict[str, object], requested: Optional[str]) -> str:
    supported = target["minecraft"]
    if requested is None:
        if len(supported) != 1:
            raise ValueError(
                f"{target['id']} supports {', '.join(supported)}; pass --minecraft explicitly"
            )
        return supported[0]
    if requested not in supported:
        raise ValueError(f"{target['id']} does not support Minecraft {requested}")
    return requested


def select_stack(target: Dict[str, object], minecraft: str,
                 stack: str) -> Dict[str, str]:
    selected = dict(target.get(stack, {}))
    for runtime_entry in target.get("runtimeStacks", []):
        if runtime_entry["minecraft"] == minecraft:
            selected.update(runtime_entry.get(stack, {}))
            break
    if not selected:
        raise ValueError(
            f"{target['id']} has no {stack!r} runtime stack for Minecraft {minecraft}"
        )
    return selected


def output_reader(stream: TextIO, lines: "queue.Queue[Optional[str]]") -> None:
    try:
        for line in stream:
            lines.put(line)
    finally:
        lines.put(None)


def process_group_exists(process: subprocess.Popen[str]) -> bool:
    # Poll reaps a completed Gradle parent before checking whether a spawned game
    # process is still alive in the same session/process group.
    process.poll()
    try:
        os.killpg(process.pid, 0)
        return True
    except ProcessLookupError:
        return False
    except PermissionError:
        return True


def wait_for_process_group(process: subprocess.Popen[str], timeout: float) -> bool:
    deadline = time.monotonic() + timeout
    while time.monotonic() < deadline:
        if not process_group_exists(process):
            return True
        time.sleep(0.1)
    return not process_group_exists(process)


def stop_process_group(process: subprocess.Popen[str]) -> None:
    for stop_signal, timeout in (
        (signal.SIGINT, STOP_TIMEOUT_SECONDS),
        (signal.SIGTERM, 5.0),
        (signal.SIGKILL, 5.0),
    ):
        if not process_group_exists(process):
            return
        try:
            os.killpg(process.pid, stop_signal)
        except ProcessLookupError:
            return
        if wait_for_process_group(process, timeout):
            return
    raise RuntimeError(f"Could not stop runtime process group {process.pid}")


def process_exited_unexpectedly(process: subprocess.Popen[str], success_at: Optional[float]) -> bool:
    return process.poll() is not None and (
        success_at is None or process.returncode not in (0,)
    )


def fatal_matches(output: str) -> List[str]:
    matches = []
    for pattern in FATAL_PATTERNS:
        match = pattern.search(output)
        if match:
            matches.append(match.group(0))
    return matches


def display_version(version: str) -> str:
    return version.split("+", 1)[0]


def binary_path(target: Dict[str, object], requested: Optional[str] = None) -> Path:
    if requested:
        path = Path(requested)
        return (path if path.is_absolute() else ROOT / path).resolve()
    if target["branch"] == "main":
        return ROOT / "neoforge" / "build" / "libs" / target["artifactFile"]
    return ROOT / "targets" / target["id"] / "build" / "libs" / target["artifactFile"]


def binary_sha256(path: Path) -> str:
    digest = hashlib.sha256()
    with path.open("rb") as binary:
        for chunk in iter(lambda: binary.read(1024 * 1024), b""):
            digest.update(chunk)
    return digest.hexdigest()


def jar_manifest(path: Path) -> Dict[str, str]:
    with zipfile.ZipFile(path) as archive:
        try:
            raw = archive.read("META-INF/MANIFEST.MF").decode("utf-8")
        except KeyError as error:
            raise ValueError(f"binary has no META-INF/MANIFEST.MF: {path}") from error

    unfolded: List[str] = []
    for line in raw.replace("\r\n", "\n").split("\n"):
        if line.startswith(" ") and unfolded:
            unfolded[-1] += line[1:]
        else:
            unfolded.append(line)
    attributes: Dict[str, str] = {}
    for line in unfolded:
        if ": " in line:
            key, value = line.split(": ", 1)
            attributes[key] = value
    return attributes


def verify_binary(
        path: Path,
        matrix: Dict[str, object],
        target: Dict[str, object],
        expected_sha256: Optional[str],
        expected_commit: Optional[str]) -> Tuple[str, str]:
    if not path.is_file():
        raise ValueError(f"minimum release JAR is missing: {path}")
    if path.name != target["artifactFile"]:
        raise ValueError(f"binary filename does not match target {target['id']}: {path.name}")

    actual_sha256 = binary_sha256(path)
    if expected_sha256 is not None and actual_sha256 != expected_sha256.lower():
        raise ValueError(
            f"binary SHA-256 mismatch: expected {expected_sha256.lower()}, got {actual_sha256}"
        )
    manifest = jar_manifest(path)
    expected_attributes = {
        "Implementation-Version": matrix["modVersion"],
        "WaystonesPlayer-Target": target["id"],
        "WaystonesPlayer-Build-Stack": "minimum",
    }
    if expected_commit is not None:
        expected_attributes["WaystonesPlayer-Source-Commit"] = expected_commit
    for key, expected in expected_attributes.items():
        actual = manifest.get(key)
        if actual != expected:
            raise ValueError(f"binary manifest {key} mismatch: expected {expected!r}, got {actual!r}")
    source_commit = manifest.get("WaystonesPlayer-Source-Commit", "")
    if not source_commit:
        raise ValueError("binary manifest has no WaystonesPlayer-Source-Commit")
    return actual_sha256, source_commit


def fabric_mod_listed(output: str, mod_id: str, version: str) -> bool:
    return bool(
        re.search(
            rf"^\s*-\s+{re.escape(mod_id)}\s+{re.escape(version)}(?:\s|$)",
            output,
            re.MULTILINE,
        )
    )


def required_signals(side: str, output: str, mod_version: str,
                     target: Dict[str, object], runtime_stack: Dict[str, str],
                     minecraft: str) -> Dict[str, bool]:
    common = {"minimum JAR manifest verified": "verifyBinaryUnderTest" in output}
    if target["loader"] == "neoforge":
        common.update({
            "Waystones Player listed": f"Waystones Player {mod_version}" in output,
            "Minecraft version exact": f"Minecraft {minecraft} (minecraft)" in output,
            "Waystones version exact": (
                f"Waystones {display_version(runtime_stack['waystones'])} (waystones)" in output
            ),
            "Balm version exact": (
                f"Balm {display_version(runtime_stack['balm'])} (balm)" in output
            ),
            "NeoForge version exact": (
                f"NeoForge {runtime_stack['neoforge']} (neoforge)" in output
            ),
        })
    elif target["loader"] == "fabric":
        common.update({
            "Fabric Loader and Minecraft exact": (
                f"Loading Minecraft {minecraft} with Fabric Loader "
                f"{runtime_stack['fabricLoader']}" in output
            ),
            "Waystones Player listed": fabric_mod_listed(
                output, "waystonesplayer", mod_version
            ),
            "Fabric API version exact": fabric_mod_listed(
                output, "fabric-api", runtime_stack["fabricApi"]
            ),
            "Waystones version exact": fabric_mod_listed(
                output, "waystones", display_version(runtime_stack["waystones"])
            ),
            "Balm version exact": fabric_mod_listed(
                output,
                runtime_stack.get("balmRuntimeModId", target["balmModId"]),
                display_version(runtime_stack["balm"]),
            ),
        })
    else:
        raise ValueError(f"Unsupported loader: {target['loader']}")
    if side == "server":
        common["dedicated server reached Done"] = bool(
            re.search(r"Done \([^\r\n)]+\)!", output)
        )
    else:
        common["client resources include Waystones Player"] = any(
            "Reloading ResourceManager:" in line and "waystonesplayer" in line
            for line in output.splitlines()
        )
        common["client GUI texture atlas created"] = any(
            "Created:" in line and "minecraft:textures/atlas/gui.png-atlas" in line
            for line in output.splitlines()
        )
    return common


def build_command(target: Dict[str, object], minecraft: str, stack: str, side: str,
                  use_xvfb: bool, binary: Path, expected_sha256: str,
                  expected_commit: str) -> List[str]:
    runtime_project = f":runtime:{target['loader']}-smoke"
    command = [
        str(ROOT / "gradlew"),
        f"{runtime_project}:run{side.capitalize()}",
        f"-PruntimeTarget={target['id']}",
        f"-PruntimeMinecraft={minecraft}",
        f"-PruntimeStack={stack}",
        f"-PbinaryUnderTest={binary}",
        f"-PexpectedBinarySha256={expected_sha256}",
        f"-PexpectedSourceCommit={expected_commit}",
        "--no-daemon",
        "--console=plain",
    ]
    if use_xvfb:
        xvfb_run = shutil.which("xvfb-run")
        if xvfb_run is None:
            raise ValueError("--xvfb was requested but xvfb-run is not installed")
        command = [xvfb_run, "-a"] + command
    return command


def run_smoke(args: argparse.Namespace) -> int:
    matrix, target = load_target(args.target)
    minecraft = select_minecraft(target, args.minecraft)
    runtime_stack = select_stack(target, minecraft, args.stack)
    if args.xvfb and args.side != "client":
        raise ValueError("--xvfb is only valid for client smoke tests")
    explicit_evidence = (args.binary, args.expected_sha256, args.expected_commit)
    if any(explicit_evidence) and not all(explicit_evidence):
        raise ValueError(
            "--binary, --expected-sha256, and --expected-commit must be provided together"
        )

    result_dir = (
        ROOT
        / "build"
        / "runtime-smoke-results"
        / target["id"]
        / minecraft
        / args.stack
    )
    result_dir.mkdir(parents=True, exist_ok=True)
    result_file = result_dir / f"{args.side}.log"
    release_jar = binary_path(target, args.binary)
    release_sha256, release_commit = verify_binary(
        release_jar, matrix, target, args.expected_sha256, args.expected_commit
    )
    command = build_command(
        target,
        minecraft,
        args.stack,
        args.side,
        args.xvfb,
        release_jar,
        release_sha256,
        release_commit,
    )

    run_directory = (
        ROOT
        / "runtime"
        / f"{target['loader']}-smoke"
        / "build"
        / "runs"
        / target["id"]
        / minecraft
        / args.stack
        / args.side
    )
    if run_directory.exists():
        shutil.rmtree(run_directory)

    print(
        f"Starting {target['id']} Minecraft {minecraft} {args.stack} {args.side} smoke",
        flush=True,
    )
    print(f"Evidence log: {result_file.relative_to(ROOT)}", flush=True)

    process = subprocess.Popen(
        command,
        cwd=ROOT,
        stdout=subprocess.PIPE,
        stderr=subprocess.STDOUT,
        text=True,
        encoding="utf-8",
        errors="replace",
        bufsize=1,
        start_new_session=True,
    )
    assert process.stdout is not None
    queued_lines: "queue.Queue[Optional[str]]" = queue.Queue()
    reader = threading.Thread(
        target=output_reader, args=(process.stdout, queued_lines), daemon=True
    )
    reader.start()

    output_parts: List[str] = []
    deadline = time.monotonic() + args.timeout
    success_at: Optional[float] = None
    readiness_at: Optional[float] = None
    timed_out = False
    unexpected_exit = False

    try:
        with result_file.open("w", encoding="utf-8") as evidence:
            evidence.write("COMMAND: " + " ".join(command) + "\n\n")
            while True:
                now = time.monotonic()
                if now >= deadline:
                    timed_out = True
                    break
                if success_at is not None and now - success_at >= SUCCESS_GRACE_SECONDS:
                    break
                if (
                    readiness_at is not None
                    and success_at is None
                    and now - readiness_at >= SUCCESS_GRACE_SECONDS
                ):
                    break

                try:
                    line = queued_lines.get(timeout=min(0.5, deadline - now))
                except queue.Empty:
                    if process.poll() is not None:
                        if process_exited_unexpectedly(process, success_at):
                            unexpected_exit = True
                        break
                    continue
                if line is None:
                    if process_exited_unexpectedly(process, success_at):
                        unexpected_exit = True
                    break

                output_parts.append(line)
                evidence.write(line)
                evidence.flush()
                if args.verbose:
                    sys.stdout.write(line)
                    sys.stdout.flush()

                if fatal_matches(line):
                    break
                output = "".join(output_parts)
                signals = required_signals(
                    args.side,
                    output,
                    matrix["modVersion"],
                    target,
                    runtime_stack,
                    minecraft,
                )
                readiness_signal = (
                    "dedicated server reached Done"
                    if args.side == "server"
                    else "client GUI texture atlas created"
                )
                if readiness_at is None and signals.get(readiness_signal, False):
                    readiness_at = time.monotonic()
                if success_at is None and all(signals.values()):
                    success_at = time.monotonic()
    finally:
        stop_process_group(process)
        reader.join(timeout=2)

    output = "".join(output_parts)
    signals = required_signals(
        args.side,
        output,
        matrix["modVersion"],
        target,
        runtime_stack,
        minecraft,
    )
    fatal = fatal_matches(output)
    missing = [name for name, present in signals.items() if not present]
    crash_reports = list((run_directory / "crash-reports").glob("crash-*.txt"))
    if crash_reports:
        unexpected_exit = True

    with result_file.open("a", encoding="utf-8") as evidence:
        try:
            evidence_path = release_jar.relative_to(ROOT)
        except ValueError:
            evidence_path = release_jar
        evidence.write(f"BINARY: {evidence_path}\n")
        evidence.write(f"BINARY-SHA256: {release_sha256}\n")
        evidence.write(f"BINARY-SOURCE-COMMIT: {release_commit}\n")
        evidence.write(f"PROCESS-EXIT-CODE: {process.returncode}\n")
        evidence.write(f"UNEXPECTED-EXIT: {unexpected_exit}\n")
        if crash_reports:
            evidence.write("CRASH-REPORTS:\n")
            for report in crash_reports:
                evidence.write(f"- {report.relative_to(ROOT)}\n")

    if timed_out:
        print(f"Smoke test timed out after {args.timeout} seconds.", file=sys.stderr)
    if fatal:
        print("Fatal startup signatures found:", file=sys.stderr)
        for match in fatal:
            print(f"  - {match}", file=sys.stderr)
    if unexpected_exit:
        print("Runtime process exited before the required startup signals or left a crash report.",
              file=sys.stderr)
    if missing:
        print("Missing startup evidence:", file=sys.stderr)
        for name in missing:
            print(f"  - {name}", file=sys.stderr)

    if timed_out or fatal or missing or unexpected_exit:
        if not args.verbose:
            print("Last startup log lines:", file=sys.stderr)
            for line in output.splitlines()[-120:]:
                print(line, file=sys.stderr)
        return 1

    print(
        f"PASS {target['id']} Minecraft {minecraft} {args.stack} {args.side}: "
        + ", ".join(signals),
        flush=True,
    )
    return 0


def main() -> int:
    args = parse_args()
    try:
        return run_smoke(args)
    except (OSError, ValueError, KeyError, json.JSONDecodeError) as error:
        print(f"runtime-smoke: {error}", file=sys.stderr)
        return 2


if __name__ == "__main__":
    raise SystemExit(main())
