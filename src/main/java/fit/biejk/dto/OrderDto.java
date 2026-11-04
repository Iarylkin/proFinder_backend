package fit.biejk.dto;

import fit.biejk.entity.Category;
import fit.biejk.entity.Location;
import fit.biejk.entity.OrderStatus;
import fit.biejk.validation.Limits;
import fit.biejk.validation.NoControlChars;
import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

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
    @NotBlank
    @Size(min = Limits.ORDER_DESCRIPTION_MIN_LENGTH, max = Limits.ORDER_DESCRIPTION_MAX_LENGTH)
    @NoControlChars(multiline = true)
    private String description;

    /** Brief title summarizing the order. */
    @NotBlank
    @Size(min = Limits.ORDER_TITLE_MIN_LENGTH, max = Limits.ORDER_TITLE_MAX_LENGTH)
    @NoControlChars
    private String title;

    /**
     * Price of the order.
     * Must be a positive value.
     */
    @Positive(message = "Price must be positive")
    @Max(Limits.PRICE_MAX)
    private int price;

    /**
     * Timestamp indicating when the order was created.
     */
    private LocalDateTime createdAt;

    /**
     * Deadline for the order completion.
     */
    @FutureOrPresent
    private LocalDateTime deadline;

    /**
     * Order's geographical location.
     */
    private Location location;
}
