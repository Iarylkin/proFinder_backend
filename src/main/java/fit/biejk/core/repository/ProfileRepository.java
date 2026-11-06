package fit.biejk.core.repository;

import fit.biejk.core.entity.Profile;
import io.quarkus.hibernate.orm.panache.PanacheRepositoryBase;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.UUID;

/**
 * Repository for the profiles of the users.
 * <p>
 * A lookup by user id returns the profile of the right type ({@code CustomerProfile} or
 * {@code ProfessionalProfile}).
 * </p>
 */
@ApplicationScoped
public class ProfileRepository implements PanacheRepositoryBase<Profile, UUID> {
}
