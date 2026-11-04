package fit.biejk.dto;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import fit.biejk.entity.Category;
import fit.biejk.entity.Location;
import fit.biejk.json.ClearableTextDeserializer;
import fit.biejk.validation.Limits;
import fit.biejk.validation.NoControlChars;
import fit.biejk.validation.Text;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

import java.util.List;
import java.util.Optional;

/**
 * Request body for a partial update of the profile of the current professional. Besides the fields of {@link
 * CustomerUpdateRequest} a professional can change the description and the categories. Every field is optional:
 * a text field (names, phone number, description) that is not sent keeps its value and one that is sent as
 * {@code null} or as an empty text is cleared; the location and the categories can only be replaced.
 *
 * @param firstName New first name.
 * @param lastName New last name.
 * @param phoneNumber New phone number.
 * @param location New location.
 * @param description New description of the services.
 * @param categories New list of categories.
 * @param version The version the client has seen. A different current version means someone changed the resource
 *        since, and the request is rejected with 409.
 */
public record ProfessionalUpdateRequest(
        @JsonDeserialize(using = ClearableTextDeserializer.class)
        Optional<@Size(max = Limits.NAME_MAX_LENGTH) @Pattern(regexp = Limits.NAME_PATTERN) String> firstName,
        @JsonDeserialize(using = ClearableTextDeserializer.class)
        Optional<@Size(max = Limits.NAME_MAX_LENGTH) @Pattern(regexp = Limits.NAME_PATTERN) String> lastName,
        @JsonDeserialize(using = ClearableTextDeserializer.class)
        Optional<@Size(max = Limits.PHONE_MAX_LENGTH) @Pattern(regexp = Limits.PHONE_PATTERN) String> phoneNumber,
        Location location,
        @JsonDeserialize(using = ClearableTextDeserializer.class)
        Optional<@Size(max = Limits.BIO_MAX_LENGTH) @NoControlChars(multiline = true) String> description,
        List<Category> categories,
        @NotNull
        @PositiveOrZero
        Long version) {

    /**
     * Cleans the text fields: the white space around a text is removed. A field that is not sent is {@code null}
     * (keep the value); a field sent as {@code null} or as an empty text is {@link Optional#empty()} (clear the value).
     */
    public ProfessionalUpdateRequest {
        firstName = Text.cleanOrClear(firstName);
        lastName = Text.cleanOrClear(lastName);
        phoneNumber = Text.cleanOrClear(phoneNumber);
        description = Text.cleanOrClear(description);
    }
}
