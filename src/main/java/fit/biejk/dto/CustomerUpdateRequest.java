package fit.biejk.dto;

import fit.biejk.entity.Location;
import fit.biejk.validation.Limits;
import fit.biejk.validation.Text;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

/**
 * Request body for a partial update of the current user's profile. The fields are the same for every role.
 * Every field is optional: a missing or {@code null} field keeps its current value.
 *
 * @param firstName New first name.
 * @param lastName New last name.
 * @param phoneNumber New phone number.
 * @param location New location.
 * @param version The version the client has seen. A different current version means someone changed the resource
 *        since, and the request is rejected with 409.
 */
public record CustomerUpdateRequest(
        @Size(max = Limits.NAME_MAX_LENGTH)
        @Pattern(regexp = Limits.NAME_PATTERN)
        String firstName,
        @Size(max = Limits.NAME_MAX_LENGTH)
        @Pattern(regexp = Limits.NAME_PATTERN)
        String lastName,
        @Size(max = Limits.PHONE_MAX_LENGTH)
        @Pattern(regexp = Limits.PHONE_PATTERN)
        String phoneNumber,
        Location location,
        @NotNull
        @PositiveOrZero
        Long version) {

    /**
     * Cleans the text fields: the white space around a text is removed and an empty text is {@code null}.
     */
    public CustomerUpdateRequest {
        firstName = Text.clean(firstName);
        lastName = Text.clean(lastName);
        phoneNumber = Text.clean(phoneNumber);
    }
}
