package fit.biejk.core.dto;

import fit.biejk.core.entity.Category;
import fit.biejk.core.entity.Location;
import fit.biejk.core.entity.OrderStatus;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Represents an order, sent to the client. An order is created with {@link OrderCreateRequest}. Contains
 * information about the order, including customer ID, categories, status, description, price, creation date,
 * deadline, and location.
 *
 * @param id Unique identifier of the order.
 * @param version Version of the resource for optimistic locking: the client sends it back in a {@code PATCH}.
 * @param customerId Identifier of the customer who placed the order.
 * @param categories List of categories of this order.
 * @param status Current status of the order.
 * @param description Description of the order.
 * @param title Brief title summarizing the order.
 * @param price Price of the order.
 * @param createdAt Timestamp indicating when the order was created.
 * @param deadline Deadline for the order completion.
 * @param location Order's geographical location.
 */
public record OrderResponse(
        UUID id,
        Long version,
        UUID customerId,
        List<Category> categories,
        OrderStatus status,
        String description,
        String title,
        int price,
        LocalDateTime createdAt,
        LocalDateTime deadline,
        Location location) {
}
