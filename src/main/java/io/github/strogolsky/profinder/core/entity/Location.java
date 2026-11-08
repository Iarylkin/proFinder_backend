package io.github.strogolsky.profinder.core.entity;

import io.github.strogolsky.profinder.common.entity.BaseEntity;
import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Entity representing a geographical location such as a city.
 * <p>
 * A location is associated with the profiles of the users and with the orders of that area.
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
     * List of orders created for this location.
     * <p>
     * Mapped by the {@code location} field in the {@link Order} entity.
     * This field is ignored during JSON serialization.
     * </p>
     */
    @OneToMany(mappedBy = "location")
    @JsonIgnore
    private List<Order> orders;
}
