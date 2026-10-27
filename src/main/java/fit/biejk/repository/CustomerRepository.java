package fit.biejk.repository;

import fit.biejk.entity.Customer;
import io.quarkus.hibernate.orm.panache.PanacheRepository;
import jakarta.enterprise.context.ApplicationScoped;

/**
 * Repository class for performing CRUD operations on {@link Customer} entities.
 * <p>
 * Uses PanacheRepository for simplified data access in Quarkus.
 * </p>
 */
@ApplicationScoped
public class CustomerRepository implements PanacheRepository<Customer> {
}
