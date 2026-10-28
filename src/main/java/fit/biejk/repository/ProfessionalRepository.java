package fit.biejk.repository;

import fit.biejk.entity.Professional;
import io.quarkus.hibernate.orm.panache.PanacheRepositoryBase;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.UUID;

/**
 * Repository class for performing CRUD operations on {@link Professional} entities.
 * <p>
 * Uses PanacheRepositoryBase for simplified data access in Quarkus.
 * </p>
 */
@ApplicationScoped
public class ProfessionalRepository implements PanacheRepositoryBase<Professional, UUID> {

}
