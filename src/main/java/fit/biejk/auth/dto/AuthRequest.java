package fit.biejk.auth.dto;

import fit.biejk.auth.entity.UserRole;
import fit.biejk.common.validation.Limits;
import fit.biejk.common.validation.Text;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Data Transfer Object for authentication requests. Contains user's email, password, and selected user role
 * for authorization purposes.
 *
 * @param email User email address, must be a valid email format.
 * @param password User password, must not be blank.
 * @param role User role in the system, must not be null.
 */
public record AuthRequest(
        @NotBlank
        @Email
        @Size(max = Limits.EMAIL_MAX_LENGTH)
        String email,
        @NotBlank
        @Size(min = Limits.PASSWORD_MIN_LENGTH, max = Limits.PASSWORD_MAX_LENGTH)
        String password,
        @NotNull
        UserRole role) {

    /**
     * Cleans the text fields: the white space around a text is removed and an empty text is {@code null}.
     */
    public AuthRequest {
        email = Text.clean(email);
    }

    @Override
    public String toString() {
        return "AuthRequest[email=" + email + ", password=***, role=" + role + "]";
    }
}
