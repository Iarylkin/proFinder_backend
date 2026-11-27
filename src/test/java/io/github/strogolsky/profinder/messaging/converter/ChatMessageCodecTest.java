package io.github.strogolsky.profinder.messaging.converter;

import io.github.strogolsky.profinder.messaging.dto.ChatMessageRequest;
import io.github.strogolsky.profinder.messaging.dto.ChatMessageResponse;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The messages of a chat travel over the WebSocket as JSON: a client sends {@code {"content": "..."}}, and every
 * message that is sent back has ids as text and the time as an ISO 8601 string.
 */
class ChatMessageCodecTest {

    private final ChatMessageDecoder decoder = new ChatMessageDecoder();
    private final ChatMessageEncoder encoder = new ChatMessageEncoder();

    @Test
    void clientMessageIsDecoded() {
        ChatMessageRequest request = decoder.decode("{\"content\":\"Hello\"}");

        assertEquals("Hello", request.content());
    }

    @Test
    void textCanBeDecodedButNullCannot() {
        assertTrue(decoder.willDecode("{}"));
        assertFalse(decoder.willDecode(null));
    }

    @Test
    void textThatIsNotJsonIsNotDecoded() {
        assertThrows(Exception.class, () -> decoder.decode("not json"));
    }

    @Test
    void messageIsEncodedWithTextIdsAndAnIsoTime() throws Exception {
        UUID id = UUID.randomUUID();
        UUID chatId = UUID.randomUUID();
        UUID senderId = UUID.randomUUID();
        ChatMessageResponse response = new ChatMessageResponse(id, chatId, "Hi",
                senderId, LocalDateTime.of(2026, 11, 27, 12, 30, 15));

        String json = encoder.encode(response);

        assertTrue(json.contains("\"id\":\"" + id + "\""), json);
        assertTrue(json.contains("\"chatId\":\"" + chatId + "\""), json);
        assertTrue(json.contains("\"senderId\":\"" + senderId + "\""), json);
        assertTrue(json.contains("\"content\":\"Hi\""), json);
        assertTrue(json.contains("\"createAt\":\"2026-11-27T12:30:15\""), json);
    }
}
