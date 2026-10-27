package fit.biejk.dto;

import fit.biejk.entity.Location;
import fit.biejk.entity.ServiceOffering;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Public view of a specialist, returned to anyone browsing specialists.
 * <p>
 * Unlike {@link SpecialistDto} it does not contain the account (login) email.
 * </p>
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class SpecialistPublicDto {

    /**
     * Unique identifier of the specialist.
     */
    private Long id;

    /**
     * Average rating based on customer reviews.
     */
    private double averageRating;

    /**
     * First name of the specialist.
     */
    private String firstName;

    /**
     * Last name of the specialist.
     */
    private String lastName;

    /**
     * Phone number of the specialist.
     */
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
