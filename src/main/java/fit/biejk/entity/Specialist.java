package fit.biejk.entity;

import jakarta.json.bind.annotation.JsonbTransient;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Entity representing a specialist.
 * <p>
 * Inherits from {@link User} and includes domain-specific information such as specialization, description,
 * rating, submitted proposals, schedule, and received reviews.
 * </p>
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
@Entity
@Table(name = "specialist")
public class Specialist extends User {

    /**
     * Specialist's professional area of expertise.
     */
    @ManyToMany
    private List<ServiceOffering> serviceOfferings;

    /**
     * Average rating calculated from customer reviews.
     */
    @Column(name = "average_rating")
    private Double averageRating = 0.0;

    /**
     * Short description of the specialist's services or qualifications.
     */
    @Column(name = "description")
    private String description;

    /**
     * List of all proposals submitted by the specialist to various orders.
     * <p>
     * Ignored during JSON serialization to prevent recursion and reduce payload size.
     * </p>
     */
    @OneToMany(mappedBy = "specialist", cascade = CascadeType.ALL, orphanRemoval = true)
    @JsonbTransient
    private List<OrderProposal> orderProposals;

    /**
     * List of reviews received from customers for completed orders.
     * <p>
     * Ignored during JSON serialization to prevent infinite loops or heavy response objects.
     * </p>
     */
    @OneToMany(mappedBy = "specialist", cascade = CascadeType.ALL, orphanRemoval = true)
    @JsonbTransient
    private List<Review> reviews;
}
