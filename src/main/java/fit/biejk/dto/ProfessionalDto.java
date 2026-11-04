package fit.biejk.dto;

import fit.biejk.entity.Category;
import fit.biejk.entity.Location;
import fit.biejk.validation.Limits;
import fit.biejk.validation.NoControlChars;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
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
    @Size(max = NAME_MAX_LENGTH)
    @Pattern(regexp = Limits.NAME_PATTERN)
    private String firstName;

    /**
     * Last name of the professional.
     */
    @Size(max = NAME_MAX_LENGTH)
    @Pattern(regexp = Limits.NAME_PATTERN)
    private String lastName;

    /**
     * Phone number of the professional.
     */
    @Size(max = PHONE_MAX_LENGTH)
    @Pattern(regexp = Limits.PHONE_PATTERN)
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
    @Size(max = Limits.BIO_MAX_LENGTH)
    @NoControlChars(multiline = true)
    private String description;
}
