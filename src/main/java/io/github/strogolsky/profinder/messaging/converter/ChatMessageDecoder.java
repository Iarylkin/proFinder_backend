package io.github.strogolsky.profinder.messaging.converter;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import io.github.strogolsky.profinder.messaging.dto.ChatMessageRequest;
import jakarta.websocket.Decoder;
import lombok.SneakyThrows;

/**
 * WebSocket decoder that converts incoming JSON strings into {@link ChatMessageRequest} objects.
 * <p>
 * Uses a custom-configured Jackson {@link ObjectMapper} to handle Java 8 date/time types and
 * other serialization settings.
 * </p>
 */
public class ChatMessageDecoder implements Decoder.Text<ChatMessageRequest> {

    /**
     * Configured Jackson {@link ObjectMapper} used for decoding JSON strings.
     */
    private final ObjectMapper jackson = ChatMessageDecoder.getJackson();

    /**
     * Decodes the given JSON string into a {@link ChatMessageRequest} object.
     *
     * @param s the input JSON string
     * @return the decoded {@link ChatMessageRequest}
     */
    @Override
    @SneakyThrows
    public ChatMessageRequest decode(final String s) {
        return jackson.readValue(s, ChatMessageRequest.class);
    }

    /**
     * Checks whether the given input can be decoded.
     *
     * @param s the input string
     * @return true if input is not null
     */
    @Override
    public boolean willDecode(final String s) {
        return s != null;
    }

    /**
     * Creates and configures a reusable {@link ObjectMapper} instance.
     *
     * @return configured {@link ObjectMapper}
     */
    public static ObjectMapper getJackson() {
        ObjectMapper om = new ObjectMapper();
        om.registerModule(new JavaTimeModule());
        om.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        om.configure(DeserializationFeature.ACCEPT_SINGLE_VALUE_AS_ARRAY, true);
        om.configure(SerializationFeature.FAIL_ON_EMPTY_BEANS, false);
        return om;
    }
}
