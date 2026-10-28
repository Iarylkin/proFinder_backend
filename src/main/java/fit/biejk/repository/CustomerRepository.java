package fit.biejk.repository;

import fit.biejk.entity.Customer;
import io.quarkus.hibernate.orm.panache.PanacheRepositoryBase;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.UUID;

/**
 * Repository class for performing CRUD operations on {@link Customer} entities.
 * <p>
 * Uses PanacheRepositoryBase for simplified data access in Quarkus.
 * </p>
 */
@ApplicationScoped
public class CustomerRepository implements PanacheRepositoryBase<Customer, UUID> {
}
