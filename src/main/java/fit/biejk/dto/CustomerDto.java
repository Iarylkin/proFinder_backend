package fit.biejk.dto;

import fit.biejk.entity.Location;
import jakarta.validation.constraints.Email;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.validator.constraints.Length;

import java.time.LocalDateTime;

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
    private Long id;

    /**
     * Email address of the customer, must be in a valid format.
     */
    @Email
    private String email;

    /**
     * First name of the customer.
     */
    @Length(max = NAME_MAX_LENGTH)
    private String firstName;

    /**
     * Last name of the customer.
     */
    @Length(max = NAME_MAX_LENGTH)
    private String lastName;

    /**
     * Phone number of the customer.
     */
    @Length(max = PHONE_MAX_LENGTH)
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
