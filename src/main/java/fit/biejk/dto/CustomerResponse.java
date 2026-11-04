package fit.biejk.dto;

import fit.biejk.entity.Location;
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
public class CustomerResponse {

    /**
     * Unique identifier of the customer.
     */
    private UUID id;

    /**
     * Version of the resource for optimistic locking: the client sends it back in a {@code PATCH}.
     */
    private Long version;

    /**
     * Email address of the customer.
     */
    private String email;

    /**
     * First name of the customer.
     */
    private String firstName;

    /**
     * Last name of the customer.
     */
    private String lastName;

    /**
     * Phone number of the customer.
     */
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
