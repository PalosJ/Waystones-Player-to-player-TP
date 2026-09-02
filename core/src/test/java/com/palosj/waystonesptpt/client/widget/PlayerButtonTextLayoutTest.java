package com.palosj.waystonesptpt.client.widget;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class PlayerButtonTextLayoutTest {
    @Test
    void centersUsingTheRenderedTextWidth() {
        assertEquals(40, PlayerButtonTextLayout.centeredTextX(10, 100, 40));
    }

    @Test
    void truncatedTextStaysInsideTheButtonPadding() {
        assertEquals(14, PlayerButtonTextLayout.centeredTextX(10, 100, 92));
    }

    @Test
    void usesTheFirstCodePointAsTheAvatarFallback() {
        assertEquals("A", PlayerButtonTextLayout.firstCharacter("Alex"));
        assertEquals("𐐷", PlayerButtonTextLayout.firstCharacter("𐐷player"));
    }

    @Test
    void usesAQuestionMarkWhenNoNameIsAvailable() {
        assertEquals("?", PlayerButtonTextLayout.firstCharacter(""));
        assertEquals("?", PlayerButtonTextLayout.firstCharacter(null));
    }
}
