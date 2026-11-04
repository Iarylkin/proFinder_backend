package fit.biejk.dto;

import fit.biejk.validation.Limits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * DTO for requesting a password change. Contains the old password, the new password, and confirmation of the
 * new password. All fields are required.
 *
 * @param oldPassword The current password of the user. Must match the password stored in the system.
 * @param newPassword The new password the user wants to set. Must be different from the old password.
 * @param confirmPassword Confirmation of the new password. Must match {@code newPassword}.
 */
public record ChangePasswordRequest(
        @NotBlank
        @Size(max = Limits.PASSWORD_MAX_LENGTH)
        String oldPassword,
        @NotBlank
        @Size(min = Limits.PASSWORD_MIN_LENGTH, max = Limits.PASSWORD_MAX_LENGTH)
        String newPassword,
        @NotBlank
        @Size(max = Limits.PASSWORD_MAX_LENGTH)
        String confirmPassword) {

    @Override
    public String toString() {
        return "ChangePasswordRequest[oldPassword=***, newPassword=***, confirmPassword=***]";
    }
}
