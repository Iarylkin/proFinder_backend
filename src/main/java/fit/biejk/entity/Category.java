package fit.biejk.entity;

import jakarta.json.bind.annotation.JsonbTransient;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Represents a category of services that can be associated with users and orders.
 * This entity is stored in the "categories" table.
 */
@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "categories")
public class Category {

    /**
     * Unique identifier for the category.
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Name of the category (e.g. "Haircut", "Legal advice").
     */
    @Column(name = "name")
    private String name;

    /**
     * List of users (professionals) who offer this service.
     * This field is excluded from JSON serialization.
     */
    @ManyToMany
    @JsonbTransient
    private List<User> users;

    /**
     * List of orders in which this category is included.
     * This field is excluded from JSON serialization.
     */
    @ManyToMany
    @JsonbTransient
    private List<Order> orders;
}
