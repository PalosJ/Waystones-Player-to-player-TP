import pathlib
import unittest


REPO = pathlib.Path(__file__).resolve().parents[2]


class RuntimeProjectContractTest(unittest.TestCase):
    def test_26_runtime_uses_the_exact_loader_branch_and_shogi_stack(self) -> None:
        settings = (REPO / "settings.gradle").read_text(encoding="utf-8")
        loader = "fabric" if "fabricTargets" in settings else "neoforge"
        runtime = (
            REPO / "runtime" / f"{loader}-smoke" / "build.gradle"
        ).read_text(encoding="utf-8")

        self.assertIn(f"target.branch != '{loader}/26.x'", runtime)
        self.assertIn(f"shogi-{loader}:${{stack.shogi}}", runtime)
        self.assertIn("version { strictly(stack.shogi as String) }", runtime)

        if loader == "fabric":
            self.assertIn("id 'net.fabricmc.fabric-loom'", runtime)
            self.assertNotIn("fabric-loom-remap", runtime)
            self.assertNotIn("officialMojangMappings", runtime)
            self.assertNotIn("modImplementation", runtime)
            self.assertNotIn("modRuntimeOnly", runtime)


if __name__ == "__main__":
    unittest.main()
