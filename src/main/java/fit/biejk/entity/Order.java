package fit.biejk.entity;

import fit.biejk.validation.Limits;
import jakarta.json.bind.annotation.JsonbTransient;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Entity representing an order placed by a customer.
 * <p>
 * Contains details about specialization, status, price, deadline,
 * bids from professionals, and a linked review after completion.
 * </p>
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Table(name = "orders")
public class Order extends BaseEntity {

    /**
     * Customer who placed the order.
     */
    @ManyToOne
    @JoinColumn(name = "customer_id", nullable = false)
    private CustomerProfile customer;

    /**
     * Specialization required for fulfilling the order.
     */
    @ManyToMany
    @JoinTable(name = "orders_categories",
            joinColumns = @JoinColumn(name = "order_id"),
            inverseJoinColumns = @JoinColumn(name = "category_id"))
    private List<Category> categories;

    /**
     * Current status of the order.
     */
    @Column(name = "status", nullable = false)
    @Enumerated(EnumType.STRING)
    private OrderStatus status;

    /**
     * List of bids submitted for this order.
     * <p>
     * Ignored during JSON serialization to avoid circular references.
     * </p>
     */
    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true)
    @JsonbTransient
    private List<Bid> bids;


    /** Brief title summarizing the order. */
    @Column(name = "title", length = Limits.ORDER_TITLE_MAX_LENGTH)
    private String title;

    /**
     * Description provided by the customer for the order.
     */
    @Column(name = "description", length = Limits.ORDER_DESCRIPTION_MAX_LENGTH)
    private String description;

    /**
     * Price of the order; replaced by the final agreed price when a bid is approved.
     */
    @Column(name = "price")
    private Integer price;

    /**
     * Timestamp indicating when the order was created.
     * <p>
     * This field is final and set at creation time.
     * </p>
     */
    @Column(name = "createAt", nullable = false, updatable = false)
    private final LocalDateTime createdAt = LocalDateTime.now();

    /**
     * Deadline for completing the order.
     */
    @Column(name = "deadline")
    private LocalDateTime deadline;

    /**
     * Order's geographical location.
     */
    @ManyToOne
    @JoinColumn(name = "location_id", nullable = false)
    private Location location;
}
