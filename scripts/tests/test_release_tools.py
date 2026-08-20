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
runtime_smoke = load_script("runtime-smoke.py")
verify_release_manifest = load_script("verify-release-manifest.py")


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
        target = {"artifactFile": "waystonesplayer-test-1.0.0.jar"}
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


class BinaryEvidenceTest(unittest.TestCase):
    TARGET = {
        "id": "neoforge-1.21.1",
        "artifactFile": "waystonesplayer-neoforge-1.21.1-1.0.0.jar",
    }
    MATRIX = {"modVersion": "1.0.0"}
    COMMIT = "a" * 40

    def create_binary(self, directory: Path, commit: str | None = None) -> Path:
        path = directory / self.TARGET["artifactFile"]
        manifest = (
            "Manifest-Version: 1.0\r\n"
            "Implementation-Version: 1.0.0\r\n"
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
                    "Implementation-Version: 1.0.0\r\n"
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
                    "modVersion": "1.0.0",
                    "branch": "main",
                    "commit": self.COMMIT,
                    "artifacts": [{
                        "artifactFile": binary.name,
                        "branch": "main",
                        "buildStack": "minimum",
                        "commit": self.COMMIT,
                        "releaseVersion": "1.0.0",
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
