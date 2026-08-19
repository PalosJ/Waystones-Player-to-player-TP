import pathlib
import unittest


REPO = pathlib.Path(__file__).resolve().parents[2]


class FabricConfigContractTest(unittest.TestCase):
    def test_fabric_keeps_the_shared_key_and_default_as_global_config(self) -> None:
        config = (
            REPO
            / "fabric/src/main/java/com/palosj/waystonesplayer/fabric/FabricWaystonesPlayerConfig.java"
        ).read_text(encoding="utf-8")

        self.assertIn('FILE_NAME = "waystonesplayer-server.toml"', config)
        self.assertIn('MODE_KEY = "playerTeleportExperienceMode"', config)
        self.assertIn('playerTeleportExperienceMode = "NEVER"', config)
        self.assertIn("PlayerTeleportExperienceMode.NEVER", config)
        self.assertIn("FabricLoader.getInstance().getConfigDir()", config)
        self.assertNotIn("serverconfig", config)

    def test_docs_do_not_claim_world_overrides_or_hot_reload(self) -> None:
        readme = (REPO / "README.md").read_text(encoding="utf-8")
        architecture = (REPO / "docs/ARCHITECTURE.md").read_text(encoding="utf-8")
        compatibility = (REPO / "docs/COMPATIBILITY.md").read_text(encoding="utf-8")

        self.assertIn("它是实例全局配置", readme)
        self.assertIn("只提供实例全局配置", architecture)
        self.assertIn("不得在 README 或平台页面声称按世界覆盖或热重载", compatibility)


if __name__ == "__main__":
    unittest.main()
