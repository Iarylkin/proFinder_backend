package fit.biejk.entity;

import jakarta.json.bind.annotation.JsonbTransient;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

/**
 * Profile of a professional: services, rating and the responses and reviews of the professional.
 * <p>
 * The account data (email, name, location) lives in the {@link User} of the profile.
 * </p>
 */
@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "professional_profiles")
public class ProfessionalProfile extends Profile {

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
