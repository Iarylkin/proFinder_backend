package fit.biejk.entity;

import jakarta.json.bind.annotation.JsonbTransient;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Entity representing a geographical location such as a city.
 * <p>
 * A location is associated with users and orders registered in that area.
 * </p>
 */
@Data
@Entity
@Table(name = "location")
@NoArgsConstructor
@AllArgsConstructor
public class Location extends BaseEntity {

    /**
     * Name of the location (e.g., city name).
     * Must be unique and not null.
     */
    @Column(name = "name", unique = true, nullable = false)
    private String name;

    /**
     * List of users registered in this location.
     * <p>
     * Mapped by the {@code location} field in the {@link User} entity.
     * This field is ignored during JSON serialization.
     * </p>
     */
    @OneToMany(mappedBy = "location")
    @JsonbTransient
    private List<User> users;

    /**
     * List of orders created for this location.
     * <p>
     * Mapped by the {@code location} field in the {@link Order} entity.
     * This field is ignored during JSON serialization.
     * </p>
     */
    @OneToMany(mappedBy = "location")
    @JsonbTransient
    private List<Order> orders;
}
