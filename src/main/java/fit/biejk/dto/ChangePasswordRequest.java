package fit.biejk.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

/**
 * DTO for requesting a password change.
 *
 * Contains the old password, the new password, and confirmation of the new password.
 * All fields are required.
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class ChangePasswordRequest {

    /**
     * The current password of the user.
     * Must match the password stored in the system.
     */
    @NotBlank
    @ToString.Exclude
    private String oldPassword;

    /**
     * The new password the user wants to set.
     * Must be different from the old password.
     */
    @NotBlank
    @ToString.Exclude
    private String newPassword;

    /**
     * Confirmation of the new password.
     * Must match {@code newPassword}.
     */
    @NotBlank
    @ToString.Exclude
    private String confirmPassword;
}

