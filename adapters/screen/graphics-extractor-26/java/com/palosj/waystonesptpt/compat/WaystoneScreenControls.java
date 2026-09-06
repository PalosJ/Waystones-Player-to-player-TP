package com.palosj.waystonesptpt.compat;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;

/** Explicit Waystones-owned vanilla controls; never infer ownership from screen coordinates. */
public final class WaystoneScreenControls {
    private static final String BASE = "net.blay09.mods.waystones.client.gui.screen.WaystoneSelectionScreenBase";
    private static final ClassValue<List<Field>> FIELDS = new ClassValue<>() {
        protected List<Field> computeValue(Class<?> screenType) {
            for (Class<?> type = screenType; type != null; type = type.getSuperclass()) {
                if (type.getName().equals(BASE)) {
                    List<Field> fields = new ArrayList<>();
                    for (String name : List.of("searchBox", "btnPrevPage", "btnNextPage")) {
                        try {
                            Field field = type.getDeclaredField(name);
                            field.setAccessible(true);
                            fields.add(field);
                        } catch (NoSuchFieldException ignored) {
                            // Pagination controls do not exist in the scrolling-list API family.
                        }
                    }
                    return List.copyOf(fields);
                }
            }
            return List.of();
        }
    };

    private WaystoneScreenControls() { }

    public static List<AbstractWidget> ownedControls(Screen screen) {
        List<AbstractWidget> result = new ArrayList<>();
        for (Field field : FIELDS.get(screen.getClass())) {
            try {
                if (field.get(screen) instanceof AbstractWidget widget) {
                    result.add(widget);
                }
            } catch (IllegalAccessException error) {
                throw new IllegalStateException("Could not read Waystones control " + field.getName(), error);
            }
        }
        return result;
    }

    public static EditBox searchBox(Screen screen) {
        return ownedControls(screen).stream().filter(EditBox.class::isInstance)
                .map(EditBox.class::cast).findFirst().orElse(null);
    }

    public static boolean hasPagination(Screen screen) {
        return FIELDS.get(screen.getClass()).stream().anyMatch(field -> field.getName().equals("btnPrevPage"));
    }
}
