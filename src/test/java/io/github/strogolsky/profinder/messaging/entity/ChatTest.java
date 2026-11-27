package io.github.strogolsky.profinder.messaging.entity;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ChatTest {

    private static final UUID FIRST_ID = UUID.randomUUID();
    private static final UUID SECOND_ID = UUID.randomUUID();
    private static final UUID OUTSIDER_ID = UUID.randomUUID();

    private static Chat chatBetween(final UUID firstId, final UUID secondId) {
        Chat chat = new Chat();
        chat.setUser1Id(firstId);
        chat.setUser2Id(secondId);
        return chat;
    }

    @Test
    void participantsAreRecognised() {
        Chat chat = chatBetween(FIRST_ID, SECOND_ID);
        assertTrue(chat.hasParticipant(FIRST_ID));
        assertTrue(chat.hasParticipant(SECOND_ID));
    }

    @Test
    void outsiderIsRejected() {
        Chat chat = chatBetween(FIRST_ID, SECOND_ID);
        assertFalse(chat.hasParticipant(OUTSIDER_ID));
    }

    @Test
    void nullUserIsRejected() {
        Chat chat = chatBetween(FIRST_ID, SECOND_ID);
        assertFalse(chat.hasParticipant(null));
    }
}
