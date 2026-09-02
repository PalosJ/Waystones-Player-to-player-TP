package com.palosj.waystonesptpt;

public enum PlayerTeleportExperienceMode {
    NEVER,
    FOLLOW_WAYSTONES,
    ALWAYS;

    public boolean shouldEvaluateWaystonesExperience(boolean waystonesCostsEnabled) {
        return this == ALWAYS || this == FOLLOW_WAYSTONES && waystonesCostsEnabled;
    }
}
