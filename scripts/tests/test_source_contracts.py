import pathlib
import unittest


REPO = pathlib.Path(__file__).resolve().parents[2]


def source(relative_path: str) -> str:
    return (REPO / relative_path).read_text(encoding="utf-8")


class ClientDirectorySourceContractTest(unittest.TestCase):
    def test_render_path_never_queries_connection_or_profile_skin(self) -> None:
        button = source(
            "common/src/main/java/com/palosj/waystonesplayer/client/widget/PlayerTeleportButton.java"
        )
        render = button.split("public void renderString", 1)[1].split("public void tickSkin", 1)[0]
        self.assertNotIn("getConnection", render)
        self.assertNotIn("getSkin", render)
        self.assertIn("playerInfo.getSkin().texture()", button.split("public void tickSkin", 1)[1])

    def test_directory_uses_exact_values_throttle_and_explicit_cleanup(self) -> None:
        injector = source(
            "common/src/main/java/com/palosj/waystonesplayer/client/WaystonePlayerScreenInjector.java"
        )
        refresh = source(
            "core/src/main/java/com/palosj/waystonesplayer/client/PlayerListRefresh.java"
        )
        policy = source(
            "core/src/main/java/com/palosj/waystonesplayer/client/PlayerDirectoryRefreshPolicy.java"
        )
        self.assertNotIn("hashCode()", injector)
        self.assertIn("!previous.equals(current)", refresh)
        self.assertIn("REFRESH_INTERVAL_TICKS = 5", policy)
        self.assertIn("PANELS.detach(candidate)", injector)
        self.assertIn("resetDirectoryCache()", injector)

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


class NetworkAuthoritySourceContractTest(unittest.TestCase):
    def test_payload_is_serverbound_uuid_only_and_service_revalidates_first(self) -> None:
        payload = source(
            "common/src/main/java/com/palosj/waystonesplayer/network/payload/RequestPlayerTeleportPayload.java"
        )
        networking = source(
            "common/src/main/java/com/palosj/waystonesplayer/network/ModNetworking.java"
        )
        service = source(
            "common/src/main/java/com/palosj/waystonesplayer/teleport/PlayerTeleportService.java"
        )

        self.assertIn("record RequestPlayerTeleportPayload(UUID targetPlayerId)", payload)
        self.assertNotIn("BlockPos", payload)
        self.assertNotIn("experience", payload.lower())
        self.assertIn("registerServerboundPacket", networking)
        self.assertLess(service.index("runtime.resolveWarpStoneUse"), service.index("runtime.tryTeleport"))
        self.assertLess(service.index("runtime.resolveListedTarget"), service.index("runtime.tryTeleport"))

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


class RuntimeWorkflowSourceContractTest(unittest.TestCase):
    def test_runtime_consumes_only_a_trusted_exact_build_artifact(self) -> None:
        workflow = source(".github/workflows/runtime-smoke.yml")
        fetcher = source("scripts/fetch-build-artifact.sh")

        self.assertIn("workflow_run:", workflow)
        self.assertIn("github.event.workflow_run.conclusion == 'success'", workflow)
        self.assertIn("github.event.workflow_run.event == 'push'", workflow)
        self.assertIn("github.event.workflow_run.id", workflow)
        self.assertNotIn("./gradlew clean", workflow)
        self.assertIn('run_head_sha != "$head_sha"', fetcher)
        self.assertIn('run_branch != "$branch"', fetcher)
        self.assertIn('run_event != push && $run_event != workflow_dispatch', fetcher)
        self.assertIn("expected_workflow='Build'", fetcher)
        self.assertIn("expected_workflow='NeoForge 1.21.x Build'", fetcher)
        self.assertIn("expected_workflow='Fabric 1.21.x Build'", fetcher)


if __name__ == "__main__":
    unittest.main()
