package com.palosj.waystonesptpt.compat;

import java.math.BigDecimal;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.RecordComponent;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.google.gson.JsonElement;
import com.mojang.datafixers.util.Either;
import com.mojang.serialization.JsonOps;

import net.blay09.mods.shogi.common.effect.compose.AggregateEffect;
import net.blay09.mods.shogi.common.parse.ShogiRuleParser;
import net.blay09.mods.shogi.effect.ShogiEffect;
import net.blay09.mods.shogi.effect.EmptyEffect;
import net.blay09.mods.shogi.context.ShogiContext;
import net.blay09.mods.waystones.config.WaystonesRules;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.RegistryOps;
import net.minecraft.server.level.ServerPlayer;

final class ShogiExperienceRuleSafety {
    private static final Pattern NUMBER = Pattern.compile(
            "(?<![A-Za-z0-9_])[-+]?(?:\\d+(?:\\.\\d*)?|\\.\\d+)(?:[eE][-+]?\\d+)?");
    private static final Set<String> XP_EFFECTS = Set.of(
            "shogi:xp_points_cost",
            "shogi:xp_level_cost");
    private static final Set<String> EXCLUDED_COST_EFFECTS = Set.of(
            "shogi:item_cost",
            "shogi:health_cost",
            "shogi:hunger_cost",
            "shogi:cooldown_cost",
            "shogi:add_cooldown");
    private static final Set<String> SAFE_SHOGI_EFFECTS = Set.of(
            "constant", "empty", "noop", "aggregate", "and", "any", "not", "if",
            "any_hand", "with_context", "off_hand",
            "has_entity_tag", "has_mob_effect", "is_on_any_vehicle", "is_on_vehicle", "is_player",
            "has_enchantment", "is_item", "has_item", "is_in_team", "lookup_team",
            "has_empty_inventory", "is_wearing_any_armor",
            "can_see_sky", "get_light_level", "is_above_y", "is_animal_nearby", "is_any_structure",
            "is_at", "is_below_y", "is_biome", "is_brighter_than", "is_block", "is_block_entity",
            "is_block_state_property", "is_darker_than", "is_dimension", "is_entity_nearby",
            "is_light_level", "is_mob_nearby", "is_near", "find_block_entity", "is_structure",
            "is_player_nearby", "is_within", "has_advancement", "is_near_poi",
            "has_cooldown", "is_cooldown_above",
            "assignment", "macro_assignment", "binary_op", "clamp_min", "clamp_max", "clamp",
            "variable", "has_value");
    private static final Set<String> SAFE_WAYSTONES_EFFECTS = Set.of(
            "source", "target", "name_equals", "name_contains", "is_dimension", "involves_dimension",
            "is_within_distance", "is_interdimensional", "is_warp_plate", "is_portstone", "is_waystone",
            "is_sharestone", "is_inventory_button", "is_scroll", "is_bound_scroll", "is_return_scroll",
            "is_warp_scroll", "is_warp_stone", "is_global", "is_owner", "has_owner", "is_with_pets",
            "is_with_passengers", "is_with_leashed", "is_ruined_sharestone", "is_copper_sharestone",
            "is_prismarine_sharestone", "is_gold_sharestone", "is_diamond_sharestone",
            "is_amethyst_sharestone", "is_lapis_sharestone", "is_emerald_sharestone",
            "is_redstone_sharestone", "is_fleeting_memorial", "is_portal_scroll", "is_warp_portal", "is_twinbound");

    private ShogiExperienceRuleSafety() {
    }

    static List<String> rulesWithSettings(net.blay09.mods.waystones.config.WaystonesConfig.Rules config) {
        List<String> rules = new ArrayList<>();
        try {
            Object settings = config.getClass().getField("warpSettings").get(config);
            if (!(settings instanceof List<?> values)) {
                throw new IllegalArgumentException("Waystones warpSettings must be a rule list");
            }
            for (Object value : values) {
                if (!(value instanceof String rule)) {
                    throw new IllegalArgumentException("Waystones warpSettings contains a non-string rule");
                }
                rules.add(rule);
            }
        } catch (NoSuchFieldException ignored) {
            // Minimum versions keep their variable definitions directly in warpRequirements.
        } catch (IllegalAccessException error) {
            throw new IllegalArgumentException("Could not read Waystones warpSettings", error);
        }
        rules.addAll(config.warpRequirements);
        return List.copyOf(rules);
    }

    /** A request-local cache; a reload invalidates the prepared costs before final evaluation. */
    static final class RuleCache {
        private final RegistryOps<JsonElement> registryOps;
        private List<String> rules;
        private boolean experienceEnabled;
        private boolean durabilityEnabled;
        private CompiledRules compiled;

        RuleCache(RegistryOps<JsonElement> registryOps) {
            this.registryOps = registryOps;
        }

        CompiledRules get(net.blay09.mods.waystones.config.WaystonesConfig.Rules config,
                com.palosj.waystonesptpt.PlayerTeleportExperienceMode mode) {
            List<String> currentRules = rulesWithSettings(config);
            boolean currentExperience = mode.shouldEvaluateWaystonesExperience(config.enableXpCosts);
            if (compiled == null || !currentRules.equals(rules)
                    || currentExperience != experienceEnabled || config.enableDurability != durabilityEnabled) {
                CompiledRules replacement = compile(registryOps, currentRules, currentExperience, config.enableDurability);
                rules = currentRules;
                experienceEnabled = currentExperience;
                durabilityEnabled = config.enableDurability;
                compiled = replacement;
            }
            return compiled;
        }
    }

    static CompiledRules compile(ServerPlayer sender, List<String> configuredRules) {
        return compile(sender, configuredRules, true, true);
    }

    static CompiledRules compile(ServerPlayer sender, List<String> configuredRules,
            boolean experienceEnabled, boolean durabilityEnabled) {
        return compile(RegistryOps.create(JsonOps.INSTANCE, sender.registryAccess()), configuredRules,
                experienceEnabled, durabilityEnabled);
    }

    static CompiledRules compile(RegistryOps<JsonElement> registryOps, List<String> configuredRules,
            boolean experienceEnabled, boolean durabilityEnabled) {
        List<ShogiEffect<?>> accepted = new ArrayList<>();
        for (String configuredRule : configuredRules) {
            if (configuredRule == null || configuredRule.isBlank()) {
                continue;
            }
            validateNumericLiterals(configuredRule);
            ShogiEffect<?> effect = parseRequiredRule(registryOps, configuredRule);
            Classification classification = classify(effect);
            if (classification.unknownEffects()) {
                throw new IllegalArgumentException("Unknown or stateful Shogi effect in Waystones XP rule: " + configuredRule);
            }
            if (classification.excludedCosts() && classification.controlledCosts()) {
                throw new IllegalArgumentException("Waystones XP rule mixes experience with another cost: " + configuredRule);
            }
            if (!classification.excludedCosts()) {
                accepted.add(effect);
            }
        }

        AggregateEffect aggregate = AggregateEffect.withAutoApplied(WaystonesRules.scope, registryOps, accepted);
        Classification aggregateClassification = classify(aggregate);
        if (aggregateClassification.unknownEffects() || aggregateClassification.excludedCosts()) {
            throw new IllegalArgumentException("Compiled Waystones XP rules contain an unsupported terminal effect");
        }
        DamageTotal damage = new DamageTotal();
        return new CompiledRules((AggregateEffect) protectCosts(aggregate, damage,
                experienceEnabled, durabilityEnabled), damage);
    }

    @SuppressWarnings("unchecked")
    private static ShogiEffect<?> parseRequiredRule(
            RegistryOps<JsonElement> registryOps,
            String configuredRule) {
        var parsed = ShogiRuleParser.parse(WaystonesRules.scope, registryOps, configuredRule);
        return parsed.result().orElseThrow(() -> new IllegalArgumentException(
                "Could not parse configured Waystones Shogi rule: " + configuredRule));
    }

    private static Classification classify(ShogiEffect<?> root) {
        boolean xp = false;
        boolean excluded = false;
        boolean unknown = false;
        Set<ShogiEffect<?>> visited = java.util.Collections.newSetFromMap(new java.util.IdentityHashMap<>());
        List<ShogiEffect<?>> pending = new ArrayList<>();
        pending.add(root);
        while (!pending.isEmpty()) {
            ShogiEffect<?> effect = pending.removeLast();
            if (!visited.add(effect)) {
                continue;
            }
            String identifier = effect.identifier().toString();
            if (isExperienceEffect(effect.identifier()) || identifier.equals("shogi:damage_item")) {
                xp = true;
            } else if (isExcludedCostEffect(effect.identifier())) {
                excluded = true;
            } else if (!identifier.equals("shogi:damage_item") && !isKnownPureEffect(effect.identifier())) {
                throw new IllegalArgumentException("Unsupported Shogi effect: " + identifier
                        + " (" + effect.getClass().getName() + ")");
            }
            pending.addAll(nestedEffects(effect));
        }
        return new Classification(xp, excluded, unknown);
    }

    private static List<ShogiEffect<?>> nestedEffects(ShogiEffect<?> effect) {
        try {
            var method = effect.getClass().getMethod("nestedEffects");
            Object value = method.invoke(effect);
            return collectEffects(value);
        } catch (NoSuchMethodException ignored) {
            // Early 26.1 Shogi predates ShogiEffect.nestedEffects; its composite effects are records.
        } catch (IllegalAccessException | InvocationTargetException error) {
            throw new IllegalArgumentException("Could not inspect a Shogi effect tree", error);
        }

        List<ShogiEffect<?>> nested = new ArrayList<>();
        if (effect.getClass().isRecord()) {
            for (RecordComponent component : effect.getClass().getRecordComponents()) {
                try {
                    nested.addAll(collectEffects(component.getAccessor().invoke(effect)));
                } catch (IllegalAccessException | InvocationTargetException error) {
                    throw new IllegalArgumentException("Could not inspect a Shogi record effect", error);
                }
            }
        }
        return nested;
    }

    private static List<ShogiEffect<?>> collectEffects(Object value) {
        List<ShogiEffect<?>> effects = new ArrayList<>();
        if (value instanceof ShogiEffect<?> effect) {
            effects.add(effect);
        } else if (value instanceof Iterable<?> values) {
            for (Object candidate : values) {
                if (candidate instanceof ShogiEffect<?> effect) {
                    effects.add(effect);
                }
            }
        }
        return effects;
    }

    static boolean isExperienceEffect(Identifier identifier) {
        return XP_EFFECTS.contains(identifier.toString());
    }

    static boolean isExcludedCostEffect(Identifier identifier) {
        return EXCLUDED_COST_EFFECTS.contains(identifier.toString());
    }

    static boolean isKnownPureEffect(Identifier identifier) {
        String namespace = identifier.getNamespace();
        String path = identifier.getPath();
        if ("shogi".equals(namespace)) {
            return SAFE_SHOGI_EFFECTS.contains(path);
        }
        if ("waystones".equals(namespace)) {
            return SAFE_WAYSTONES_EFFECTS.contains(path);
        }
        return false;
    }

    static void validateNumericLiterals(String rule) {
        String unquoted = stripQuotedText(rule);
        String normalized = unquoted.toLowerCase(Locale.ROOT);
        if (normalized.contains("nan") || normalized.contains("infinity")) {
            throw new IllegalArgumentException("Waystones XP rules must not contain non-finite numbers");
        }
        Matcher matcher = NUMBER.matcher(unquoted);
        while (matcher.find()) {
            BigDecimal value;
            try {
                value = new BigDecimal(matcher.group());
            } catch (NumberFormatException error) {
                throw new IllegalArgumentException("Invalid numeric literal in Waystones XP rule", error);
            }
            if (value.abs().compareTo(BigDecimal.valueOf(Double.MAX_VALUE)) > 0) {
                throw new IllegalArgumentException("Waystones XP rule number is outside the supported range: " + value);
            }
        }
    }

    private static String stripQuotedText(String input) {
        StringBuilder result = new StringBuilder(input.length());
        char quote = 0;
        boolean escaped = false;
        for (int index = 0; index < input.length(); index++) {
            char character = input.charAt(index);
            if (quote != 0) {
                result.append(' ');
                if (escaped) {
                    escaped = false;
                } else if (character == '\\') {
                    escaped = true;
                } else if (character == quote) {
                    quote = 0;
                }
            } else if (character == '\'' || character == '"') {
                quote = character;
                result.append(' ');
            } else {
                result.append(character);
            }
        }
        return result.toString();
    }

    record CompiledRules(AggregateEffect aggregate, DamageTotal damage) {
        Either<List<Object>, List<Object>> evaluate(
                LockedWaystoneTeleportContext context,
                boolean creative) {
            context.resetExecutor(creative);
            damage.value = 0;
            damage.expectedStack = context.getWarpItem();
            Either<? extends List<Object>, ?> result = aggregate.apply(context);
            if (result.left().isPresent()) {
                return Either.left(List.copyOf(result.left().orElseThrow()));
            }
            Object failure = result.right().orElseThrow();
            List<Object> failures = normalizeFailures(failure);
            if (!failures.stream().allMatch(CompiledRules::isExperienceFailure)) {
                throw new IllegalArgumentException("Waystones Shogi rule returned an unknown failure: " + failure);
            }
            return creative ? Either.left(List.of()) : Either.right(failures);
        }

        private static List<Object> normalizeFailures(Object failure) {
            if (failure instanceof List<?> list) {
                return List.copyOf(list);
            }
            return List.of(failure);
        }

        private static boolean isExperienceFailure(Object failure) {
            if (failure == null || failure instanceof Throwable) {
                return false;
            }
            String name = failure.getClass().getName();
            return name.equals("net.blay09.mods.shogi.common.effect.cost.ExperiencePointsCostInformation")
                    || name.equals("net.blay09.mods.shogi.common.effect.cost.ExperienceLevelCostInformation");
        }
    }

    // Rebuild only the admitted upstream record tree, after auto-applied costs have been added.
    // Wrapping cost arguments preserves predicates and arithmetic while validating before coercion.
    private static ShogiEffect<?> protectCosts(ShogiEffect<?> effect, DamageTotal damage,
            boolean experienceEnabled, boolean durabilityEnabled) {
        String id = effect.identifier().toString();
        boolean xp = isExperienceEffect(effect.identifier());
        boolean itemDamage = id.equals("shogi:damage_item");
        if ((xp && !experienceEnabled) || (itemDamage && !durabilityEnabled)) {
            return EmptyEffect.INSTANCE;
        }
        if (!effect.getClass().isRecord()) {
            if (!nestedEffects(effect).isEmpty() || xp || itemDamage) {
                throw new IllegalArgumentException("Unsupported composite Shogi implementation: " + id);
            }
            return effect;
        }
        try {
            RecordComponent[] components = effect.getClass().getRecordComponents();
            Object[] arguments = new Object[components.length];
            Class<?>[] types = new Class<?>[components.length];
            for (int index = 0; index < components.length; index++) {
                types[index] = components[index].getType();
                Object value = components[index].getAccessor().invoke(effect);
                if (value instanceof ShogiEffect<?> child) {
                    ShogiEffect<?> protectedChild = protectCosts(child, damage, experienceEnabled, durabilityEnabled);
                    arguments[index] = xp || itemDamage ? new CheckedAmount(protectedChild) : protectedChild;
                } else if (value instanceof List<?> list) {
                    arguments[index] = list.stream().map(child -> child instanceof ShogiEffect<?> nested
                            ? protectCosts(nested, damage, experienceEnabled, durabilityEnabled) : child).toList();
                } else {
                    arguments[index] = value;
                }
            }
            if (itemDamage) {
                if (arguments.length != 1 || !(arguments[0] instanceof ShogiEffect<?> amount)) {
                    throw new IllegalArgumentException("Unsupported damage_item arguments");
                }
                return new DeferredDamage(amount, damage);
            }
            return (ShogiEffect<?>) effect.getClass().getDeclaredConstructor(types).newInstance(arguments);
        } catch (ReflectiveOperationException error) {
            throw new IllegalArgumentException("Could not protect Shogi cost tree: " + id, error);
        }
    }

    static int checkedAmount(Object value) {
        if (value instanceof com.google.gson.JsonPrimitive primitive) {
            if (primitive.isNumber()) {
                value = primitive.getAsNumber();
            } else if (primitive.isString()) {
                value = primitive.getAsString();
            } else if (primitive.isBoolean()) {
                return primitive.getAsBoolean() ? 1 : 0;
            }
        }
        if (value instanceof String text) {
            // The native integer coercion accepts integer strings, but cannot validate before narrowing.
            try {
                int cost = Integer.parseInt(text);
                if (cost < 0) { throw new IllegalArgumentException("Negative Shogi cost"); }
                return cost;
            } catch (NumberFormatException error) {
                throw new IllegalArgumentException("Invalid integer Shogi cost", error);
            }
        }
        if (!(value instanceof Number number)) {
            throw new IllegalArgumentException("A Shogi cost must evaluate to a number, got "
                    + (value == null ? "null" : value.getClass().getName()));
        }
        double cost = number.doubleValue();
        if (!Double.isFinite(cost) || cost < 0 || cost > Integer.MAX_VALUE) {
            throw new IllegalArgumentException("A Shogi cost must be finite, non-negative and within integer range");
        }
        // Upstream NON_NEGATIVE_INT truncates fractional costs; preserve that behavior.
        return (int) cost;
    }

    private record CheckedAmount(ShogiEffect<?> delegate) implements ShogiEffect<Integer> {
        public Identifier identifier() { return delegate.identifier(); }
        public Either<Integer, ?> apply(ShogiContext context) {
            return delegate.apply(context).mapLeft(ShogiExperienceRuleSafety::checkedAmount);
        }
    }

    private record DeferredDamage(ShogiEffect<?> amount, DamageTotal total) implements ShogiEffect<Boolean> {
        public Identifier identifier() { return Identifier.fromNamespaceAndPath("shogi", "damage_item"); }
        public Either<Boolean, ?> apply(ShogiContext context) {
            return amount.apply(context).mapLeft(value -> {
                if (context.itemStack() != total.expectedStack) {
                    throw new IllegalArgumentException("damage_item changed the bound Warp Stone context");
                }
                int damage = checkedAmount(value);
                if (damage == 0 || context.itemStack().isEmpty() || !context.itemStack().isDamageableItem()) {
                    return false;
                }
                total.value = Math.addExact(total.value, damage);
                return true;
            });
        }
    }

    static final class DamageTotal {
        private int value;
        private net.minecraft.world.item.ItemStack expectedStack;
        int value() { return value; }
    }

    private record Classification(
            boolean controlledCosts,
            boolean excludedCosts,
            boolean unknownEffects) {
    }
}
