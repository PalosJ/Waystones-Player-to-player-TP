package com.palosj.waystonesplayer.teleport;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

import org.junit.jupiter.api.Test;

import com.palosj.waystonesplayer.PlayerTeleportExperienceMode;
import com.palosj.waystonesplayer.compat.WaystonesCompat;

import net.blay09.mods.waystones.api.WaystoneTeleportContext;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;

class PlayerTeleportServiceTest {
    private static final UUID SENDER_ID = new UUID(0, 1);
    private static final UUID TARGET_ID = new UUID(0, 2);

    @Test
    void rejectsInvalidMenuAndUnavailableOrSelfTargetsBeforeTeleporting() {
        FakeRuntime invalidMenu = new FakeRuntime();
        invalidMenu.warpStoneUse = Optional.empty();
        execute(invalidMenu);
        assertEquals(List.of("message.waystonesplayer.invalid_context"), invalidMenu.messages);
        assertEquals(0, invalidMenu.teleportCalls);

        FakeRuntime unavailable = new FakeRuntime();
        unavailable.target = Optional.empty();
        execute(unavailable);
        assertEquals(List.of("message.waystonesplayer.target_unavailable"), unavailable.messages);
        assertEquals(0, unavailable.teleportCalls);

        FakeRuntime self = new FakeRuntime();
        self.target = Optional.of(new TeleportRuntimeBoundary.TargetPlayer(null, SENDER_ID));
        execute(self);
        assertEquals(List.of("message.waystonesplayer.target_self"), self.messages);
        assertEquals(0, self.teleportCalls);
    }

    @Test
    void mapsCompatibilityAffordabilityAndFailedMovementWithoutDurability() {
        assertRejected(Optional.empty(), "message.waystonesplayer.compatibility_unavailable");
        assertRejected(Optional.of(TeleportOutcome.UNAFFORDABLE),
                "message.waystonesplayer.insufficient_experience");
        assertRejected(Optional.of(TeleportOutcome.FAILED), "message.waystonesplayer.teleport_failed");
    }

    @Test
    void successfulMainAndOffhandTeleportsDamageExactlyOnceAndClose() {
        for (InteractionHand hand : InteractionHand.values()) {
            FakeRuntime runtime = new FakeRuntime();
            runtime.warpStoneUse = Optional.of(new WaystonesCompat.WarpStoneUse(null, hand));

            execute(runtime);

            assertEquals(1, runtime.teleportCalls);
            assertEquals(1, runtime.resetFallDistanceCalls);
            assertEquals(1, runtime.damageCalls);
            assertEquals(1, runtime.closeCalls);
            assertSame(hand, runtime.damagedUse.hand());
            assertEquals(List.of(), runtime.messages);
        }
    }

    @Test
    void confirmedMovementWithRemovedItemNeverDamagesAnotherStackOrRollsBackSuccess() {
        FakeRuntime runtime = new FakeRuntime();
        runtime.durabilityTarget = Optional.empty();

        execute(runtime);

        assertEquals(1, runtime.resetFallDistanceCalls);
        assertEquals(0, runtime.damageCalls);
        assertEquals(1, runtime.closeCalls);
        assertEquals(List.of("message.waystonesplayer.post_teleport_item_changed"), runtime.messages);
    }

    @Test
    void incompatibleConfirmedMovementStillSettlesDurabilityAndCloses() {
        FakeRuntime runtime = new FakeRuntime();
        runtime.outcome = Optional.of(TeleportOutcome.MOVED_INCOMPATIBLY);

        execute(runtime);

        assertEquals(1, runtime.resetFallDistanceCalls);
        assertEquals(1, runtime.damageCalls);
        assertEquals(1, runtime.closeCalls);
        assertEquals(List.of("message.waystonesplayer.post_teleport_destination_changed"), runtime.messages);
    }

    @Test
    void repeatedPacketsAreRateLimitedBeforeRuntimeStateIsReadAgain() {
        FakeRuntime runtime = new FakeRuntime();
        RequestRateLimiter limiter = new RequestRateLimiter(10);
        PlayerTeleportService.handleRequest(
                null, TARGET_ID, PlayerTeleportExperienceMode.NEVER, runtime, limiter);
        PlayerTeleportService.handleRequest(
                null, TARGET_ID, PlayerTeleportExperienceMode.NEVER, runtime, limiter);

        assertEquals(1, runtime.teleportCalls);
        assertEquals(List.of("message.waystonesplayer.teleport_cooling_down"), runtime.messages);
    }

    private static void assertRejected(Optional<TeleportOutcome> outcome, String expectedMessage) {
        FakeRuntime runtime = new FakeRuntime();
        runtime.outcome = outcome;
        execute(runtime);
        assertEquals(1, runtime.teleportCalls);
        assertEquals(0, runtime.damageCalls);
        assertEquals(0, runtime.closeCalls);
        assertEquals(List.of(expectedMessage), runtime.messages);
    }

    private static void execute(FakeRuntime runtime) {
        PlayerTeleportService.handleRequest(
                null,
                TARGET_ID,
                PlayerTeleportExperienceMode.NEVER,
                runtime,
                new RequestRateLimiter(10));
    }

    private static final class FakeRuntime implements TeleportRuntimeBoundary {
        private final List<String> messages = new ArrayList<>();
        private Optional<WaystonesCompat.WarpStoneUse> warpStoneUse = Optional.of(
                new WaystonesCompat.WarpStoneUse(null, InteractionHand.MAIN_HAND));
        private Optional<TargetPlayer> target = Optional.of(new TargetPlayer(null, TARGET_ID));
        private Optional<TeleportOutcome> outcome = Optional.of(TeleportOutcome.SUCCESS);
        private Optional<DurabilityTarget> durabilityTarget = Optional.of(new DurabilityTarget(null));
        private int teleportCalls;
        private int resetFallDistanceCalls;
        private int damageCalls;
        private int closeCalls;
        private WaystonesCompat.WarpStoneUse damagedUse;

        @Override
        public int currentTick(ServerPlayer sender) {
            return 100;
        }

        @Override
        public UUID playerId(ServerPlayer player) {
            return SENDER_ID;
        }

        @Override
        public String playerName(ServerPlayer player) {
            return "Sender";
        }

        @Override
        public Optional<WaystonesCompat.WarpStoneUse> resolveWarpStoneUse(ServerPlayer sender) {
            return warpStoneUse;
        }

        @Override
        public Optional<TargetPlayer> resolveListedTarget(ServerPlayer sender, UUID targetPlayerId) {
            return target;
        }

        @Override
        public CompletionStage<Optional<TeleportOutcome>> tryTeleport(
                ServerPlayer sender,
                ServerPlayer target,
                WaystonesCompat.WarpStoneUse use,
                PlayerTeleportExperienceMode experienceMode) {
            teleportCalls++;
            return CompletableFuture.completedFuture(outcome);
        }

        @Override
        public void executeOnServerThread(ServerPlayer sender, Runnable action) {
            action.run();
        }

        @Override
        public Optional<DurabilityTarget> resolveDurabilityTarget(
                ServerPlayer sender,
                WaystonesCompat.WarpStoneUse use) {
            return durabilityTarget;
        }

        @Override
        public void damageWarpStone(
                DurabilityTarget target,
                ServerPlayer sender,
                WaystonesCompat.WarpStoneUse use) {
            damageCalls++;
            damagedUse = use;
        }

        @Override
        public void resetFallDistance(ServerPlayer sender) {
            resetFallDistanceCalls++;
        }

        @Override
        public void closeContainer(ServerPlayer sender) {
            closeCalls++;
        }

        @Override
        public void displayMessage(ServerPlayer sender, String translationKey) {
            messages.add(translationKey);
        }

        @Override
        public boolean isWarpStoneUseBound(ServerPlayer sender, WaystonesCompat.WarpStoneUse use) {
            return true;
        }

        @Override
        public boolean hasAdjacentNonSuffocatingSpace(
                ServerPlayer sender,
                WaystoneTeleportContext context) {
            return true;
        }
    }
}
