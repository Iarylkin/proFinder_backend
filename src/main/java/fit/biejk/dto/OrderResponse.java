package fit.biejk.dto;

import fit.biejk.entity.Category;
import fit.biejk.entity.Location;
import fit.biejk.entity.OrderStatus;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Data Transfer Object representing an order, sent to the client. An order is created with
 * {@link OrderCreateRequest}.
 * <p>
 * Contains information about the order, including customer ID, categories,
 * status, description, price, creation date, deadline, and location.
 * </p>
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class OrderResponse {

    /**
     * Unique identifier of the order.
     */
    private UUID id;

    /**
     * Version of the resource for optimistic locking: the client sends it back in a {@code PATCH}.
     */
    private Long version;

    /**
     * Identifier of the customer who placed the order.
     */
    private UUID customerId;

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
     */
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
