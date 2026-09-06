package com.palosj.waystonesptpt.teleport;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.function.BooleanSupplier;
import org.junit.jupiter.api.AfterEach;

import org.junit.jupiter.api.Test;

import com.palosj.waystonesptpt.PlayerTeleportExperienceMode;
import com.palosj.waystonesptpt.compat.WaystonesCompat;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;

class PlayerTeleportServiceTest {
    private static final UUID SENDER_ID = new UUID(0, 1);
    private static final UUID TARGET_ID = new UUID(0, 2);

    @AfterEach
    void cleanup() { PlayerTeleportService.clearCooldown(SENDER_ID); }

    @Test
    void rejectsInvalidMenuAndUnavailableOrSelfTargetsBeforeTeleporting() {
        FakeRuntime invalidMenu = new FakeRuntime();
        invalidMenu.warpStoneUse = Optional.empty();
        execute(invalidMenu);
        assertEquals(List.of("message.waystonesptpt.invalid_context"), invalidMenu.messages);
        assertEquals(0, invalidMenu.teleportCalls);

        FakeRuntime unavailable = new FakeRuntime();
        unavailable.target = Optional.empty();
        execute(unavailable);
        assertEquals(List.of("message.waystonesptpt.target_unavailable"), unavailable.messages);
        assertEquals(0, unavailable.teleportCalls);

        FakeRuntime self = new FakeRuntime();
        self.target = Optional.of(new TeleportRuntimeBoundary.TargetPlayer(null, SENDER_ID));
        execute(self);
        assertEquals(List.of("message.waystonesptpt.target_self"), self.messages);
        assertEquals(0, self.teleportCalls);
    }

    @Test
    void mapsCompatibilityAffordabilityAndFailedMovementWithoutDurability() {
        assertRejected(Optional.empty(), "message.waystonesptpt.compatibility_unavailable");
        assertRejected(Optional.of(TeleportOutcome.UNAFFORDABLE),
                "message.waystonesptpt.insufficient_experience");
        assertRejected(Optional.of(TeleportOutcome.FAILED), "message.waystonesptpt.teleport_failed");
    }

    @Test
    void successfulMainAndOffhandTeleportsDamageExactlyOnceAndClose() {
        for (InteractionHand hand : InteractionHand.values()) {
            FakeRuntime runtime = new FakeRuntime();
            runtime.warpStoneUse = Optional.of(new WaystonesCompat.WarpStoneUse(null, hand));

            execute(runtime);

            assertEquals(1, runtime.teleportCalls);
            assertEquals(1, runtime.captureRotationCalls);
            assertEquals(1, runtime.restoreRotationCalls);
            assertEquals(1, runtime.resetFallDistanceCalls);
            assertEquals(1, runtime.damageCalls);
            assertEquals(1, runtime.closeCalls);
            assertSame(hand, runtime.damagedUse.hand());
            assertSame(runtime.rotation, runtime.restoredRotation);
            assertEquals(List.of(), runtime.messages);
        }
    }

    @Test
    void confirmedMovementWithRemovedItemNeverDamagesAnotherStackOrRollsBackSuccess() {
        FakeRuntime runtime = new FakeRuntime();
        runtime.durabilityTarget = Optional.empty();

        execute(runtime);

        assertEquals(1, runtime.resetFallDistanceCalls);
        assertEquals(1, runtime.restoreRotationCalls);
        assertEquals(0, runtime.damageCalls);
        assertEquals(1, runtime.closeCalls);
        assertEquals(List.of("message.waystonesptpt.post_teleport_item_changed"), runtime.messages);
    }

    @Test
    void incompatibleConfirmedMovementStillSettlesDurabilityAndCloses() {
        FakeRuntime runtime = new FakeRuntime();
        runtime.outcome = Optional.of(TeleportOutcome.MOVED_INCOMPATIBLY);

        execute(runtime);

        assertEquals(1, runtime.resetFallDistanceCalls);
        assertEquals(1, runtime.restoreRotationCalls);
        assertEquals(1, runtime.damageCalls);
        assertEquals(1, runtime.closeCalls);
        assertEquals(List.of("message.waystonesptpt.post_teleport_destination_changed"), runtime.messages);
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
        assertEquals(List.of("message.waystonesptpt.teleport_cooling_down"), runtime.messages);
    }

    @Test
    void pendingRequestBlocksAnotherAfterRateLimitExpires() {
        FakeRuntime runtime = new FakeRuntime();
        runtime.delayed = true;
        RequestRateLimiter limiter = new RequestRateLimiter(10);
        PlayerTeleportService.handleRequest(null, TARGET_ID, PlayerTeleportExperienceMode.NEVER, runtime, limiter);
        runtime.tick += 11;
        PlayerTeleportService.handleRequest(null, TARGET_ID, PlayerTeleportExperienceMode.NEVER, runtime, limiter);
        assertEquals(1, runtime.teleportCalls);
        assertEquals(List.of("message.waystonesptpt.teleport_pending"), runtime.messages);
        runtime.attempt.beginCommit(runtime.tick);
        runtime.completion.complete(Optional.of(TeleportOutcome.SUCCESS));
        assertEquals(1, runtime.damageCalls);
    }

    @Test
    void closingMenuAndTimeoutFenceLateSuccessfulCallbacks() {
        for (boolean timeout : List.of(false, true)) {
            FakeRuntime runtime = new FakeRuntime();
            runtime.delayed = true;
            execute(runtime);
            runtime.valid = timeout;
            runtime.tick += timeout ? 200 : 1;
            PlayerTeleportService.tickPendingRequests();
            runtime.completion.complete(Optional.of(TeleportOutcome.SUCCESS));
            assertEquals(TeleportAttempt.State.CANCELLED, runtime.attempt.state());
            assertEquals(0, runtime.damageCalls);
            assertEquals(0, runtime.closeCalls);
            PlayerTeleportService.clearCooldown(SENDER_ID);
        }
    }

    @Test
    void logoutFencesOldSessionWithoutClosingAReopenedMenu() {
        FakeRuntime old = new FakeRuntime();
        old.delayed = true;
        execute(old);
        PlayerTeleportService.clearCooldown(SENDER_ID);
        FakeRuntime replacement = new FakeRuntime();
        execute(replacement);
        old.completion.complete(Optional.of(TeleportOutcome.SUCCESS));
        assertEquals(0, old.damageCalls);
        assertEquals(0, old.closeCalls);
        assertEquals(1, replacement.damageCalls);
    }

    @Test
    void aCallbackCannotReportUncommittedMovementAsSuccess() {
        FakeRuntime runtime = new FakeRuntime();
        runtime.delayed = true;
        execute(runtime);
        runtime.completion.complete(Optional.of(TeleportOutcome.SUCCESS));
        assertEquals(0, runtime.damageCalls);
        assertEquals(0, runtime.closeCalls);
        assertEquals(List.of("message.waystonesptpt.teleport_failed"), runtime.messages);
    }

    @Test
    void disabledReceiverRejectsNewRequestsAndCancelsPreparingRequests() {
        FakeRuntime disabled = new FakeRuntime();
        disabled.receivingAllowed = false;
        execute(disabled);
        assertEquals(0, disabled.teleportCalls);
        assertEquals(List.of("message.waystonesptpt.target_receiving_disabled"), disabled.messages);
        FakeRuntime pending = new FakeRuntime();
        pending.delayed = true;
        execute(pending);
        pending.receivingAllowed = false;
        PlayerTeleportService.tickPendingRequests();
        pending.completion.complete(Optional.of(TeleportOutcome.SUCCESS));
        assertEquals(TeleportAttempt.State.CANCELLED, pending.attempt.state());
        assertEquals(0, pending.damageCalls);
    }

    @Test
    void backgroundCompletionSettlesOnlyAfterTheServerRunsItsQueuedAction() throws InterruptedException {
        FakeRuntime runtime = new FakeRuntime();
        runtime.delayed = true;
        runtime.queueCompletion = true;
        execute(runtime);
        runtime.attempt.beginCommit(runtime.tick);
        Thread completionThread = new Thread(() -> runtime.completion.complete(Optional.of(TeleportOutcome.SUCCESS)));
        completionThread.start();
        completionThread.join();
        assertEquals(0, runtime.damageCalls);
        assertEquals(0, runtime.closeCalls);
        assertEquals(1, runtime.serverActions.size());
        runtime.serverActions.remove().run();
        assertEquals(1, runtime.damageCalls);
        assertEquals(1, runtime.closeCalls);
    }

    private static void assertRejected(Optional<TeleportOutcome> outcome, String expectedMessage) {
        FakeRuntime runtime = new FakeRuntime();
        runtime.outcome = outcome;
        execute(runtime);
        assertEquals(1, runtime.teleportCalls);
        assertEquals(0, runtime.restoreRotationCalls);
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
        private final PlayerRotation rotation = new PlayerRotation(123.5f, -27.25f);
        private int tick = 100;
        private boolean valid = true;
        private boolean receivingAllowed = true;
        private boolean queueCompletion;
        private final java.util.Queue<Runnable> serverActions = new java.util.concurrent.ConcurrentLinkedQueue<>();
        private boolean sameSession = true;
        private boolean delayed;
        private final CompletableFuture<Optional<TeleportOutcome>> completion = new CompletableFuture<>();
        private TeleportAttempt attempt;
        private int teleportCalls;
        private int captureRotationCalls;
        private int restoreRotationCalls;
        private int resetFallDistanceCalls;
        private int damageCalls;
        private int closeCalls;
        private WaystonesCompat.WarpStoneUse damagedUse;
        private PlayerRotation restoredRotation;

        @Override
        public int currentTick(ServerPlayer sender) {
            return tick;
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
        public Optional<TargetPlayer> resolveOnlineTarget(ServerPlayer sender, UUID targetPlayerId) {
            return target;
        }

        @Override
        public boolean allowsReceiving(ServerPlayer target) { return receivingAllowed; }

        @Override
        public PlayerRotation captureRotation(ServerPlayer player) {
            captureRotationCalls++;
            return rotation;
        }

        @Override
        public BooleanSupplier captureRequestValidity(ServerPlayer sender, UUID targetId, WaystonesCompat.WarpStoneUse use) {
            return () -> valid && receivingAllowed;
        }

        @Override
        public boolean isSameSession(ServerPlayer sender) { return sameSession; }

        @Override
        public void executeOnServerThread(ServerPlayer sender, Runnable action) {
            if (queueCompletion) { serverActions.add(action); } else { action.run(); }
        }

        @Override
        public CompletionStage<Optional<TeleportOutcome>> tryTeleport(
                ServerPlayer sender,
                ServerPlayer target,
                WaystonesCompat.WarpStoneUse use,
                PlayerTeleportExperienceMode experienceMode,
                TeleportAttempt attempt) {
            teleportCalls++;
            this.attempt = attempt;
            if (delayed) { return completion; }
            if (outcome.filter(value -> value == TeleportOutcome.SUCCESS || value == TeleportOutcome.MOVED_INCOMPATIBLY).isPresent()) {
                attempt.beginCommit(tick);
            }
            return CompletableFuture.completedFuture(outcome);
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
                WaystonesCompat.WarpStoneUse use, int damage) {
            damageCalls++;
            damagedUse = use;
        }

        @Override
        public void resetFallDistance(ServerPlayer sender) {
            resetFallDistanceCalls++;
        }

        @Override
        public void restoreRotation(ServerPlayer sender, PlayerRotation rotation) {
            restoreRotationCalls++;
            restoredRotation = rotation;
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
    }
}
