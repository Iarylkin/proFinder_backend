package fit.biejk.dto;

import fit.biejk.entity.Category;
import fit.biejk.entity.Location;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Data Transfer Object representing a professional.
 * <p>
 * Used to transfer professional data between layers of the application.
 * Contains personal information, location, rating, categories, and avatar link.
 * </p>
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class ProfessionalDto {

    /**
     * Unique identifier of the professional.
     */
    private UUID id;

    /**
     * Version of the resource for optimistic locking: the client sends it back in a {@code PATCH}.
     */
    private Long version;

    /**
     * Average rating based on customer reviews.
     */
    private double averageRating;

    /**
     * Professional's email address.
     */
    private String email;

    /**
     * First name of the professional.
     */
    private String firstName;

    /**
     * Last name of the professional.
     */
    private String lastName;

    /**
     * Phone number of the professional.
     */
    private String phoneNumber;

    /**
     * Geographic location of the professional.
     */
    private Location location;

    /**
     * Timestamp of account creation.
     */
    private LocalDateTime createAt;

    /**
     * Categories the professional works in.
     */
    private List<Category> categories;

    /**
     * Detailed description of the professional's expertise.
     */
    private String description;
}
