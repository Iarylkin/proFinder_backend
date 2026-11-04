package fit.biejk.dto;

import fit.biejk.validation.Limits;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

/**
 * DTO representing a request to change the user's email address.
 * <p>
 * Contains the new email and the current password for verification.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ChangeEmailRequest {

    /**
     * The new email address to be set for the user.
     * Must be a valid, non-blank email address.
     */
    @NotBlank
    @Email
    @Size(max = Limits.EMAIL_MAX_LENGTH)
    private String newEmail;

    /**
     * The current password of the user.
     * Used to verify the user's identity before changing the email.
     */
    @NotBlank
    @Size(max = Limits.PASSWORD_MAX_LENGTH)
    @ToString.Exclude
    private String password;
}
