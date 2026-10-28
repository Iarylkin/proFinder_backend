package fit.biejk.entity;

import jakarta.json.bind.annotation.JsonbTransient;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Entity representing a professional.
 * <p>
 * Inherits from {@link User} and includes domain-specific information such as specialization, description,
 * rating, submitted responses, schedule, and received reviews.
 * </p>
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
@Entity
@Table(name = "professional")
public class Professional extends User {

    /**
     * Professional's area of expertise.
     */
    @ManyToMany
    @JoinTable(name = "professional_categories",
            joinColumns = @JoinColumn(name = "professional_id"),
            inverseJoinColumns = @JoinColumn(name = "category_id"))
    private List<Category> categories;

    /**
     * Average rating calculated from customer reviews.
     */
    @Column(name = "average_rating")
    private Double averageRating = 0.0;

    /**
     * Short description of the professional's services or qualifications.
     */
    @Column(name = "description")
    private String description;

    /**
     * List of all responses submitted by the professional to various orders.
     * <p>
     * Ignored during JSON serialization to prevent recursion and reduce payload size.
     * </p>
     */
    @OneToMany(mappedBy = "professional", cascade = CascadeType.ALL, orphanRemoval = true)
    @JsonbTransient
    private List<OrderResponse> orderResponses;

    /**
     * List of reviews received from customers for completed orders.
     * <p>
     * Ignored during JSON serialization to prevent infinite loops or heavy response objects.
     * </p>
     */
    @OneToMany(mappedBy = "professional", cascade = CascadeType.ALL, orphanRemoval = true)
    @JsonbTransient
    private List<Review> reviews;
}
