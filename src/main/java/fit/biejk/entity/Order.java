package fit.biejk.entity;

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
 * responses from professionals, and a linked review after completion.
 * </p>
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Table(name = "orders")
public class Order {

    /**
     * Unique identifier for the order.
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Customer who placed the order.
     */
    @ManyToOne
    @JoinColumn(name = "customer_id", nullable = false)
    private Customer customer;

    /**
     * Specialization required for fulfilling the order.
     */
    @ManyToMany
    private List<ServiceOffering> serviceOfferings;

    /**
     * Current status of the order.
     */
    @Column(name = "status", nullable = false)
    @Enumerated(EnumType.STRING)
    private OrderStatus status;

    /**
     * List of responses submitted for this order.
     * <p>
     * Ignored during JSON serialization to avoid circular references.
     * </p>
     */
    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true)
    @JsonbTransient
    private List<OrderResponse> orderResponses;


    /** Brief title summarizing the order. */
    @Column(name = "title")
    private String title;

    /**
     * Description provided by the customer for the order.
     */
    @Column(name = "description")
    private String description;

    /**
     * Price set for the order after confirmation.
     */
    @Column(name = "price")
    private int price;

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
