package fit.biejk.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Entity representing a review submitted by a customer after an order is completed.
 */
@Entity
@Data
@AllArgsConstructor
@NoArgsConstructor
@Table(name = "review")
public class Review {

    /**
     * Unique identifier of the review.
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * The customer who submitted the review.
     */
    @ManyToOne
    @JoinColumn(name = "customer_id", nullable = false)
    private Customer customer;

    /**
     * The specialist being reviewed.
     */
    @ManyToOne
    @JoinColumn(name = "specialist_id", nullable = false)
    private Specialist specialist;

    /**
     * Rating score from 1 to 5.
     */
    @Column(name = "rating")
    private int rating;

    /**
     * Textual comment provided by the customer (max 1000 characters).
     */
    @Column(name = "comment")
    private String comment;

    /**
     * Timestamp of when the review was created.
     * <p>
     * Automatically set at instantiation and cannot be modified later.
     * </p>
     */
    @Column(name = "createAt", nullable = false, updatable = false)
    private final LocalDateTime createAt = LocalDateTime.now();
}
