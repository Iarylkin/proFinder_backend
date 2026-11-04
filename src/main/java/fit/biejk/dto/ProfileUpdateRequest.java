package fit.biejk.dto;

import fit.biejk.entity.Location;
import fit.biejk.validation.Limits;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request body for a partial update of the current user's profile.
 * <p>
 * The fields are the same for every role. Every field is optional: a missing or {@code null} field keeps its
 * current value.
 * </p>
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ProfileUpdateRequest {

    /**
     * New first name.
     */
    @Size(max = CustomerDto.NAME_MAX_LENGTH)
    @Pattern(regexp = Limits.NAME_PATTERN)
    private String firstName;

    /**
     * New last name.
     */
    @Size(max = CustomerDto.NAME_MAX_LENGTH)
    @Pattern(regexp = Limits.NAME_PATTERN)
    private String lastName;

    /**
     * New phone number.
     */
    @Size(max = CustomerDto.PHONE_MAX_LENGTH)
    @Pattern(regexp = Limits.PHONE_PATTERN)
    private String phoneNumber;

    /**
     * New location.
     */
    private Location location;

    /**
     * The version the client has seen. A different current version means someone changed the resource since,
     * and the request is rejected with 409.
     */
    @NotNull
    @PositiveOrZero
    private Long version;
}
