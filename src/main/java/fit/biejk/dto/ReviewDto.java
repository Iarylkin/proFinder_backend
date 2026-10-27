package fit.biejk.dto;

import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Data Transfer Object for transferring review data between client and server.
 * <p>
 * Contains information about the review, including author, recipient, order,
 * textual comment, and numerical rating.
 * </p>
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class ReviewDto {

    /**
     * Unique identifier of the review.
     */
    private Long id;

    /**
     * ID of the professional receiving the review.
     */
    private Long professionalId;

    /**
     * ID of the customer who submitted the review.
     */
    private Long customerId;

    /**
     * ID of the order that the review is related to.
     */
    private Long orderId;

    /**
     * Review comment content.
     * <p>
     * Must not be blank and must contain between 5 and 1000 characters.
     * </p>
     */
    @NotBlank(message = "Comment must not be blank")
    @Size(min = 5, max = 1000, message = "Comment must be between 5 and 1000 characters")
    private String comment;

    /**
     * Rating value from 1 to 5.
     */
    @Min(value = 1, message = "Rating must be at least 1")
    @Max(value = 5, message = "Rating must be at most 5")
    private int rating;
}
