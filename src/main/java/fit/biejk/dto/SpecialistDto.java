package fit.biejk.dto;

import fit.biejk.entity.Location;
import fit.biejk.entity.ServiceOffering;
import jakarta.validation.constraints.Email;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.validator.constraints.Length;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Data Transfer Object representing a specialist.
 * <p>
 * Used to transfer specialist data between layers of the application.
 * Contains personal information, location, rating, services, and avatar link.
 * </p>
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class SpecialistDto {

    /**
     * Maximum allowed length for firstName and lastName.
     */
    public static final int NAME_MAX_LENGTH = 50;

    /**
     * Maximum allowed length for phoneNumber.
     */
    public static final int PHONE_MAX_LENGTH = 20;

    /**
     * Unique identifier of the specialist.
     */
    private Long id;

    /**
     * Average rating based on customer reviews.
     */
    private double averageRating;

    /**
     * Specialist's email address (must be valid format).
     */
    @Email
    private String email;

    /**
     * First name of the specialist.
     */
    @Length(max = NAME_MAX_LENGTH)
    private String firstName;

    /**
     * Last name of the specialist.
     */
    @Length(max = NAME_MAX_LENGTH)
    private String lastName;

    /**
     * Phone number of the specialist.
     */
    @Length(max = PHONE_MAX_LENGTH)
    private String phoneNumber;

    /**
     * Geographic location of the specialist.
     */
    private Location location;

    /**
     * Timestamp of account creation.
     */
    private LocalDateTime createAt;

    /**
     * Services offered by the specialist.
     */
    private List<ServiceOffering> serviceOfferings;

    /**
     * Detailed description of the specialist's expertise.
     */
    private String description;
}
