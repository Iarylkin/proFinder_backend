package fit.biejk.dto;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

/**
 * Data Transfer Object for creating a new chat.
 * <p>
 * This DTO is used when a user wants to initiate a chat with another user.
 * </p>
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CreateChatRequest {

    /**
     * The ID of the user with whom the chat will be created.
     * <p>
     * This field must not be {@code null}.
     * </p>
     */
    @NotNull
    private UUID recipientId;
}
