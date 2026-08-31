from __future__ import annotations

import hashlib
import importlib.util
import json
from pathlib import Path
import tempfile
import unittest
import zipfile


ROOT = Path(__file__).resolve().parents[2]


def load_script(name: str):
    path = ROOT / "scripts" / name
    spec = importlib.util.spec_from_file_location(name.replace("-", "_"), path)
    if spec is None or spec.loader is None:
        raise RuntimeError(f"could not load {path}")
    module = importlib.util.module_from_spec(spec)
    spec.loader.exec_module(module)
    return module


release_manifest = load_script("release-manifest.py")
runtime_matrix = load_script("runtime-matrix.py")
runtime_smoke = load_script("runtime-smoke.py")
verify_release_manifest = load_script("verify-release-manifest.py")
prepare_waystones_source = load_script("prepare-waystones-source.py")


class ReleaseManifestPathTest(unittest.TestCase):
    def test_output_must_stay_under_build(self):
        expected = (ROOT / "build" / "evidence" / "manifest.json").resolve()
        self.assertEqual(
            expected,
            release_manifest.resolve_output_path("build/evidence/manifest.json"),
        )
        with self.assertRaises(ValueError):
            release_manifest.resolve_output_path("../release-manifest.json")
        with self.assertRaises(ValueError):
            release_manifest.resolve_output_path("docs/release-manifest.json")

    def test_artifact_root_requires_one_exact_filename(self):
        target = {"artifactFile": "waystonesplayer-test-1.0.1.jar"}
        with tempfile.TemporaryDirectory() as temporary:
            root = Path(temporary)
            with self.assertRaises(ValueError):
                release_manifest.artifact_path(target, root)

            first = root / target["artifactFile"]
            first.write_bytes(b"first")
            self.assertEqual(first, release_manifest.artifact_path(target, root))

            nested = root / "duplicate"
            nested.mkdir()
            (nested / target["artifactFile"]).write_bytes(b"second")
            with self.assertRaises(ValueError):
                release_manifest.artifact_path(target, root)


class FixedWaystonesSourceTest(unittest.TestCase):
    def test_generated_paths_stay_under_build(self):
        self.assertEqual(
            (ROOT / "build" / "upstream-maven").resolve(),
            prepare_waystones_source.build_path("build/upstream-maven"),
        )
        with self.assertRaises(ValueError):
            prepare_waystones_source.build_path("../upstream-maven")

    def test_patched_catalog_has_immutable_build_inputs(self):
        matrix = json.loads((ROOT / "gradle" / "targets.json").read_text(encoding="utf-8"))
        expected = prepare_waystones_source.expected_catalog(matrix)
        self.assertNotIn("SNAPSHOT", " ".join(expected.values()))
        self.assertEqual("26.1.0.1-20260324.181500-45", expected["shogiApi"])

    def test_fixed_source_patch_matches_matrix_sha(self):
        matrix = json.loads((ROOT / "gradle" / "targets.json").read_text(encoding="utf-8"))
        target = prepare_waystones_source.target_for(matrix, "neoforge")
        source = prepare_waystones_source.verify_source_identity(target)
        self.assertEqual("795bb9ac93e73a0df8e5678ba6746dfbf8b055a3", source["commit"])

    def test_source_built_release_jar_requires_upstream_manifest_provenance(self):
        matrix = json.loads((ROOT / "gradle" / "targets.json").read_text(encoding="utf-8"))
        target = prepare_waystones_source.target_for(matrix, "neoforge")
        commit = "a" * 40
        icon = b"approved-icon"
        upstream_sha = "b" * 64
        with tempfile.TemporaryDirectory() as temporary:
            path = Path(temporary) / target["artifactFile"]
            manifest = (
                "Manifest-Version: 1.0\r\n"
                "Implementation-Version: 1.0.1\r\n"
                f"WaystonesPlayer-Target: {target['id']}\r\n"
                "WaystonesPlayer-Build-Stack: minimum\r\n"
                f"WaystonesPlayer-Source-Commit: {commit}\r\n"
                f"WaystonesPlayer-Upstream-Waystones-Commit: {target['waystonesSource']['commit']}\r\n"
                "WaystonesPlayer-Upstream-Waystones-Patch-SHA256: "
                f"{target['waystonesSource']['patchSha256']}\r\n"
                f"WaystonesPlayer-Upstream-Waystones-JAR-SHA256: {upstream_sha}\r\n\r\n"
            )
            with zipfile.ZipFile(path, "w") as archive:
                archive.writestr("META-INF/MANIFEST.MF", manifest)
                archive.writestr("waystonesplayer.png", icon)
                archive.writestr("waystonesplayer.mixins.json", "{}")
                archive.writestr("waystonesplayer.network.json", "{}")
                archive.writestr("assets/waystonesplayer/lang/en_us.json", "{}")
                archive.writestr("assets/waystonesplayer/lang/zh_cn.json", "{}")
            attributes = release_manifest.inspect_artifact(
                path, target, commit, "1.0.1", hashlib.sha256(icon).hexdigest()
            )
            self.assertEqual(
                upstream_sha,
                attributes["WaystonesPlayer-Upstream-Waystones-JAR-SHA256"],
            )


class RuntimeMatrixBranchTest(unittest.TestCase):
    def test_26_x_targets_use_supported_unified_branches(self):
        self.assertEqual(
            "neoforge/26.x",
            runtime_matrix.load_target("neoforge-26.1")["branch"],
        )
        self.assertEqual(
            "fabric/26.x",
            runtime_matrix.load_target("fabric-26.2")["branch"],
        )


class BinaryEvidenceTest(unittest.TestCase):
    TARGET = {
        "id": "neoforge-1.21.1",
        "artifactFile": "waystonesplayer-neoforge-1.21.1-1.0.1.jar",
    }
    MATRIX = {"modVersion": "1.0.1"}
    COMMIT = "a" * 40

    def create_binary(self, directory: Path, commit: str | None = None) -> Path:
        path = directory / self.TARGET["artifactFile"]
        manifest = (
            "Manifest-Version: 1.0\r\n"
            "Implementation-Version: 1.0.1\r\n"
            "WaystonesPlayer-Target: neoforge-1.21.1\r\n"
            "WaystonesPlayer-Build-Stack: minimum\r\n"
            f"WaystonesPlayer-Source-Commit: {commit or self.COMMIT}\r\n\r\n"
        )
        with zipfile.ZipFile(path, "w") as archive:
            archive.writestr("META-INF/MANIFEST.MF", manifest)
        return path

    def test_binary_sha_and_commit_are_bound_together(self):
        with tempfile.TemporaryDirectory() as temporary:
            path = self.create_binary(Path(temporary))
            digest = hashlib.sha256(path.read_bytes()).hexdigest()
            actual_sha, actual_commit = runtime_smoke.verify_binary(
                path,
                self.MATRIX,
                self.TARGET,
                digest,
                self.COMMIT,
            )
            self.assertEqual(digest, actual_sha)
            self.assertEqual(self.COMMIT, actual_commit)

            with self.assertRaises(ValueError):
                runtime_smoke.verify_binary(
                    path,
                    self.MATRIX,
                    self.TARGET,
                    "0" * 64,
                    self.COMMIT,
                )
            with self.assertRaises(ValueError):
                runtime_smoke.verify_binary(
                    path,
                    self.MATRIX,
                    self.TARGET,
                    digest,
                    "b" * 40,
                )

    def test_binary_requires_source_commit_even_without_explicit_expectation(self):
        with tempfile.TemporaryDirectory() as temporary:
            path = Path(temporary) / self.TARGET["artifactFile"]
            with zipfile.ZipFile(path, "w") as archive:
                archive.writestr(
                    "META-INF/MANIFEST.MF",
                    "Manifest-Version: 1.0\r\n"
                    "Implementation-Version: 1.0.1\r\n"
                    "WaystonesPlayer-Target: neoforge-1.21.1\r\n"
                    "WaystonesPlayer-Build-Stack: minimum\r\n\r\n",
                )
            with self.assertRaises(ValueError):
                runtime_smoke.verify_binary(path, self.MATRIX, self.TARGET, None, None)

    def test_downloaded_binary_matches_release_manifest_entry(self):
        with tempfile.TemporaryDirectory() as temporary:
            directory = Path(temporary)
            binary = self.create_binary(directory)
            digest = hashlib.sha256(binary.read_bytes()).hexdigest()
            manifest_path = directory / "release-manifest.json"
            manifest_path.write_text(
                json.dumps({
                    "schemaVersion": 2,
                    "modVersion": "1.0.1",
                    "branch": "main",
                    "commit": self.COMMIT,
                    "artifacts": [{
                        "artifactFile": binary.name,
                        "branch": "main",
                        "buildStack": "minimum",
                        "commit": self.COMMIT,
                        "releaseVersion": "1.0.1",
                        "sha256": digest,
                        "target": self.TARGET["id"],
                    }],
                }),
                encoding="utf-8",
            )

            entry = verify_release_manifest.verify(
                manifest_path,
                binary,
                self.TARGET["id"],
                "main",
                self.COMMIT,
                digest,
            )
            self.assertEqual(digest, entry["sha256"])

            with self.assertRaises(ValueError):
                verify_release_manifest.verify(
                    manifest_path,
                    binary,
                    self.TARGET["id"],
                    "main",
                    "b" * 40,
                    digest,
                )


if __name__ == "__main__":
    unittest.main()
