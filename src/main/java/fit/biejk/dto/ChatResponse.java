package fit.biejk.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

/**
 * DTO for displaying chat information in the chat list.
 */
@NoArgsConstructor
@AllArgsConstructor
@Data
public class ChatResponse {

    /** Unique ID of the chat. */
    private UUID chatId;

    /** ID of the other user in the chat. */
    private UUID partnerId;

    /** First name of the chat partner. */
    private String partnerFirstName;
}
