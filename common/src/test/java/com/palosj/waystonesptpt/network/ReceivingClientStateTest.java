package com.palosj.waystonesptpt.network;

import java.util.List;
import java.util.UUID;
import com.palosj.waystonesptpt.network.payload.ReceivingStatePayload;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class ReceivingClientStateTest {
    @AfterEach void cleanup() { ReceivingClientState.clear(); }

    @Test
    void ignoresOldMenuResponsesAndNeverAddsUnknownDirectoryPlayers() {
        UUID oldSession = ReceivingClientState.begin();
        UUID session = ReceivingClientState.begin();
        UUID listed = UUID.randomUUID();
        UUID unknown = UUID.randomUUID();
        ReceivingClientState.setDirectory(List.of(listed));
        ReceivingClientState.accept(new ReceivingStatePayload(oldSession, 0, false,
                List.of(new ReceivingStatePayload.Entry(listed, false))));
        assertFalse(ReceivingClientState.ready());
        ReceivingClientState.accept(new ReceivingStatePayload(session, 0, true,
                List.of(new ReceivingStatePayload.Entry(listed, false), new ReceivingStatePayload.Entry(unknown, false))));
        assertFalse(ReceivingClientState.allows(listed));
        assertTrue(ReceivingClientState.allows(unknown));
        assertTrue(ReceivingClientState.ownAllowed());
    }

    @Test
    void waitsForItsOwnAcknowledgementAndAcceptsAnAuthoritativeRejectedToggle() {
        UUID session = ReceivingClientState.begin();
        ReceivingClientState.accept(new ReceivingStatePayload(session, 0, true, List.of()));
        long change = ReceivingClientState.beginChange();
        ReceivingClientState.accept(new ReceivingStatePayload(session, 0, true, List.of()));
        assertFalse(ReceivingClientState.ready());
        assertThrows(IllegalStateException.class, ReceivingClientState::beginChange);
        ReceivingClientState.accept(new ReceivingStatePayload(session, change, true, List.of()));
        assertTrue(ReceivingClientState.ready());
        assertTrue(ReceivingClientState.ownAllowed());
    }
}
