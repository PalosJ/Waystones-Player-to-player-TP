from __future__ import annotations

import importlib.util
import json
from pathlib import Path
import tempfile
import unittest


ROOT = Path(__file__).resolve().parents[2]
SCRIPT = ROOT / "scripts" / "verify-target-matrix.py"
SPEC = importlib.util.spec_from_file_location("verify_target_matrix", SCRIPT)
if SPEC is None or SPEC.loader is None:
    raise RuntimeError(f"could not load {SCRIPT}")
verify = importlib.util.module_from_spec(SPEC)
SPEC.loader.exec_module(verify)


class TargetPropertiesTest(unittest.TestCase):
    TARGET = {
        "id": "neoforge-1.21.4",
        "branch": "neoforge/1.21.x",
        "loader": "neoforge",
        "minecraft": ["1.21.4"],
        "families": {
            "balm": "runnable-21.3",
            "screen": "legacy-input",
            "teleport": "legacy-21.3-21.9",
        },
    }
    ROOTS = (
        "loader/neoforge-runnable-21.3",
        "screen/legacy-1.21.3-1.21.10",
        "teleport/legacy-21.3",
        "teleport/context-optional-hand-21.3-21.10",
    )

    def fixture(self, target_id: str = "neoforge-1.21.4", roots=None):
        temporary = tempfile.TemporaryDirectory()
        root = Path(temporary.name)
        selected_roots = roots if roots is not None else self.ROOTS
        for adapter_root in selected_roots:
            java_root = root / "adapters" / adapter_root / "java"
            java_root.mkdir(parents=True)
            (java_root / "Adapter.java").write_text("class Adapter {}\n", encoding="utf-8")
        common_context = (
            root / "common" / "src" / "main" / "java" / "com" / "palosj"
            / "waystonesptpt" / "compat" / "LockedWaystoneTeleportContext.java"
        )
        common_context.parent.mkdir(parents=True)
        common_context.write_text("class LockedWaystoneTeleportContext {}\n", encoding="utf-8")
        properties = root / "targets" / self.TARGET["id"] / "target.properties"
        properties.parent.mkdir(parents=True)
        properties.write_text(
            f"targetId={target_id}\n"
            "commonExcludes=com/palosj/waystonesptpt/compat/LockedWaystoneTeleportContext.java\n"
            "neoForgeExcludes=\n"
            f"adapterRoots={','.join(selected_roots)}\n",
            encoding="utf-8",
        )
        return temporary, root

    def test_accepts_matching_target_and_families(self):
        temporary, root = self.fixture()
        with temporary:
            verify.validate_target_properties(root, "neoforge/1.21.x", [self.TARGET])

    def test_rejects_wrong_target_id(self):
        temporary, root = self.fixture(target_id="neoforge-1.21.5")
        with temporary, self.assertRaises(ValueError):
            verify.validate_target_properties(root, "neoforge/1.21.x", [self.TARGET])

    def test_rejects_family_root_mismatch(self):
        wrong_roots = (
            "screen/legacy-1.21.3-1.21.10",
            "teleport/legacy-21.3",
        )
        temporary, root = self.fixture(roots=wrong_roots)
        with temporary, self.assertRaises(ValueError):
            verify.validate_target_properties(root, "neoforge/1.21.x", [self.TARGET])

    def test_rejects_missing_adapter_source(self):
        temporary, root = self.fixture()
        with temporary:
            adapter = root / "adapters" / self.ROOTS[0] / "java" / "Adapter.java"
            adapter.unlink()
            with self.assertRaises(ValueError):
                verify.validate_target_properties(root, "neoforge/1.21.x", [self.TARGET])

    def test_rejects_obsolete_context_source_root(self):
        wrong_roots = (
            "loader/neoforge-runnable-21.3",
            "screen/legacy-1.21.3-1.21.10",
            "teleport/legacy-21.3",
            "teleport/context-no-hand-21.3-21.9",
        )
        temporary, root = self.fixture(roots=wrong_roots)
        with temporary, self.assertRaises(ValueError):
                verify.validate_target_properties(root, "neoforge/1.21.x", [self.TARGET])


class MatrixShapeTest(unittest.TestCase):
    def test_repository_matrix_covers_all_28_targets(self):
        matrix = json.loads((ROOT / "gradle" / "targets.json").read_text(encoding="utf-8"))
        verify.validate_matrix_shape(matrix)
        self.assertEqual("waystonesptpt", matrix["modId"])
        self.assertEqual(28, len(matrix["targets"]))

        targets = {item["id"]: item for item in matrix["targets"]}
        self.assertEqual(
            {"neoforge": "21.1.249", "waystones": "21.1.42+1.21.1", "balm": "21.0.65+1.21.1"},
            targets["neoforge-1.21.1"]["current"],
        )
        self.assertEqual(
            {
                "fabricLoader": "0.19.5",
                "fabricApi": "0.116.17+1.21.1",
                "waystones": "21.1.42+1.21.1",
                "balm": "21.0.65+1.21.1",
            },
            targets["fabric-1.21.1"]["current"],
        )
        self.assertEqual("0.19.5", targets["fabric-1.21.11"]["current"]["fabricLoader"])

    def test_rejects_wrong_26_source_commit(self):
        matrix = json.loads((ROOT / "gradle" / "targets.json").read_text(encoding="utf-8"))
        target = next(item for item in matrix["targets"] if item["id"] == "fabric-26.1.1")
        target["waystonesSource"]["commit"] = "0" * 40
        with self.assertRaises(ValueError):
            verify.validate_matrix_shape(matrix)

    def test_rejects_old_mod_id(self):
        matrix = json.loads((ROOT / "gradle" / "targets.json").read_text(encoding="utf-8"))
        matrix["modId"] = "waystonesplayer"
        with self.assertRaises(ValueError):
            verify.validate_matrix_shape(matrix)

    def test_rejects_missing_embedded_shogi_api_contract(self):
        matrix = json.loads((ROOT / "gradle" / "targets.json").read_text(encoding="utf-8"))
        target = next(item for item in matrix["targets"] if item["id"] == "neoforge-26.2")
        target["current"].pop("shogiApiEmbedded")
        with self.assertRaises(ValueError):
            verify.validate_matrix_shape(matrix)

    def test_26_runtime_compatible_dependency_floors(self):
        matrix = json.loads((ROOT / "gradle" / "targets.json").read_text(encoding="utf-8"))
        targets = {item["id"]: item for item in matrix["targets"]}

        self.assertEqual("26.1.0.19-beta", targets["neoforge-26.1"]["minimum"]["neoforge"])
        self.assertEqual("26.1.0.4", targets["neoforge-26.1"]["minimum"]["waystones"])
        self.assertEqual("26.1.0.6", targets["neoforge-26.1"]["minimum"]["balm"])
        self.assertEqual("26.1.0.1", targets["neoforge-26.1"]["current"]["shogi"])
        self.assertEqual("26.1.1.8-beta", targets["neoforge-26.1.1"]["minimum"]["neoforge"])
        self.assertEqual("26.1.2.0-beta", targets["neoforge-26.1.2"]["minimum"]["neoforge"])
        self.assertEqual("26.1.0.1", targets["fabric-26.1"]["current"]["shogi"])
        self.assertEqual("0.145.4+26.1.1", targets["fabric-26.1.1"]["minimum"]["fabricApi"])


class TargetProperties26Test(unittest.TestCase):
    TARGET = {
        "id": "fabric-26.1",
        "branch": "fabric/26.x",
        "loader": "fabric",
        "minecraft": ["26.1"],
        "families": {
            "balm": "load-context-26",
            "screen": "graphics-extractor-26",
            "teleport": "shogi-26",
        },
    }
    ROOTS = (
        "loader/fabric-load-context-26",
        "screen/graphics-extractor-26",
        "teleport/shogi-26",
    )

    def fixture(self, roots=None):
        temporary = tempfile.TemporaryDirectory()
        root = Path(temporary.name)
        selected_roots = roots if roots is not None else self.ROOTS
        for adapter_root in selected_roots:
            java_root = root / "adapters" / adapter_root / "java"
            java_root.mkdir(parents=True)
            (java_root / "Adapter.java").write_text("class Adapter {}\n", encoding="utf-8")
        common_root = root / "common" / "src" / "main" / "java"
        fabric_root = root / "fabric" / "src" / "main" / "java"
        common_root.mkdir(parents=True)
        fabric_root.mkdir(parents=True)
        properties = root / "targets" / self.TARGET["id"] / "target.properties"
        properties.parent.mkdir(parents=True)
        properties.write_text(
            f"targetId={self.TARGET['id']}\n"
            "commonExcludes=\n"
            "fabricExcludes=\n"
            f"adapterRoots={','.join(selected_roots)}\n",
            encoding="utf-8",
        )
        return temporary, root

    def test_accepts_26_families(self):
        temporary, root = self.fixture()
        with temporary:
            verify.validate_target_properties(root, "fabric/26.x", [self.TARGET])

    def test_rejects_legacy_root_in_26_family(self):
        temporary, root = self.fixture(roots=(
            "loader/fabric-load-context-26",
            "screen/legacy-1.21.3-1.21.10",
            "teleport/shogi-26",
        ))
        with temporary, self.assertRaises(ValueError):
            verify.validate_target_properties(root, "fabric/26.x", [self.TARGET])


if __name__ == "__main__":
    unittest.main()
