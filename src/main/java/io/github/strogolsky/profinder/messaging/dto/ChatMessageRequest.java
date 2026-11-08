package io.github.strogolsky.profinder.messaging.dto;

/**
 * DTO for receiving a chat message from a client. This class represents the structure of a message sent from a
 * user over WebSocket.
 *
 * @param content The content of the message sent by the user.
 */
public record ChatMessageRequest(
        String content) {
}
