package fit.biejk.dto;

import fit.biejk.entity.Category;
import fit.biejk.validation.Limits;
import fit.biejk.validation.NoControlChars;
import fit.biejk.validation.Text;
import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Request to create an order. It holds only what the client may choose: the identifier, status, version,
 * customer and location are set by the server.
 *
 * @param categories Categories of the order.
 * @param title Title of the order.
 * @param description Description of the order.
 * @param price Price offered by the customer.
 * @param deadline Date by which the work should be done; it must not be in the past.
 */
public record OrderCreateRequest(
        List<Category> categories,
        @NotBlank
        @Size(min = Limits.ORDER_TITLE_MIN_LENGTH, max = Limits.ORDER_TITLE_MAX_LENGTH)
        @NoControlChars
        String title,
        @NotBlank
        @Size(min = Limits.ORDER_DESCRIPTION_MIN_LENGTH, max = Limits.ORDER_DESCRIPTION_MAX_LENGTH)
        @NoControlChars(multiline = true)
        String description,
        @NotNull(message = "Price is required")
        @Positive(message = "Price must be positive")
        @Max(Limits.PRICE_MAX)
        Integer price,
        @FutureOrPresent
        LocalDateTime deadline) {

    /**
     * Cleans the text fields: the white space around a text is removed and an empty text is {@code null}.
     */
    public OrderCreateRequest {
        title = Text.clean(title);
        description = Text.clean(description);
    }
}
