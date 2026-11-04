package fit.biejk.dto;

import fit.biejk.entity.UserRole;
import fit.biejk.validation.Limits;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

/**
 * Data Transfer Object for authentication requests.
 * <p>
 * Contains user's email, password, and selected user role
 * for authorization purposes.
 * </p>
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class AuthRequest {

    /**
     * User email address, must be a valid email format.
     */
    @NotBlank
    @Email
    @Size(max = Limits.EMAIL_MAX_LENGTH)
    private String email;

    /**
     * User password, must not be blank.
     */
    @NotBlank
    @Size(min = Limits.PASSWORD_MIN_LENGTH, max = Limits.PASSWORD_MAX_LENGTH)
    @ToString.Exclude
    private String password;

    /**
     * User role in the system, must not be null.
     */
    @NotNull
    private UserRole role;
}
