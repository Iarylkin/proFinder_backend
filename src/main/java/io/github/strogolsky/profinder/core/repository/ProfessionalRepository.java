package io.github.strogolsky.profinder.core.repository;

import io.github.strogolsky.profinder.core.entity.ProfessionalProfile;
import io.quarkus.hibernate.orm.panache.PanacheRepositoryBase;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.UUID;

/**
 * Repository class for performing CRUD operations on {@link ProfessionalProfile} entities.
 * <p>
 * Uses PanacheRepositoryBase for simplified data access in Quarkus.
 * </p>
 */
@ApplicationScoped
public class ProfessionalRepository implements PanacheRepositoryBase<ProfessionalProfile, UUID> {

}
