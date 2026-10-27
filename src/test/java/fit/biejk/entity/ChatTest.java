package fit.biejk.entity;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ChatTest {

    private static final long FIRST_ID = 1L;
    private static final long SECOND_ID = 2L;
    private static final long OUTSIDER_ID = 3L;

    private static Chat chatBetween(final long firstId, final long secondId) {
        Client first = new Client();
        first.setId(firstId);
        Specialist second = new Specialist();
        second.setId(secondId);
        Chat chat = new Chat();
        chat.setUser1(first);
        chat.setUser2(second);
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
