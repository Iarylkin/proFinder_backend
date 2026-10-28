package fit.biejk.dto;

import fit.biejk.entity.Location;
import fit.biejk.entity.Category;
import jakarta.validation.constraints.Email;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.validator.constraints.Length;

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
     * Maximum allowed length for firstName and lastName.
     */
    public static final int NAME_MAX_LENGTH = 50;

    /**
     * Maximum allowed length for phoneNumber.
     */
    public static final int PHONE_MAX_LENGTH = 20;

    /**
     * Unique identifier of the professional.
     */
    private UUID id;

    /**
     * Average rating based on customer reviews.
     */
    private double averageRating;

    /**
     * Professional's email address (must be valid format).
     */
    @Email
    private String email;

    /**
     * First name of the professional.
     */
    @Length(max = NAME_MAX_LENGTH)
    private String firstName;

    /**
     * Last name of the professional.
     */
    @Length(max = NAME_MAX_LENGTH)
    private String lastName;

    /**
     * Phone number of the professional.
     */
    @Length(max = PHONE_MAX_LENGTH)
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
