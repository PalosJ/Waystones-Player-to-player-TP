package com.palosj.waystonesplayer.compat;

import java.util.HashMap;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

import net.blay09.mods.shogi.context.executor.DeferredEffectExecutor;
import net.blay09.mods.shogi.context.executor.aggregate.AggregateKey;
import net.minecraft.resources.Identifier;

final class LockedShogiExecutor implements DeferredEffectExecutor {
    private static final Identifier AGGREGATE_EFFECT = id("aggregate");
    private static final Identifier XP_POINTS = id("xp_points_cost");
    private static final Identifier XP_LEVELS = id("xp_level_cost");

    private final Map<AggregateKey<?>, Object> aggregates = new HashMap<>();
    private final Map<AggregateKey<?>, Consumer<?>> consumers = new HashMap<>();
    private final Map<Identifier, Runnable> runnables = new HashMap<>();
    private final Runnable beforeExecute;
    private final Runnable overrideAttempted;
    private boolean creative;
    private boolean executing;
    private boolean executed;

    LockedShogiExecutor(Runnable beforeExecute, Runnable overrideAttempted) {
        this.beforeExecute = beforeExecute;
        this.overrideAttempted = overrideAttempted;
    }

    void reset(boolean creative) {
        aggregates.clear();
        consumers.clear();
        runnables.clear();
        this.creative = creative;
        executing = false;
        executed = false;
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T aggregate(AggregateKey<T> key, Supplier<T> initializer, Function<T, T> aggregator) {
        requireExperienceKey(key);
        T current = (T) aggregates.get(key);
        if (current == null) {
            current = initializer.get();
        }
        requireCost(current, "initial Shogi aggregate");
        T updated = aggregator.apply(current);
        validateAggregateProgress(current, updated);
        aggregates.put(key, updated);
        return updated;
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T, R> R statefulAggregate(AggregateKey<T> key, Supplier<T> initializer, Function<T, R> aggregator) {
        requireExperienceKey(key);
        T current = (T) aggregates.computeIfAbsent(key, ignored -> initializer.get());
        requireCost(current, "stateful Shogi aggregate");
        return aggregator.apply(current);
    }

    @Override
    public <T> void consume(AggregateKey<T> key, Consumer<T> consumer) {
        requireExperienceKey(key);
        consumers.put(key, consumer);
    }

    @Override
    public void execute(Identifier identifier, Runnable runnable) {
        if (!AGGREGATE_EFFECT.equals(identifier)) {
            throw new IllegalArgumentException("Unsupported Shogi side effect: " + identifier);
        }
        runnables.put(identifier, runnable);
    }

    public <T, R> void overrideAggregate(
            Identifier identifier,
            BiFunction<Function<T, R>, T, R> override) {
        overrideAttempted.run();
    }

    @Override
    public <T> void overrideConsume(
            Identifier identifier,
            BiConsumer<Consumer<T>, T> override) {
        overrideAttempted.run();
    }

    @Override
    public void overrideExecute(Identifier identifier, Consumer<Runnable> override) {
        overrideAttempted.run();
    }

    @Override
    public void execute() {
        if (executing || executed) {
            return;
        }
        beforeExecute.run();
        executing = true;
        try {
            if (!creative) {
                for (Map.Entry<AggregateKey<?>, Consumer<?>> entry : consumers.entrySet()) {
                    executeConsumer(entry.getKey(), entry.getValue());
                }
            }
            for (Runnable runnable : runnables.values()) {
                runnable.run();
            }
            executed = true;
        } finally {
            executing = false;
        }
    }

    @SuppressWarnings("unchecked")
    private <T> void executeConsumer(AggregateKey<?> key, Consumer<?> consumer) {
        Object value = aggregates.get(key);
        requireCost(value, "consumed Shogi aggregate");
        ((Consumer<T>) consumer).accept((T) value);
    }

    private static void requireExperienceKey(AggregateKey<?> key) {
        Identifier identifier = key.identifier();
        if (!XP_POINTS.equals(identifier) && !XP_LEVELS.equals(identifier)) {
            throw new IllegalArgumentException("Unsupported Shogi aggregate: " + identifier);
        }
    }

    static void validateAggregateProgress(Object current, Object updated) {
        requireCost(current, "initial Shogi aggregate");
        requireCost(updated, "updated Shogi aggregate");
        if (((Integer) updated) < ((Integer) current)) {
            throw new IllegalArgumentException("Shogi experience aggregation overflowed");
        }
    }

    static void requireCost(Object value, String label) {
        if (!(value instanceof Integer cost) || cost < 0) {
            throw new IllegalArgumentException(label + " must be a finite non-negative integer");
        }
    }

    private static Identifier id(String path) {
        return Identifier.fromNamespaceAndPath("shogi", path);
    }
}
