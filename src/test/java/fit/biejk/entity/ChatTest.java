package fit.biejk.entity;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ChatTest {

    private static final UUID FIRST_ID = UUID.randomUUID();
    private static final UUID SECOND_ID = UUID.randomUUID();
    private static final UUID OUTSIDER_ID = UUID.randomUUID();

    private static Chat chatBetween(final UUID firstId, final UUID secondId) {
        Customer first = new Customer();
        first.setId(firstId);
        Professional second = new Professional();
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
