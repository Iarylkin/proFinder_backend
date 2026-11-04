package fit.biejk.dto;

import fit.biejk.validation.Limits;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request DTO used for initiating a password reset process.
 * <p>
 * Contains only the user's email address to which a verification code will be sent.
 * </p>
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ForgotPasswordRequest {

    /**
     * The email address of the user requesting password reset.
     */
    @NotBlank(message = "Email must not be blank")
    @Email(message = "Invalid email format")
    @Size(max = Limits.EMAIL_MAX_LENGTH)
    private String email;
}
