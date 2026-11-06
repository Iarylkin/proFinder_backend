package fit.biejk.dto;

import fit.biejk.validation.Limits;
import fit.biejk.validation.Text;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * DTO representing a request to change the user's email address. Contains the new email and the current
 * password for verification.
 *
 * @param newEmail The new email address to be set for the user. Must be a valid, non-blank email address.
 * @param password The current password of the user. Used to verify the user's identity before changing the email.
 */
public record ChangeEmailRequest(
        @NotBlank
        @Email
        @Size(max = Limits.EMAIL_MAX_LENGTH)
        String newEmail,
        @NotBlank
        @Size(max = Limits.PASSWORD_MAX_LENGTH)
        String password) {

    /**
     * Cleans the text fields: the white space around a text is removed and an empty text is {@code null}.
     */
    public ChangeEmailRequest {
        newEmail = Text.clean(newEmail);
    }

    @Override
    public String toString() {
        return "ChangeEmailRequest[newEmail=" + newEmail + ", password=***]";
    }
}
