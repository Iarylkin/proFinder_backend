package fit.biejk.dto;

import jakarta.validation.constraints.NotNull;

import java.util.UUID;

/**
 * Data Transfer Object for creating a new chat. This DTO is used when a user wants to initiate a chat with
 * another user.
 *
 * @param recipientId The ID of the user with whom the chat will be created. This field must not be {@code null}.
 */
public record ChatCreateRequest(
        @NotNull
        UUID recipientId) {
}
