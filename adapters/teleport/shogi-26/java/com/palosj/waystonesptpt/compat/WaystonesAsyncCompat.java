package com.palosj.waystonesptpt.compat;

import java.util.concurrent.CompletableFuture;
import net.blay09.mods.waystones.api.WaystoneTeleportContext;
import net.minecraft.server.MinecraftServer;

/** Keeps only this add-on's preparation continuations on the owning server thread. */
public final class WaystonesAsyncCompat {
    private WaystonesAsyncCompat() { }

    public static boolean isPlayerTeleport(WaystoneTeleportContext context) {
        return context instanceof LockedWaystoneTeleportContext;
    }

    public static <T> CompletableFuture<T> onServerThread(
            WaystoneTeleportContext context, CompletableFuture<T> source) {
        if (!isPlayerTeleport(context)) {
            return source;
        }
        MinecraftServer server = context.getEntity().level().getServer();
        CompletableFuture<T> result = new CompletableFuture<>();
        source.whenComplete((value, error) -> server.execute(() -> {
            if (error == null) {
                result.complete(value);
            } else {
                result.completeExceptionally(error);
            }
        }));
        return result;
    }
}
