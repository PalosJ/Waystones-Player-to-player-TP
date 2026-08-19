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


if __name__ == "__main__":
    unittest.main()
