package com.palosj.waystonesptpt.compat;

/** Marker propagated only through Shogi contexts rooted in this add-on's guarded teleport. */
public interface PlayerTeleportShogiContext {
    boolean waystonesptpt$isPlayerTeleport();
}
