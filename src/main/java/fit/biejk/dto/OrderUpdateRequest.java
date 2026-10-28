package fit.biejk.dto;

import fit.biejk.entity.Location;
import fit.biejk.entity.OrderStatus;
import fit.biejk.entity.Category;
import jakarta.validation.constraints.Positive;
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
    private String description;

    /**
     * New title of the order.
     */
    private String title;

    /**
     * New price of the order.
     */
    @Positive(message = "Price must be positive")
    private Integer price;

    /**
     * New deadline of the order.
     */
    private LocalDateTime deadline;

    /**
     * New location of the order.
     */
    private Location location;
}
