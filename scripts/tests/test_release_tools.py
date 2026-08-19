from __future__ import annotations

import hashlib
import importlib.util
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


if __name__ == "__main__":
    unittest.main()
