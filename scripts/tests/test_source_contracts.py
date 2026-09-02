import pathlib
import unittest


REPO = pathlib.Path(__file__).resolve().parents[2]


def source(relative_path: str) -> str:
    return (REPO / relative_path).read_text(encoding="utf-8")


class ClientDirectorySourceContractTest(unittest.TestCase):
    def test_render_path_never_queries_connection_or_profile_skin(self) -> None:
        button = source(
            "common/src/main/java/com/palosj/waystonesptpt/client/widget/PlayerTeleportButton.java"
        )
        render = button.split("public void renderString", 1)[1].split("public void tickSkin", 1)[0]
        self.assertNotIn("getConnection", render)
        self.assertNotIn("getSkin", render)
        self.assertIn("playerInfo.getSkin().texture()", button.split("public void tickSkin", 1)[1])

    def test_directory_uses_exact_values_throttle_and_explicit_cleanup(self) -> None:
        injector = source(
            "common/src/main/java/com/palosj/waystonesptpt/client/WaystonePlayerScreenInjector.java"
        )
        refresh = source(
            "core/src/main/java/com/palosj/waystonesptpt/client/PlayerListRefresh.java"
        )
        policy = source(
            "core/src/main/java/com/palosj/waystonesptpt/client/PlayerDirectoryRefreshPolicy.java"
        )
        self.assertNotIn("hashCode()", injector)
        self.assertIn("!previous.equals(current)", refresh)
        self.assertIn("hasSamePlayersIgnoringOrder", refresh)
        self.assertIn("cachedPlayerInfoById", injector)
        self.assertIn("selfId.equals(cachedSelf)", injector)
        self.assertIn("REFRESH_INTERVAL_TICKS = 5", policy)
        self.assertIn("PANELS.detach(candidate)", injector)
        self.assertIn("resetDirectoryCache()", injector)

class NetworkAuthoritySourceContractTest(unittest.TestCase):
    def test_payload_is_serverbound_uuid_only_and_service_revalidates_first(self) -> None:
        payload = source(
            "common/src/main/java/com/palosj/waystonesptpt/network/payload/RequestPlayerTeleportPayload.java"
        )
        networking = source(
            "common/src/main/java/com/palosj/waystonesptpt/network/ModNetworking.java"
        )
        service = source(
            "common/src/main/java/com/palosj/waystonesptpt/teleport/PlayerTeleportService.java"
        )

        self.assertIn("record RequestPlayerTeleportPayload(UUID targetPlayerId)", payload)
        self.assertNotIn("BlockPos", payload)
        self.assertNotIn("experience", payload.lower())
        self.assertIn("registerServerboundPacket", networking)
        self.assertLess(service.index("runtime.resolveWarpStoneUse"), service.index("runtime.tryTeleport"))
        self.assertLess(service.index("runtime.resolveOnlineTarget"), service.index("runtime.tryTeleport"))

    def test_online_targets_do_not_treat_listing_privacy_as_authorization(self) -> None:
        runtime = source(
            "common/src/main/java/com/palosj/waystonesptpt/teleport/TeleportRuntime.java"
        )
        evaluator = source(
            "common/src/main/java/com/palosj/waystonesptpt/compat/WaystonesTeleportEvaluator.java"
        )
        compat = source(
            "common/src/main/java/com/palosj/waystonesptpt/compat/WaystonesCompat.java"
        )

        self.assertIn("resolveOnlineTarget", runtime)
        self.assertNotIn("allowsListing", runtime)
        self.assertNotIn("hasAdjacentNonSuffocatingSpace", evaluator)
        self.assertNotIn("isCurrentPositionNonSuffocating", evaluator)
        self.assertNotIn("hasAdjacentNonSuffocatingSpace", compat)

    def test_successful_player_teleports_restore_the_original_rotation(self) -> None:
        service = source(
            "common/src/main/java/com/palosj/waystonesptpt/teleport/PlayerTeleportService.java"
        )

        self.assertLess(service.index("runtime.captureRotation"), service.index("runtime.tryTeleport"))
        self.assertGreater(service.index("runtime.restoreRotation"), service.index("runtime.tryTeleport"))

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
        self.assertIn("verify-release-manifest.py", fetcher)

    def test_build_and_runtime_share_the_canonical_workflow_name(self) -> None:
        build = source(".github/workflows/build.yml")
        runtime = source(".github/workflows/runtime-smoke.yml")

        self.assertTrue(build.startswith("name: Build\n"))
        self.assertIn("workflows: [Build]", runtime)
        if "branches:\n      - main" in build:
            self.assertIn("github.ref == 'refs/heads/main'", build)

    def test_runtime_concurrency_is_scoped_to_the_triggering_build_branch(self) -> None:
        workflow = source(".github/workflows/runtime-smoke.yml")
        concurrency = workflow.split("concurrency:", 1)[1].split("jobs:", 1)[0]

        self.assertIn(
            "github.event.workflow_run.head_branch || github.ref",
            concurrency,
        )


class ClientTickFailureSourceContractTest(unittest.TestCase):
    def test_tick_failure_is_scoped_to_one_screen(self) -> None:
        setup = source(
            "common/src/main/java/com/palosj/waystonesptpt/client/WaystoneClientSetup.java"
        )

        self.assertIn("Screen tickInjectionDisabledScreen", setup)
        self.assertNotIn("boolean tickInjectionDisabled", setup)
        self.assertIn("live refresh is disabled for this screen", setup)


if __name__ == "__main__":
    unittest.main()
