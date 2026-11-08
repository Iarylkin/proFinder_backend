package io.github.strogolsky.profinder.core.dto;

import io.github.strogolsky.profinder.common.validation.Limits;
import io.github.strogolsky.profinder.common.validation.Text;
import io.github.strogolsky.profinder.core.entity.Location;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

/**
 * Request body for a partial update of the current user's profile. The fields are the same for every role.
 * Every field is optional: a field that is not sent (or sent as {@code null}) keeps its value. The phone number
 * is optional data, so an empty text clears it. The names and the location are required: they can only be replaced,
 * an empty name is rejected.
 *
 * @param firstName New first name.
 * @param lastName New last name.
 * @param phoneNumber New phone number.
 * @param location New location.
 * @param version The version the client has seen. A different current version means someone changed the resource
 *        since, and the request is rejected with 409.
 */
public record CustomerUpdateRequest(
        @Size(min = 1, max = Limits.NAME_MAX_LENGTH) @Pattern(regexp = Limits.NAME_PATTERN) String firstName,
        @Size(min = 1, max = Limits.NAME_MAX_LENGTH) @Pattern(regexp = Limits.NAME_PATTERN) String lastName,
        @Size(max = Limits.PHONE_MAX_LENGTH) @Pattern(regexp = Limits.PHONE_PATTERN) String phoneNumber,
        Location location,
        @NotNull
        @PositiveOrZero
        Long version) {

    /**
     * Cleans the text fields: the white space around a text is removed. An empty text stays empty (it clears the
     * phone number and is rejected for a name); a field that is not sent is {@code null} (keep the value).
     */
    public CustomerUpdateRequest {
        firstName = Text.strip(firstName);
        lastName = Text.strip(lastName);
        phoneNumber = Text.strip(phoneNumber);
    }
}
