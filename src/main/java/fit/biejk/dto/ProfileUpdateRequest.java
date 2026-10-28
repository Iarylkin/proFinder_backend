package fit.biejk.dto;

import fit.biejk.entity.Location;
import fit.biejk.entity.Category;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.validator.constraints.Length;

import java.util.List;

/**
 * Request body for a partial update of the current user's profile.
 * <p>
 * Every field is optional: a missing or {@code null} field keeps its current value.
 * {@link #description} and {@link #categories} belong to professionals only.
 * </p>
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ProfileUpdateRequest {

    /**
     * New first name.
     */
    @Length(max = CustomerDto.NAME_MAX_LENGTH)
    private String firstName;

    /**
     * New last name.
     */
    @Length(max = CustomerDto.NAME_MAX_LENGTH)
    private String lastName;

    /**
     * New phone number.
     */
    @Length(max = CustomerDto.PHONE_MAX_LENGTH)
    private String phoneNumber;

    /**
     * New location.
     */
    private Location location;

    /**
     * New description (professionals only).
     */
    private String description;

    /**
     * New list of categories (professionals only).
     */
    private List<Category> categories;
}
