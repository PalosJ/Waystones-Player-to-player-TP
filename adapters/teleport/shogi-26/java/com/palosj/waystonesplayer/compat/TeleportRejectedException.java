package com.palosj.waystonesplayer.compat;

final class TeleportRejectedException extends RuntimeException {
    TeleportRejectedException(String message) {
        super(message);
    }

    TeleportRejectedException(String message, Throwable cause) {
        super(message, cause);
    }
}
