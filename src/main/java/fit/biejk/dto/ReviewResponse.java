package fit.biejk.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

/**
 * Data Transfer Object with the data of a review, sent to the client. A review is created with
 * {@link ReviewCreateRequest}.
 * <p>
 * Contains information about the review, including author, recipient, order,
 * textual comment, and numerical rating.
 * </p>
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class ReviewResponse {

    /**
     * Unique identifier of the review.
     */
    private UUID id;

    /**
     * ID of the professional receiving the review.
     */
    private UUID professionalId;

    /**
     * ID of the customer who submitted the review.
     */
    private UUID customerId;

    /**
     * ID of the order that the review is related to.
     */
    private UUID orderId;

    /**
     * Review comment content.
     */
    private String comment;

    /**
     * Rating value from 1 to 5.
     */
    private int rating;
}
