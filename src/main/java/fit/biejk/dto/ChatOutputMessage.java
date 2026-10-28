package fit.biejk.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

import java.util.UUID;

/**
 * DTO representing a message sent from the server to WebSocket clients.
 * <p>
 * This class is used to deliver chat message data to users in a structured format.
 * </p>
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ChatOutputMessage {

    /**
     * Unique identifier of the message.
     */
    private UUID id;

    /**
     * ID of the chat to which the message belongs.
     */
    private UUID chatId;

    /**
     * The content of the message.
     */
    private String content;

    /**
     * ID of the user who sent the message.
     */
    private UUID senderId;

    /**
     * Timestamp indicating when the message was created.
     */
    private LocalDateTime createAt;
}
