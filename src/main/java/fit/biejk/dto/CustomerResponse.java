package fit.biejk.dto;

import fit.biejk.entity.Location;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Represents a customer. Used to transfer customer-related data such as identity, contact information,
 * location, and avatar reference.
 *
 * @param id Unique identifier of the customer.
 * @param version Version of the resource for optimistic locking: the client sends it back in a {@code PATCH}.
 * @param email Email address of the customer.
 * @param firstName First name of the customer.
 * @param lastName Last name of the customer.
 * @param phoneNumber Phone number of the customer.
 * @param location Geographical location associated with the customer.
 * @param createAt Timestamp of when the customer account was created.
 */
public record CustomerResponse(
        UUID id,
        Long version,
        String email,
        String firstName,
        String lastName,
        String phoneNumber,
        Location location,
        LocalDateTime createAt) {
}
