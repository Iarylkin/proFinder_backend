package fit.biejk.dto;

import fit.biejk.entity.Location;
import fit.biejk.validation.Limits;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Data Transfer Object representing a customer.
 * <p>
 * Used to transfer customer-related data such as identity,
 * contact information, location, and avatar reference.
 * </p>
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CustomerDto {

    /**
     * Maximum length for firstName and lastName fields.
     */
    public static final int NAME_MAX_LENGTH = 50;

    /**
     * Maximum length for phoneNumber field.
     */
    public static final int PHONE_MAX_LENGTH = 20;

    /**
     * Unique identifier of the customer.
     */
    private UUID id;

    /**
     * Version of the resource for optimistic locking: the client sends it back in a {@code PATCH}.
     */
    private Long version;

    /**
     * Email address of the customer, must be in a valid format.
     */
    @Email
    private String email;

    /**
     * First name of the customer.
     */
    @Size(max = NAME_MAX_LENGTH)
    @Pattern(regexp = Limits.NAME_PATTERN)
    private String firstName;

    /**
     * Last name of the customer.
     */
    @Size(max = NAME_MAX_LENGTH)
    @Pattern(regexp = Limits.NAME_PATTERN)
    private String lastName;

    /**
     * Phone number of the customer.
     */
    @Size(max = PHONE_MAX_LENGTH)
    @Pattern(regexp = Limits.PHONE_PATTERN)
    private String phoneNumber;

    /**
     * Geographical location associated with the customer.
     */
    private Location location;

    /**
     * Timestamp of when the customer account was created.
     */
    private LocalDateTime createAt;
}
