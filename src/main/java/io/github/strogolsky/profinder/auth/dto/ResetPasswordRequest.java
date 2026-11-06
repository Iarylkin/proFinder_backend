package io.github.strogolsky.profinder.auth.dto;

import io.github.strogolsky.profinder.common.validation.Limits;
import io.github.strogolsky.profinder.common.validation.Text;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Request DTO for resetting a user's password using a verification code. This object is used when the user
 * submits a new password along with the verification code received via email.
 *
 * @param email The email address of the user requesting the password reset.
 * @param newPassword The new password the user wants to set.
 * @param confirmPassword Confirmation of the new password for validation.
 * @param verificationCode The verification code previously sent to the user's email.
 */
public record ResetPasswordRequest(
        @NotBlank(message = "Email must not be blank")
        @Email(message = "Invalid email format")
        @Size(max = Limits.EMAIL_MAX_LENGTH)
        String email,
        @NotBlank(message = "New password must not be blank")
        @Size(min = Limits.PASSWORD_MIN_LENGTH, max = Limits.PASSWORD_MAX_LENGTH)
        String newPassword,
        @NotBlank(message = "Password confirmation must not be blank")
        @Size(max = Limits.PASSWORD_MAX_LENGTH)
        String confirmPassword,
        @NotBlank(message = "Verification code must not be blank")
        String verificationCode) {

    /**
     * Cleans the text fields: the white space around a text is removed and an empty text is {@code null}.
     */
    public ResetPasswordRequest {
        email = Text.clean(email);
    }

    @Override
    public String toString() {
        return "ResetPasswordRequest[email=" + email + ", newPassword=***, confirmPassword=***, verificationCode=***]";
    }
}
