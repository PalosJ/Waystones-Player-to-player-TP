package com.palosj.waystonesptpt.client.widget;

import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.AbstractWidget;

/** Test-only dispatch to the native input and scrolling APIs of the selected Minecraft target. */
final class WidgetTestCompat {
    private WidgetTestCompat() {
    }

    static void focus(EditBox box, boolean focused) throws ReflectiveOperationException {
        // There is no platform text-input owner in this headless unit test.
        // Set only the widget focus flag; native key/character handling still runs below.
        var field = AbstractWidget.class.getDeclaredField("focused");
        field.setAccessible(true);
        field.setBoolean(box, focused);
    }

    static boolean keyPressed(EditBox box, int key) throws ReflectiveOperationException {
        try {
            return (boolean) box.getClass().getMethod("keyPressed", int.class, int.class, int.class)
                    .invoke(box, key, 0, 0);
        } catch (NoSuchMethodException oldInputUnavailable) {
            Class<?> event = Class.forName("net.minecraft.client.input.KeyEvent");
            return (boolean) box.getClass().getMethod("keyPressed", event)
                    .invoke(box, event.getConstructor(int.class, int.class, int.class).newInstance(key, 0, 0));
        }
    }

    static boolean charTyped(EditBox box, char character) throws ReflectiveOperationException {
        try {
            return (boolean) box.getClass().getMethod("charTyped", char.class, int.class).invoke(box, character, 0);
        } catch (NoSuchMethodException oldInputUnavailable) {
            Class<?> event = Class.forName("net.minecraft.client.input.CharacterEvent");
            Object value;
            try {
                value = event.getConstructor(int.class, int.class).newInstance((int) character, 0);
            } catch (NoSuchMethodException modifiersUnavailable) {
                value = event.getConstructor(int.class).newInstance((int) character);
            }
            return (boolean) box.getClass().getMethod("charTyped", event).invoke(box, value);
        }
    }

    static double scrollAmount(PlayerDestinationList list) throws ReflectiveOperationException {
        try {
            return (double) list.getClass().getMethod("getScrollAmount").invoke(list);
        } catch (NoSuchMethodException oldScrollUnavailable) {
            return (double) list.getClass().getMethod("scrollAmount").invoke(list);
        }
    }
}
