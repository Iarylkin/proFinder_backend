package fit.biejk.dto;

import fit.biejk.entity.UserRole;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
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
    @Email
    private String email;

    /**
     * User password, must not be blank.
     */
    @NotBlank
    @ToString.Exclude
    private String password;

    /**
     * User role in the system, must not be null.
     */
    @NotNull
    private UserRole role;
}
