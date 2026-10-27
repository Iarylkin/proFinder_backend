package fit.biejk.dto;

import fit.biejk.entity.Location;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Public view of a customer, returned to any authenticated user who knows the customer ID.
 * <p>
 * Unlike {@link CustomerDto} it contains no contact data (email, phone number).
 * </p>
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CustomerPublicDto {

    /**
     * Unique identifier of the customer.
     */
    private Long id;

    /**
     * First name of the customer.
     */
    private String firstName;

    /**
     * Last name of the customer.
     */
    private String lastName;

    /**
     * Geographical location associated with the customer.
     */
    private Location location;

    /**
     * Timestamp of when the customer account was created.
     */
    private LocalDateTime createAt;
}
