package fit.biejk.dto;

import fit.biejk.validation.Limits;
import fit.biejk.validation.NoControlChars;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request of a customer to review a professional. The author and the recipient come from the session and the path.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ReviewCreateRequest {

    /**
     * Text of the review.
     */
    @NotBlank(message = "Comment must not be blank")
    @Size(min = Limits.REVIEW_COMMENT_MIN_LENGTH, max = Limits.REVIEW_COMMENT_MAX_LENGTH,
            message = "Comment must be between 5 and 1000 characters")
    @NoControlChars(multiline = true)
    private String comment;

    /**
     * Rating from 1 to 5.
     */
    @Min(value = 1, message = "Rating must be at least 1")
    @Max(value = 5, message = "Rating must be at most 5")
    private int rating;
}
