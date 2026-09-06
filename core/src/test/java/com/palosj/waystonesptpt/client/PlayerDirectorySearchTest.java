package com.palosj.waystonesptpt.client;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

import java.util.List;
import java.util.Locale;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class PlayerDirectorySearchTest {
    private final PlayerDirectoryEntry alice = new PlayerDirectoryEntry(new UUID(0, 1), "Alice");
    private final PlayerDirectoryEntry fake = new PlayerDirectoryEntry(new UUID(0, 2), "Carpet_Alice_Bot");
    private final PlayerDirectoryEntry bob = new PlayerDirectoryEntry(new UUID(0, 3), "Bob");
    private final List<PlayerDirectoryEntry> players = List.of(alice, fake, bob);

    @Test
    void emptyQueryReturnsTheOriginalDirectoryWithoutChangingIt() {
        assertSame(players, PlayerDirectorySearch.filter(players, ""));
        assertEquals(List.of(alice, fake, bob), players);
        assertEquals(List.of(), PlayerDirectorySearch.filter(List.of(), "Alice"));
    }

    @Test
    void matchesSubstringsIgnoringCaseAndPreservesOrderAndIdentity() {
        List<PlayerDirectoryEntry> filtered = PlayerDirectorySearch.filter(players, "LiCe");
        assertEquals(List.of(alice, fake), filtered);
        assertSame(alice, filtered.getFirst());
        assertSame(fake, filtered.getLast());
        assertEquals(List.of(), PlayerDirectorySearch.filter(players, "offline"));
        assertEquals(List.of(alice, fake, bob), players);
    }

    @Test
    void matchingDoesNotDependOnTheSystemLocale() {
        Locale previous = Locale.getDefault();
        try {
            Locale.setDefault(Locale.forLanguageTag("tr-TR"));
            assertEquals(List.of(alice, fake), PlayerDirectorySearch.filter(players, "ALICE"));
        } finally {
            Locale.setDefault(previous);
        }
    }

    @Test
    void joiningLeavingAndRenamingAreReflectedWithoutChangingTheQuery() {
        assertEquals(List.of(alice, fake), PlayerDirectorySearch.filter(players, "alice"));
        PlayerDirectoryEntry renamed = new PlayerDirectoryEntry(fake.id(), "Carpet_Bot");
        PlayerDirectoryEntry joined = new PlayerDirectoryEntry(new UUID(0, 4), "NewAlice");
        assertEquals(List.of(joined), PlayerDirectorySearch.filter(List.of(renamed, bob, joined), "alice"));
    }
}
