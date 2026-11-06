package fit.biejk.core.dto;

import java.util.UUID;

/**
 * Data Transfer Object with the data of a review, sent to the client. A review is created with {@link
 * ReviewCreateRequest}. Contains information about the review, including author, recipient, order, textual
 * comment, and numerical rating.
 *
 * @param id Unique identifier of the review.
 * @param professionalId ID of the professional receiving the review.
 * @param customerId ID of the customer who submitted the review.
 * @param orderId ID of the order that the review is related to.
 * @param comment Review comment content.
 * @param rating Rating value from 1 to 5.
 */
public record ReviewResponse(
        UUID id,
        UUID professionalId,
        UUID customerId,
        UUID orderId,
        String comment,
        int rating) {
}
