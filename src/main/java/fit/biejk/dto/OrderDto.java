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
 * Data Transfer Object representing an order.
 * <p>
 * Contains information about the order, including customer ID, categories,
 * status, description, price, creation date, deadline, and location.
 * </p>
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class OrderDto {

    /**
     * Unique identifier of the order.
     */
    private Long id;

    /**
     * Identifier of the customer who placed the order.
     */
    private Long customerId;

    /**
     * List of categories of this order.
     */
    private List<Category> categories;

    /**
     * Current status of the order.
     */
    private OrderStatus status;

    /**
     * Description of the order.
     */
    private String description;

    /** Brief title summarizing the order. */
    private String title;

    /**
     * Price of the order.
     * Must be a positive value.
     */
    @Positive(message = "Price must be positive")
    private int price;

    /**
     * Timestamp indicating when the order was created.
     */
    private LocalDateTime createdAt;

    /**
     * Deadline for the order completion.
     */
    private LocalDateTime deadline;

    /**
     * Order's geographical location.
     */
    private Location location;
}
