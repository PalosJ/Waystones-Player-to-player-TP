package com.palosj.waystonesptpt.client.widget;

import static org.junit.jupiter.api.Assertions.*;

import com.palosj.waystonesptpt.network.ReceivingClientState;
import com.palosj.waystonesptpt.network.payload.ReceivingStatePayload;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

class PlayerReceivingControlTest {
    @AfterEach
    void cleanup() {
        ReceivingClientState.clear();
    }

    @Test
    void squareIconButtonWaitsForConfirmationAndKeepsItsLastConfirmedState() {
        PlayerReceivingControl button = new PlayerReceivingControl(10, 20);
        assertEquals(20, button.getWidth());
        assertEquals(20, button.getHeight());
        assertEquals("", button.getMessage().getString());
        assertFalse(button.active);

        ReceivingClientState.accept(new ReceivingStatePayload(ReceivingClientState.session(), 0, false, List.of()));
        button.tick();
        assertTrue(button.active);
        assertFalse(button.receivingAllowed());

        long change = ReceivingClientState.beginChange();
        button.tick();
        assertFalse(button.active);
        assertFalse(button.receivingAllowed());
        ReceivingClientState.accept(new ReceivingStatePayload(ReceivingClientState.session(), change, true, List.of()));
        button.tick();
        assertTrue(button.active);
        assertTrue(button.receivingAllowed());
        assertEquals("", button.getMessage().getString());
    }
}
