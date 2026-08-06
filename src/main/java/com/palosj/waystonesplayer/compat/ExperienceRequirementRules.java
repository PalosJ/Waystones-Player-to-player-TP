package com.palosj.waystonesplayer.compat;

final class ExperienceRequirementRules {
    private static final String WAYSTONES_NAMESPACE_PREFIX = "waystones:";
    private static final String EXPERIENCE_POINTS_TYPE = "waystones:experience_points";
    private static final String EXPERIENCE_LEVELS_TYPE = "waystones:experience_levels";

    private ExperienceRequirementRules() {
    }

    static boolean isExperienceRequirementType(String requirementType) {
        return EXPERIENCE_POINTS_TYPE.equals(requirementType) || EXPERIENCE_LEVELS_TYPE.equals(requirementType);
    }

    static boolean shouldApply(
            String requirementType,
            String functionId,
            boolean functionEnabled,
            boolean forceWaystonesFunctions) {
        return isExperienceRequirementType(requirementType)
                && (functionEnabled || forceWaystonesFunctions && functionId.startsWith(WAYSTONES_NAMESPACE_PREFIX));
    }
}
