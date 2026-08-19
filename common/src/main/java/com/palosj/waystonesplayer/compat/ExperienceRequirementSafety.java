package com.palosj.waystonesplayer.compat;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

import com.palosj.waystonesplayer.compat.ExperienceCostSafety;

import net.blay09.mods.waystones.api.requirement.WarpRequirement;
import net.blay09.mods.waystones.requirement.CombinedRequirement;
import net.blay09.mods.waystones.requirement.ConfiguredCondition;
import net.blay09.mods.waystones.requirement.ConfiguredRequirementModifier;
import net.blay09.mods.waystones.requirement.ExperienceLevelRequirement;
import net.blay09.mods.waystones.requirement.ExperiencePointsRequirement;
import net.blay09.mods.waystones.requirement.NoRequirement;
import net.blay09.mods.waystones.requirement.RequirementModifierParser;
import net.blay09.mods.waystones.requirement.RequirementRegistry;
import net.blay09.mods.waystones.requirement.WarpRequirementsContextImpl;

final class ExperienceRequirementSafety {
    private static final String POINTS_TYPE = "waystones:experience_points";
    private static final String LEVELS_TYPE = "waystones:experience_levels";
    private static final String WAYSTONES_PREFIX = "waystones:";

    private ExperienceRequirementSafety() {
    }

    static List<? extends ConfiguredRequirementModifier<?, ?>> parseRequiredRule(String configuredRule) {
        return normalizeParsedRule(configuredRule, RequirementModifierParser.parse(configuredRule));
    }

    static List<? extends ConfiguredRequirementModifier<?, ?>> normalizeParsedRule(
            String configuredRule,
            Object parsed) {
        List<ConfiguredRequirementModifier<?, ?>> modifiers = new ArrayList<>();
        if (parsed instanceof Optional<?> optional) {
            if (optional.isEmpty()
                    || !(optional.orElseThrow() instanceof ConfiguredRequirementModifier<?, ?> modifier)) {
                throw invalidParsedRule(configuredRule);
            }
            modifiers.add(modifier);
        } else if (parsed instanceof Iterable<?> candidates) {
            for (Object candidate : candidates) {
                if (!(candidate instanceof ConfiguredRequirementModifier<?, ?> modifier)) {
                    throw invalidParsedRule(configuredRule);
                }
                modifiers.add(modifier);
            }
        } else {
            throw invalidParsedRule(configuredRule);
        }
        if (modifiers.isEmpty()) {
            throw invalidParsedRule(configuredRule);
        }
        return List.copyOf(modifiers);
    }

    private static IllegalArgumentException invalidParsedRule(String configuredRule) {
        return new IllegalArgumentException(
                "Configured Waystones requirement produced an empty or unknown result shape: "
                        + configuredRule);
    }

    static int expectedCost(
            String requirementType,
            String functionId,
            int current,
            Object parameters,
            float contextValue) {
        validateModifierIdentity(requirementType, functionId);

        String path = functionId.substring(WAYSTONES_PREFIX.length());
        String label = requirementType.equals(POINTS_TYPE) ? "experience points" : "experience levels";
        if (requirementType.equals(LEVELS_TYPE)) {
            return switch (path) {
                case "add_level_cost" -> ExperienceCostSafety.checkedAdd(
                        current, floatValue(parameters), label);
                case "multiply_level_cost" -> ExperienceCostSafety.checkedMultiply(
                        current, floatValue(parameters), label);
                case "scaled_add_level_cost" -> ExperienceCostSafety.checkedScaledAdd(
                        current, contextValue(parameters, contextValue), scaleValue(parameters), label);
                case "scaled_multiply_level_cost" -> ExperienceCostSafety.checkedScaledMultiply(
                        current, contextValue(parameters, contextValue), scaleValue(parameters), label);
                case "min_level_cost" -> ExperienceCostSafety.checkedMinimum(
                        current, intValue(parameters), label);
                case "max_level_cost" -> ExperienceCostSafety.checkedMaximum(
                        current, intValue(parameters), label);
                default -> throw new IllegalArgumentException("Unsupported Waystones level modifier: " + functionId);
            };
        }
        if (requirementType.equals(POINTS_TYPE)) {
            return switch (path) {
                case "add_xp_cost" -> ExperienceCostSafety.checkedAdd(
                        current, intValue(parameters), label);
                case "multiply_xp_cost" -> ExperienceCostSafety.checkedMultiply(
                        current, floatValue(parameters), label);
                case "scaled_add_xp_cost" -> ExperienceCostSafety.checkedScaledAdd(
                        current, contextValue(parameters, contextValue), scaleValue(parameters), label);
                // 21.1.x contains a historical duplicate ID for this XP modifier.
                case "scaled_multiply_xp_cost", "scaled_multiply_level_cost" ->
                        ExperienceCostSafety.checkedScaledMultiply(
                                current, contextValue(parameters, contextValue), scaleValue(parameters), label);
                case "min_xp_cost" -> ExperienceCostSafety.checkedMinimum(
                        current, intValue(parameters), label);
                case "max_xp_cost" -> ExperienceCostSafety.checkedMaximum(
                        current, intValue(parameters), label);
                default -> throw new IllegalArgumentException("Unsupported Waystones XP modifier: " + functionId);
            };
        }
        throw new IllegalArgumentException("Unsupported experience requirement type: " + requirementType);
    }

    static void validateModifierIdentity(String requirementType, String functionId) {
        if (!ExperienceRequirementRules.isExperienceRequirementType(requirementType)) {
            return;
        }
        if (functionId == null || !functionId.startsWith(WAYSTONES_PREFIX)) {
            throw new IllegalArgumentException("Unsupported third-party experience modifier: " + functionId);
        }

        String path = functionId.substring(WAYSTONES_PREFIX.length());
        boolean supported = requirementType.equals(LEVELS_TYPE)
                ? switch (path) {
                    case "add_level_cost", "multiply_level_cost", "scaled_add_level_cost",
                            "scaled_multiply_level_cost", "min_level_cost", "max_level_cost" -> true;
                    default -> false;
                }
                : switch (path) {
                    case "add_xp_cost", "multiply_xp_cost", "scaled_add_xp_cost",
                            "scaled_multiply_xp_cost", "scaled_multiply_level_cost",
                            "min_xp_cost", "max_xp_cost" -> true;
                    default -> false;
                };
        if (!supported) {
            throw new IllegalArgumentException("Unsupported Waystones experience modifier: " + functionId);
        }
    }

    static int validateBeforeApply(
            WarpRequirementsContextImpl context,
            ConfiguredRequirementModifier<?, ?> modifier) {
        var configured = modifier.requirement();
        var function = configured.modifier();
        String requirementType = function.getRequirementType().toString();
        String functionId = function.getId().toString();
        validateRequirementTree(context.resolve());
        int current = costFor(context.resolve(), requirementType);

        float contextValue = 0;
        Object parameters = configured.parameters();
        if (parameters instanceof RequirementRegistry.VariableScaledParameter scaled) {
            contextValue = context.getContextValue(scaled.id().value());
        }
        int expected = expectedCost(requirementType, functionId, current, parameters, contextValue);
        if (!matchesAll(context, modifier.conditions())) {
            return current;
        }
        return expected;
    }

    static void validateAfterApply(WarpRequirementsContextImpl context, String requirementType, int expected) {
        WarpRequirement result = context.resolve();
        validateRequirementTree(result);
        int actual = costFor(result, requirementType);
        if (actual != expected) {
            throw new IllegalArgumentException(
                    "Waystones experience modifier produced an unexpected cost: " + actual + " != " + expected);
        }
    }

    static void validateRequirementTree(WarpRequirement requirement) {
        int[] counts = new int[2];
        validateRequirementNode(requirement, counts);
        if (counts[0] > 1 || counts[1] > 1) {
            throw new IllegalArgumentException("Experience requirement result contains duplicate cost types");
        }
    }

    private static void validateRequirementNode(WarpRequirement requirement, int[] counts) {
        if (requirement == null) {
            throw new IllegalArgumentException("Waystones returned a null experience requirement");
        }
        if (requirement == NoRequirement.INSTANCE) {
            return;
        }
        if (requirement instanceof ExperiencePointsRequirement points) {
            ExperienceCostSafety.requireNonNegative(points.getPoints(), "experience points");
            counts[0]++;
            return;
        }
        if (requirement instanceof ExperienceLevelRequirement levels) {
            ExperienceCostSafety.requireNonNegative(levels.getLevels(), "experience levels");
            counts[1]++;
            return;
        }
        if (requirement instanceof CombinedRequirement combined) {
            Collection<WarpRequirement> children = combined.getRequirements();
            if (children == null || children.isEmpty()) {
                throw new IllegalArgumentException("Waystones returned an empty combined experience requirement");
            }
            for (WarpRequirement child : children) {
                validateRequirementNode(child, counts);
            }
            return;
        }
        throw new IllegalArgumentException(
                "Unsupported experience requirement result: " + requirement.getClass().getName());
    }

    private static int costFor(WarpRequirement requirement, String requirementType) {
        if (requirement == NoRequirement.INSTANCE) {
            return 0;
        }
        if (requirementType.equals(POINTS_TYPE) && requirement instanceof ExperiencePointsRequirement points) {
            return ExperienceCostSafety.requireNonNegative(points.getPoints(), "experience points");
        }
        if (requirementType.equals(LEVELS_TYPE) && requirement instanceof ExperienceLevelRequirement levels) {
            return ExperienceCostSafety.requireNonNegative(levels.getLevels(), "experience levels");
        }
        if (requirement instanceof CombinedRequirement combined) {
            List<Integer> matches = new ArrayList<>();
            for (WarpRequirement child : combined.getRequirements()) {
                if (requirementType.equals(POINTS_TYPE) && child instanceof ExperiencePointsRequirement points) {
                    matches.add(points.getPoints());
                } else if (requirementType.equals(LEVELS_TYPE)
                        && child instanceof ExperienceLevelRequirement levels) {
                    matches.add(levels.getLevels());
                }
            }
            if (matches.size() == 1) {
                return ExperienceCostSafety.requireNonNegative(matches.getFirst(), requirementType);
            }
        }
        throw new IllegalArgumentException("Could not resolve cost for requirement type " + requirementType);
    }

    private static boolean matchesAll(
            WarpRequirementsContextImpl context,
            List<ConfiguredCondition<?>> conditions) {
        for (ConfiguredCondition<?> condition : conditions) {
            if (!matches(context, condition)) {
                return false;
            }
        }
        return true;
    }

    private static <P> boolean matches(WarpRequirementsContextImpl context, ConfiguredCondition<P> condition) {
        return context.matchesCondition(condition);
    }

    private static int intValue(Object parameters) {
        if (parameters instanceof RequirementRegistry.IntParameter value) {
            return ExperienceCostSafety.requireNonNegative(value.value(), "experience modifier parameter");
        }
        throw new IllegalArgumentException("Expected an integer experience modifier parameter");
    }

    private static float floatValue(Object parameters) {
        if (parameters instanceof RequirementRegistry.FloatParameter value) {
            return ExperienceCostSafety.requireFiniteNonNegative(value.value(), "experience modifier parameter");
        }
        throw new IllegalArgumentException("Expected a floating-point experience modifier parameter");
    }

    private static float scaleValue(Object parameters) {
        if (parameters instanceof RequirementRegistry.VariableScaledParameter value) {
            return ExperienceCostSafety.requireFiniteNonNegative(
                    value.scale().value(), "experience modifier scale");
        }
        throw new IllegalArgumentException("Expected a scaled experience modifier parameter");
    }

    private static float contextValue(Object parameters, float value) {
        if (!(parameters instanceof RequirementRegistry.VariableScaledParameter)) {
            throw new IllegalArgumentException("Expected a scaled experience modifier parameter");
        }
        return ExperienceCostSafety.requireFiniteNonNegative(value, "experience modifier context value");
    }
}
