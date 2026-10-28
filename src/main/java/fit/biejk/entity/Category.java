package fit.biejk.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Represents a category of services.
 * Professionals and orders refer to it from their own side ({@code Professional.categories} and
 * {@code Order.categories}), so the category itself keeps no list of them.
 * This entity is stored in the "categories" table.
 */
@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "categories")
public class Category extends BaseEntity {

    /**
     * Name of the category (e.g. "Haircut", "Legal advice").
     */
    @Column(name = "name")
    private String name;
}
