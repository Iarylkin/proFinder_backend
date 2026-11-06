package io.github.strogolsky.profinder.messaging.dto;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * DTO representing a message sent from the server to WebSocket clients. This class is used to deliver chat
 * message data to users in a structured format.
 *
 * @param id Unique identifier of the message.
 * @param chatId ID of the chat to which the message belongs.
 * @param content The content of the message.
 * @param senderId ID of the user who sent the message.
 * @param createAt Timestamp indicating when the message was created.
 */
public record ChatMessageResponse(
        UUID id,
        UUID chatId,
        String content,
        UUID senderId,
        LocalDateTime createAt) {
}
