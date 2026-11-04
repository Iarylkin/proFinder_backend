package fit.biejk.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO for receiving a chat message from a client.
 * <p>
 * This class represents the structure of a message sent from a user over WebSocket.
 * </p>
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ChatMessageRequest {

    /**
     * The content of the message sent by the user.
     */
    private String content;
}
