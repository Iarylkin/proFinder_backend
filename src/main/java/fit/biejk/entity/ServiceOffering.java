package fit.biejk.entity;

import jakarta.json.bind.annotation.JsonbTransient;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Represents a service offering that can be associated with users and orders.
 * This entity is stored in the "service_offering" table.
 */
@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "service_offering")
public class ServiceOffering {

    /**
     * Unique identifier for the service offering.
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Name of the service offering (e.g. "Haircut", "Legal advice").
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
     * List of orders in which this service offering is included.
     * This field is excluded from JSON serialization.
     */
    @ManyToMany
    @JsonbTransient
    private List<Order> orders;
}
