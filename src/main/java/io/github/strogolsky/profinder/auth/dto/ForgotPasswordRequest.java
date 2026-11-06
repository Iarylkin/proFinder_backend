package io.github.strogolsky.profinder.auth.dto;

import io.github.strogolsky.profinder.common.validation.Limits;
import io.github.strogolsky.profinder.common.validation.Text;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Request DTO used for initiating a password reset process. Contains only the user's email address to which a
 * verification code will be sent.
 *
 * @param email The email address of the user requesting password reset.
 */
public record ForgotPasswordRequest(
        @NotBlank(message = "Email must not be blank")
        @Email(message = "Invalid email format")
        @Size(max = Limits.EMAIL_MAX_LENGTH)
        String email) {

    /**
     * Cleans the text fields: the white space around a text is removed and an empty text is {@code null}.
     */
    public ForgotPasswordRequest {
        email = Text.clean(email);
    }
}
