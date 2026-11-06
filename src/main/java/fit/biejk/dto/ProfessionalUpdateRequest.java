package fit.biejk.dto;

import fit.biejk.entity.Category;
import fit.biejk.entity.Location;
import fit.biejk.validation.Limits;
import fit.biejk.validation.NoControlChars;
import fit.biejk.validation.Text;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

import java.util.List;

/**
 * Request body for a partial update of the profile of the current professional. Besides the fields of {@link
 * CustomerUpdateRequest} a professional can change the description and the categories. Every field is optional:
 * a field that is not sent (or sent as {@code null}) keeps its value. The phone number and the description are
 * optional data, so an empty text clears them. The names, the location and the categories are required: they can
 * only be replaced, an empty name is rejected and the list of categories must keep at least one category.
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
        @Size(min = 1, max = Limits.NAME_MAX_LENGTH) @Pattern(regexp = Limits.NAME_PATTERN) String firstName,
        @Size(min = 1, max = Limits.NAME_MAX_LENGTH) @Pattern(regexp = Limits.NAME_PATTERN) String lastName,
        @Size(max = Limits.PHONE_MAX_LENGTH) @Pattern(regexp = Limits.PHONE_PATTERN) String phoneNumber,
        Location location,
        @Size(max = Limits.BIO_MAX_LENGTH) @NoControlChars(multiline = true) String description,
        List<Category> categories,
        @NotNull
        @PositiveOrZero
        Long version) {

    /**
     * Cleans the text fields: the white space around a text is removed. An empty text stays empty (it clears the
     * phone number and the description and is rejected for a name); a field that is not sent is {@code null}
     * (keep the value).
     */
    public ProfessionalUpdateRequest {
        firstName = Text.strip(firstName);
        lastName = Text.strip(lastName);
        phoneNumber = Text.strip(phoneNumber);
        description = Text.strip(description);
    }
}
