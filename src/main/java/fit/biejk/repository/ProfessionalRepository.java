package fit.biejk.repository;

import fit.biejk.entity.Professional;
import io.quarkus.hibernate.orm.panache.PanacheRepository;
import jakarta.enterprise.context.ApplicationScoped;

/**
 * Repository class for performing CRUD operations on {@link Professional} entities.
 * <p>
 * Uses PanacheRepository for simplified data access in Quarkus.
 * </p>
 */
@ApplicationScoped
public class ProfessionalRepository implements PanacheRepository<Professional> {

}
