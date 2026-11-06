package fit.biejk.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

/**
 * Profile of a customer: the orders the customer created and the reviews the customer wrote.
 * <p>
 * The account data (email, name, location) lives in the {@link User} of the profile.
 * </p>
 */
@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "customer_profiles")
public class CustomerProfile extends Profile {

    /**
     * List of orders created by the customer.
     * <p>
     * This field is ignored during JSON serialization to prevent recursion.
     * </p>
     */
    @OneToMany(mappedBy = "customer")
    @JsonIgnore
    private List<Order> orders;

    /**
     * List of reviews submitted by the customer.
     */
    @OneToMany(mappedBy = "customer", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Review> reviews;
}
