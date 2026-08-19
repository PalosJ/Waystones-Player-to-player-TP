import pathlib
import unittest


REPO = pathlib.Path(__file__).resolve().parents[2]


def source(relative_path: str) -> str:
    return (REPO / relative_path).read_text(encoding="utf-8")


class UnifiedScreenFamilySourceContractTest(unittest.TestCase):
    def test_all_screen_families_cache_skins_reuse_rows_and_clean_up(self) -> None:
        buttons = [
            "adapters/screen/player-skin-1.21.3-1.21.10/java/com/palosj/waystonesplayer/client/widget/PlayerTeleportButton.java",
            "adapters/screen/player-skin-1.21.9-1.21.10/java/com/palosj/waystonesplayer/client/widget/PlayerTeleportButton.java",
            "adapters/screen/player-skin-1.21.11/java/com/palosj/waystonesplayer/client/widget/PlayerTeleportButton.java",
        ]
        for path in buttons:
            with self.subTest(path=path):
                button = source(path)
                self.assertNotIn("getConnection()", button)
                self.assertNotIn("refreshSkinFromConnection", button)
                self.assertIn("SkinRetryThrottle", button)
                self.assertIn("public void tickSkin()", button)

        lists = [
            "adapters/screen/list-1.21.4-1.21.10/java/com/palosj/waystonesplayer/client/widget/PlayerDestinationList.java",
            "adapters/screen/list-1.21.9-1.21.11/java/com/palosj/waystonesplayer/client/widget/PlayerDestinationList.java",
        ]
        for path in lists:
            with self.subTest(path=path):
                player_list = source(path)
                self.assertIn("entriesById", player_list)
                self.assertIn("previousEntries.remove(playerId)", player_list)
                self.assertIn("entry.bind(playerInfo)", player_list)
                self.assertIn("tickVisibleEntries", player_list)

        injectors = [
            "adapters/screen/legacy-1.21.3-1.21.10/java/com/palosj/waystonesplayer/client/WaystonePlayerScreenInjector.java",
            "adapters/screen/platform-1.21.11/java/com/palosj/waystonesplayer/client/WaystonePlayerScreenInjector.java",
        ]
        for path in injectors:
            with self.subTest(path=path):
                injector = source(path)
                self.assertNotIn("hashCode()", injector)
                self.assertIn("PlayerDirectoryRefreshPolicy.shouldRefresh", injector)
                self.assertIn("public static void onScreenClosed", injector)
                self.assertIn("tickVisibleEntries", injector)


class UnifiedNetworkFamilySourceContractTest(unittest.TestCase):
    def test_each_balm_network_family_is_serverbound_and_delegates_to_revalidation(self) -> None:
        adapters = [
            "adapters/network/legacy-codec/java/com/palosj/waystonesplayer/network/ModNetworking.java",
            "adapters/network/stream-codec-no-version/java/com/palosj/waystonesplayer/network/ModNetworking.java",
        ]
        for path in adapters:
            with self.subTest(path=path):
                networking = source(path)
                self.assertIn("registerServerboundPacket", networking)
                self.assertIn("PlayerTeleportService.handleRequest", networking)
                self.assertNotIn("registerClientboundPacket", networking)


if __name__ == "__main__":
    unittest.main()
