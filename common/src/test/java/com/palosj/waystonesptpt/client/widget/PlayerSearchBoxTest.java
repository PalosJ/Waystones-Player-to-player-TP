package com.palosj.waystonesptpt.client.widget;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;
import org.lwjgl.glfw.GLFW;

class PlayerSearchBoxTest {
    @Test
    void typingAndEnterStayInTheSearchWhileTabAndEscapeBelongToTheScreen() {
        PlayerSearchBox search = new PlayerSearchBox(null, 0, 0, 110);
        assertFalse(search.keyPressed(GLFW.GLFW_KEY_E, 0, 0));
        search.setFocused(true);
        assertTrue(search.keyPressed(GLFW.GLFW_KEY_E, 0, 0));
        assertTrue(search.keyPressed(GLFW.GLFW_KEY_1, 0, 0));
        assertTrue(search.keyPressed(GLFW.GLFW_KEY_ENTER, 0, 0));
        assertTrue(search.keyPressed(GLFW.GLFW_KEY_KP_ENTER, 0, 0));
        assertFalse(search.keyPressed(GLFW.GLFW_KEY_TAB, 0, 0));
        assertFalse(search.keyPressed(GLFW.GLFW_KEY_ESCAPE, 0, 0));
        assertTrue(search.charTyped('e', 0));
        assertEquals("e", search.getValue());
        search.setFocused(false);
        assertFalse(search.charTyped('x', 0));
    }
}
