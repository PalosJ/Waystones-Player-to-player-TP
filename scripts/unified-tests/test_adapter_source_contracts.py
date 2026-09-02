import pathlib
import unittest


REPO = pathlib.Path(__file__).resolve().parents[2]


def source(relative_path: str) -> str:
    return (REPO / relative_path).read_text(encoding="utf-8")


def existing(relative_paths):
    return [path for path in relative_paths if (REPO / path).is_file()]


class UnifiedScreenFamilySourceContractTest(unittest.TestCase):
    def test_all_screen_families_cache_skins_reuse_rows_and_clean_up(self) -> None:
        buttons = existing([
            "adapters/screen/player-skin-1.21.3-1.21.10/java/com/palosj/waystonesptpt/client/widget/PlayerTeleportButton.java",
            "adapters/screen/player-skin-1.21.9-1.21.10/java/com/palosj/waystonesptpt/client/widget/PlayerTeleportButton.java",
            "adapters/screen/player-skin-1.21.11/java/com/palosj/waystonesptpt/client/widget/PlayerTeleportButton.java",
            "adapters/screen/graphics-extractor-26/java/com/palosj/waystonesptpt/client/widget/PlayerTeleportButton.java",
        ])
        for path in buttons:
            with self.subTest(path=path):
                button = source(path)
                self.assertNotIn("getConnection()", button)
                self.assertNotIn("refreshSkinFromConnection", button)
                self.assertIn("SkinRetryThrottle", button)
                self.assertIn("public void tickSkin()", button)

        lists = existing([
            "adapters/screen/list-1.21.4-1.21.10/java/com/palosj/waystonesptpt/client/widget/PlayerDestinationList.java",
            "adapters/screen/list-1.21.9-1.21.11/java/com/palosj/waystonesptpt/client/widget/PlayerDestinationList.java",
            "adapters/screen/graphics-extractor-26/java/com/palosj/waystonesptpt/client/widget/PlayerDestinationList.java",
        ])
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

        injectors = existing([
            "adapters/screen/legacy-1.21.3-1.21.10/java/com/palosj/waystonesptpt/client/WaystonePlayerScreenInjector.java",
            "adapters/screen/platform-1.21.11/java/com/palosj/waystonesptpt/client/WaystonePlayerScreenInjector.java",
            "adapters/screen/graphics-extractor-26/java/com/palosj/waystonesptpt/client/WaystonePlayerScreenInjector.java",
        ])
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

        setups = existing([
            "adapters/screen/legacy-1.21.3-1.21.10/java/com/palosj/waystonesptpt/client/WaystoneClientSetup.java",
            "adapters/screen/platform-1.21.11/java/com/palosj/waystonesptpt/client/WaystoneClientSetup.java",
        ])
        for path in setups:
            with self.subTest(path=path):
                setup = source(path)
                self.assertNotIn("boolean refreshDisabled", setup)
                self.assertRegex(setup, r"private static Screen \w+(?:Disabled|Unavailable)Screen;")
                self.assertRegex(setup, r"(?:Disabled|Unavailable)Screen = screen;")
                self.assertRegex(setup, r"(?:Disabled|Unavailable)Screen = null;")

        graphics_setup_path = (
            "adapters/screen/graphics-extractor-26/java/com/palosj/waystonesptpt/client/WaystoneClientSetup.java"
        )
        if REPO.joinpath(graphics_setup_path).is_file():
            setup = source(graphics_setup_path)
            self.assertIn("activeWaystoneScreen", setup)
            self.assertIn("onScreenClosed(activeWaystoneScreen)", setup)
            self.assertNotIn("minecraft.screen", setup)
            self.assertNotIn("minecraft.gui.screen()", setup)


class UnifiedNetworkFamilySourceContractTest(unittest.TestCase):
    def test_each_balm_network_family_is_serverbound_and_delegates_to_revalidation(self) -> None:
        adapters = existing([
            "adapters/network/legacy-codec/java/com/palosj/waystonesptpt/network/ModNetworking.java",
            "adapters/network/stream-codec-no-version/java/com/palosj/waystonesptpt/network/ModNetworking.java",
        ])
        for path in adapters:
            with self.subTest(path=path):
                networking = source(path)
                self.assertIn("registerServerboundPacket", networking)
                self.assertIn("PlayerTeleportService.handleRequest", networking)
                self.assertNotIn("registerClientboundPacket", networking)


class UnifiedTeleportContextFamilySourceContractTest(unittest.TestCase):
    def test_identifier_compat_does_not_reintroduce_arrival_suffocation_check(self) -> None:
        path = "adapters/identifier/1.21.11/java/com/palosj/waystonesptpt/compat/WaystonesCompat.java"
        if not REPO.joinpath(path).is_file():
            return
        compat = source(path)
        self.assertNotIn("isCurrentPositionNonSuffocating", compat)
        self.assertNotIn("hasAdjacentNonSuffocatingSpace", compat)

    def test_optional_hand_family_never_directly_links_the_delegate(self) -> None:
        context_path = (
            "adapters/teleport/context-optional-hand-21.3-21.10/java/com/palosj/waystonesptpt/compat/"
            "LockedWaystoneTeleportContext.java"
        )
        if not REPO.joinpath(context_path).is_file():
            return
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
        if not list(REPO.glob(f"targets/{loader}-1.21*/target.properties")):
            return
        found = 0
        for suffix in target_suffixes:
            path = f"targets/{loader}-{suffix}/target.properties"
            if not REPO.joinpath(path).is_file():
                continue
            found += 1
            with self.subTest(path=path):
                properties = source(path)
                self.assertIn(
                    "com/palosj/waystonesptpt/compat/LockedWaystoneTeleportContext.java",
                    properties,
                )
                self.assertIn("teleport/legacy-21.3", properties)
                self.assertIn("teleport/context-optional-hand-21.3-21.10", properties)
        if not list(REPO.glob(f"targets/{loader}-26*/target.properties")):
            self.assertGreater(found, 0)

    def test_26_targets_select_locked_shogi_and_graphics_extractor_families(self) -> None:
        settings = source("settings.gradle")
        loader = "fabric" if "fabricTargets" in settings else "neoforge"
        target_files = sorted(REPO.glob(f"targets/{loader}-26*/target.properties"))
        if not target_files:
            return
        self.assertEqual(4, len(target_files))
        for target_file in target_files:
            properties = target_file.read_text(encoding="utf-8")
            with self.subTest(path=str(target_file.relative_to(REPO))):
                self.assertIn(
                    "com/palosj/waystonesptpt/compat/LockedWaystoneTeleportContext.java",
                    properties,
                )
                self.assertIn("teleport/shogi-26", properties)
                self.assertIn("screen/graphics-extractor-26", properties)
                self.assertIn(f"loader/{loader}-load-context-26", properties)

        context = source(
            "adapters/teleport/shogi-26/java/com/palosj/waystonesptpt/compat/LockedWaystoneTeleportContext.java"
        )
        self.assertIn("requireUnmodified", context)
        self.assertIn("requirementReplacementAttempted", context)
        self.assertIn("feeContextMutationAttempted", context)
        self.assertIn("executorOverrideAttempted", context)


if __name__ == "__main__":
    unittest.main()
