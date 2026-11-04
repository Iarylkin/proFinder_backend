package fit.biejk.dto;

import fit.biejk.entity.Category;
import fit.biejk.entity.Location;
import fit.biejk.entity.OrderStatus;
import fit.biejk.validation.Limits;
import fit.biejk.validation.NoControlChars;
import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Request body for a partial update of an order.
 * <p>
 * Every field is optional: a missing or {@code null} field keeps its current value.
 * </p>
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class OrderUpdateRequest {

    /**
     * New list of categories for the order.
     */
    private List<Category> categories;

    /**
     * Requested status. Only {@code CANCELLED} can be set through an order update.
     */
    private OrderStatus status;

    /**
     * New description of the order.
     */
    @Pattern(regexp = Limits.NOT_BLANK_PATTERN, message = "must not be blank")
    @Size(min = Limits.ORDER_DESCRIPTION_MIN_LENGTH, max = Limits.ORDER_DESCRIPTION_MAX_LENGTH)
    @NoControlChars(multiline = true)
    private String description;

    /**
     * New title of the order.
     */
    @Pattern(regexp = Limits.NOT_BLANK_PATTERN, message = "must not be blank")
    @Size(min = Limits.ORDER_TITLE_MIN_LENGTH, max = Limits.ORDER_TITLE_MAX_LENGTH)
    @NoControlChars
    private String title;

    /**
     * New price of the order.
     */
    @Positive(message = "Price must be positive")
    @Max(Limits.PRICE_MAX)
    private Integer price;

    /**
     * New deadline of the order.
     */
    @FutureOrPresent
    private LocalDateTime deadline;

    /**
     * New location of the order.
     */
    private Location location;
}
