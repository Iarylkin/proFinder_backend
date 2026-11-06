package fit.biejk.core.dto;

import fit.biejk.core.entity.Location;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Public view of a customer, returned to any authenticated user who knows the customer ID. Unlike {@link
 * CustomerResponse} it contains no contact data (email, phone number).
 *
 * @param id Unique identifier of the customer.
 * @param firstName First name of the customer.
 * @param lastName Last name of the customer.
 * @param location Geographical location associated with the customer.
 * @param createAt Timestamp of when the customer account was created.
 */
public record CustomerPublicResponse(
        UUID id,
        String firstName,
        String lastName,
        Location location,
        LocalDateTime createAt) {
}
