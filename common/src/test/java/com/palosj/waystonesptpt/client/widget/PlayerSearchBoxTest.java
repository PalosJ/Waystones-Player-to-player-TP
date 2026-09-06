package com.palosj.waystonesptpt.client.widget;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;
import org.lwjgl.glfw.GLFW;

class PlayerSearchBoxTest {
    @Test
    void typingAndEnterStayInTheSearchWhileTabAndEscapeBelongToTheScreen() throws ReflectiveOperationException {
        PlayerSearchBox search = new PlayerSearchBox(null, 0, 0, 110);
        assertFalse(WidgetTestCompat.keyPressed(search, GLFW.GLFW_KEY_E));
        WidgetTestCompat.focus(search, true);
        assertTrue(WidgetTestCompat.keyPressed(search, GLFW.GLFW_KEY_E));
        assertTrue(WidgetTestCompat.keyPressed(search, GLFW.GLFW_KEY_1));
        assertTrue(WidgetTestCompat.keyPressed(search, GLFW.GLFW_KEY_ENTER));
        assertTrue(WidgetTestCompat.keyPressed(search, GLFW.GLFW_KEY_KP_ENTER));
        assertFalse(WidgetTestCompat.keyPressed(search, GLFW.GLFW_KEY_TAB));
        assertFalse(WidgetTestCompat.keyPressed(search, GLFW.GLFW_KEY_ESCAPE));
        assertTrue(WidgetTestCompat.charTyped(search, 'e'));
        assertEquals("e", search.getValue());
        WidgetTestCompat.focus(search, false);
        assertFalse(WidgetTestCompat.charTyped(search, 'x'));
    }
}
