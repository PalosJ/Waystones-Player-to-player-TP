from __future__ import annotations

import importlib.util
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
    )

    def fixture(self, target_id: str = "neoforge-1.21.4", roots=None):
        temporary = tempfile.TemporaryDirectory()
        root = Path(temporary.name)
        selected_roots = roots if roots is not None else self.ROOTS
        for adapter_root in selected_roots:
            java_root = root / "adapters" / adapter_root / "java"
            java_root.mkdir(parents=True)
            (java_root / "Adapter.java").write_text("class Adapter {}\n", encoding="utf-8")
        properties = root / "targets" / self.TARGET["id"] / "target.properties"
        properties.parent.mkdir(parents=True)
        properties.write_text(
            f"targetId={target_id}\n"
            "commonExcludes=\n"
            "neoforgeExcludes=\n"
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


if __name__ == "__main__":
    unittest.main()
