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
                self.assertIn("restoreFocusedPlayer(previousFocus, previous, players)", player_list)
                self.assertIn("clearButtonFocus", player_list)
                self.assertIn("setFocused(null)", player_list)

        injectors = [
            "adapters/screen/legacy-1.21.3-1.21.10/java/com/palosj/waystonesplayer/client/WaystonePlayerScreenInjector.java",
            "adapters/screen/platform-1.21.11/java/com/palosj/waystonesplayer/client/WaystonePlayerScreenInjector.java",
        ]
        for path in injectors:
            with self.subTest(path=path):
                injector = source(path)
                self.assertNotIn("hashCode()", injector)
                self.assertIn("PlayerDirectoryRefreshPolicy.shouldRefresh", injector)
                self.assertIn("hasSamePlayersIgnoringOrder", injector)
                self.assertIn("cachedPlayerInfoById", injector)
                self.assertIn("selfId.equals(cachedSelf)", injector)
                self.assertIn("public static void onScreenClosed", injector)
                self.assertIn("tickVisibleEntries", injector)

        setups = [
            "adapters/screen/legacy-1.21.3-1.21.10/java/com/palosj/waystonesplayer/client/WaystoneClientSetup.java",
            "adapters/screen/platform-1.21.11/java/com/palosj/waystonesplayer/client/WaystoneClientSetup.java",
        ]
        for path in setups:
            with self.subTest(path=path):
                setup = source(path)
                self.assertNotIn("boolean refreshDisabled", setup)
                self.assertRegex(setup, r"private static Screen \w+(?:Disabled|Unavailable)Screen;")
                self.assertRegex(setup, r"(?:Disabled|Unavailable)Screen = screen;")
                self.assertRegex(setup, r"(?:Disabled|Unavailable)Screen = null;")


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


class UnifiedTeleportContextFamilySourceContractTest(unittest.TestCase):
    def test_identifier_compat_keeps_post_move_suffocation_check(self) -> None:
        compat = source(
            "adapters/identifier/1.21.11/java/com/palosj/waystonesplayer/compat/WaystonesCompat.java"
        )
        self.assertIn("isCurrentPositionNonSuffocating", compat)
        self.assertIn("player.blockPosition()", compat)
        self.assertIn("body.above()", compat)

    def test_optional_hand_family_never_directly_links_the_delegate(self) -> None:
        context_path = (
            "adapters/teleport/context-optional-hand-21.3-21.10/java/com/palosj/waystonesplayer/compat/"
            "LockedWaystoneTeleportContext.java"
        )
        context = source(context_path)

        self.assertIn('findMethod("getWarpHand")', context)
        self.assertIn('"setWarpHand", InteractionHand.class', context)
        self.assertNotIn("delegate.getWarpHand()", context)
        self.assertNotIn("delegate.setWarpHand", context)
        self.assertNotIn("@Override\n    public InteractionHand getWarpHand", context)
        self.assertIn('findMethod("appliesModifiers")', context)
        self.assertIn('"setAppliesModifiers", boolean.class', context)

    def test_legacy_targets_replace_the_common_context_implementation(self) -> None:
        settings = source("settings.gradle")
        loader = "fabric" if "fabricTargets" in settings else "neoforge"
        target_suffixes = [
            "1.21.2-1.21.3", "1.21.4", "1.21.5", "1.21.6", "1.21.7", "1.21.8", "1.21.9", "1.21.10"
        ]
        for suffix in target_suffixes:
            path = f"targets/{loader}-{suffix}/target.properties"
            with self.subTest(path=path):
                properties = source(path)
                self.assertIn(
                    "com/palosj/waystonesplayer/compat/LockedWaystoneTeleportContext.java",
                    properties,
                )
                self.assertIn("teleport/legacy-21.3", properties)
                self.assertIn("teleport/context-optional-hand-21.3-21.10", properties)


if __name__ == "__main__":
    unittest.main()
