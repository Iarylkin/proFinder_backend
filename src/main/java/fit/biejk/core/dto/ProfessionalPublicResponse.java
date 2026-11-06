package fit.biejk.core.dto;

import fit.biejk.core.entity.Category;
import fit.biejk.core.entity.Location;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Public view of a professional, returned to anyone browsing professionals. Unlike {@link
 * ProfessionalResponse} it does not contain the account (login) email.
 *
 * @param id Unique identifier of the professional.
 * @param averageRating Average rating based on customer reviews.
 * @param firstName First name of the professional.
 * @param lastName Last name of the professional.
 * @param phoneNumber Phone number of the professional.
 * @param location Geographic location of the professional.
 * @param createAt Timestamp of account creation.
 * @param categories Categories the professional works in.
 * @param description Detailed description of the professional's expertise.
 */
public record ProfessionalPublicResponse(
        UUID id,
        double averageRating,
        String firstName,
        String lastName,
        String phoneNumber,
        Location location,
        LocalDateTime createAt,
        List<Category> categories,
        String description) {
}
