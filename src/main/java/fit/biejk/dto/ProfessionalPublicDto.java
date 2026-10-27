package fit.biejk.dto;

import fit.biejk.entity.Location;
import fit.biejk.entity.ServiceOffering;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Public view of a professional, returned to anyone browsing professionals.
 * <p>
 * Unlike {@link ProfessionalDto} it does not contain the account (login) email.
 * </p>
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class ProfessionalPublicDto {

    /**
     * Unique identifier of the professional.
     */
    private Long id;

    /**
     * Average rating based on customer reviews.
     */
    private double averageRating;

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
     * Services offered by the professional.
     */
    private List<ServiceOffering> serviceOfferings;

    /**
     * Detailed description of the professional's expertise.
     */
    private String description;
}
