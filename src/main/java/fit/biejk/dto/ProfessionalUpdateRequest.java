package fit.biejk.dto;

import fit.biejk.entity.Category;
import fit.biejk.validation.Limits;
import fit.biejk.validation.NoControlChars;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Request body for a partial update of the profile of the current professional.
 * <p>
 * Besides the fields of {@link ProfileUpdateRequest} a professional can change the description and the
 * categories. Every field is optional: a missing or {@code null} field keeps its current value.
 * </p>
 */
@Data
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class ProfessionalUpdateRequest extends ProfileUpdateRequest {

    /**
     * New description of the services.
     */
    @Size(max = Limits.BIO_MAX_LENGTH)
    @NoControlChars(multiline = true)
    private String description;

    /**
     * New list of categories.
     */
    private List<Category> categories;
}
