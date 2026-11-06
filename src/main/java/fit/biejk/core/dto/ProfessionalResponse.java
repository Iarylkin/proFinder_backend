package fit.biejk.core.dto;

import fit.biejk.core.entity.Category;
import fit.biejk.core.entity.Location;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Represents a professional. Used to transfer professional data between layers of the application. Contains
 * personal information, location, rating, categories, and avatar link.
 *
 * @param id Unique identifier of the professional.
 * @param version Version of the resource for optimistic locking: the client sends it back in a {@code PATCH}.
 * @param averageRating Average rating based on customer reviews.
 * @param email Professional's email address.
 * @param firstName First name of the professional.
 * @param lastName Last name of the professional.
 * @param phoneNumber Phone number of the professional.
 * @param location Geographic location of the professional.
 * @param createAt Timestamp of account creation.
 * @param categories Categories the professional works in.
 * @param description Detailed description of the professional's expertise.
 */
public record ProfessionalResponse(
        UUID id,
        Long version,
        double averageRating,
        String email,
        String firstName,
        String lastName,
        String phoneNumber,
        Location location,
        LocalDateTime createAt,
        List<Category> categories,
        String description) {
}
