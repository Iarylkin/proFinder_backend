package fit.biejk.dto;

import java.util.UUID;

/**
 * DTO for displaying chat information in the chat list.
 *
 * @param chatId Unique ID of the chat.
 * @param partnerId ID of the other user in the chat.
 * @param partnerFirstName First name of the chat partner.
 */
public record ChatResponse(
        UUID chatId,
        UUID partnerId,
        String partnerFirstName) {
}
