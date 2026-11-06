package io.github.strogolsky.profinder.core.dto;

import io.github.strogolsky.profinder.common.validation.Limits;
import io.github.strogolsky.profinder.common.validation.NoControlChars;
import io.github.strogolsky.profinder.common.validation.Text;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Request of a customer to review a professional. The author and the recipient come from the session and the
 * path.
 *
 * @param comment Text of the review.
 * @param rating Rating from 1 to 5.
 */
public record ReviewCreateRequest(
        @NotBlank(message = "Comment must not be blank")
        @Size(min = Limits.REVIEW_COMMENT_MIN_LENGTH, max = Limits.REVIEW_COMMENT_MAX_LENGTH,
                message = "Comment must be between 5 and 1000 characters")
        @NoControlChars(multiline = true)
        String comment,
        @Min(value = 1, message = "Rating must be at least 1")
        @Max(value = 5, message = "Rating must be at most 5")
        int rating) {

    /**
     * Cleans the text fields: the white space around a text is removed and an empty text is {@code null}.
     */
    public ReviewCreateRequest {
        comment = Text.clean(comment);
    }
}
