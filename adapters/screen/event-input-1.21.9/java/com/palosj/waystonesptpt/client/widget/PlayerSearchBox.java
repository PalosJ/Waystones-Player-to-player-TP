package com.palosj.waystonesptpt.client.widget;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.network.chat.Component;
import org.lwjgl.glfw.GLFW;
import net.minecraft.client.input.KeyEvent;

public final class PlayerSearchBox extends EditBox {
    public PlayerSearchBox(Font font, int x, int y, int width) {
        super(font, x, y, width, PlayerToolbarLayout.BUTTON_SIZE,
                Component.translatable("gui.waystonesptpt.search_players"));
        setHint(getMessage());
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        if (!canConsumeInput() || event.key() == GLFW.GLFW_KEY_ESCAPE || event.key() == GLFW.GLFW_KEY_TAB) {
            return false;
        }
        super.keyPressed(event);
        // Printable characters arrive separately in charTyped. Consume their key events
        // so inventory/hotbar shortcuts and Enter cannot act on the container beneath us.
        return true;
    }
}
