package fit.biejk.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

/**
 * Request DTO for resetting a user's password using a verification code.
 * <p>
 * This object is used when the user submits a new password along with the verification code
 * received via email.
 * </p>
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class ResetPasswordRequest {

    /**
     * The email address of the user requesting the password reset.
     */
    @NotBlank(message = "Email must not be blank")
    @Email(message = "Invalid email format")
    private String email;

    /**
     * The new password the user wants to set.
     */
    @NotBlank(message = "New password must not be blank")
    @ToString.Exclude
    private String newPassword;

    /**
     * Confirmation of the new password for validation.
     */
    @NotBlank(message = "Password confirmation must not be blank")
    @ToString.Exclude
    private String confirmPassword;

    /**
     * The verification code previously sent to the user's email.
     */
    @NotBlank(message = "Verification code must not be blank")
    @ToString.Exclude
    private String verificationCode;
}
