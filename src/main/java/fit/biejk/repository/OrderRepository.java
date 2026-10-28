package fit.biejk.repository;

import fit.biejk.entity.Order;
import io.quarkus.hibernate.orm.panache.PanacheRepository;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.List;
import java.util.UUID;

/**
 * Repository class for performing CRUD operations on {@link Order} entities.
 * <p>
 * Uses {@link PanacheRepository} for simplified data access with Quarkus and Hibernate ORM.
 * Provides custom queries for retrieving orders by customer and professional.
 * </p>
 */
@ApplicationScoped
public class OrderRepository implements PanacheRepository<Order> {

    /**
     * Retrieves all orders created by the specified customer.
     *
     * @param customerId the ID of the customer
     * @param page     page number for pagination
     * @param size     number of orders per page
     * @return a list of {@link Order} entities associated with the given customer
     */
    public List<Order> findByCustomerId(final UUID customerId, final int page, final int size) {
        return find("customer.id", customerId)
                .page(page - 1, size)
                .list();
    }

    /**
     * Retrieves all orders assigned to the specified professional.
     *
     * @param professionalId the ID of the professional
     * @param page         page number for pagination
     * @param size         number of orders per page
     * @return a list of {@link Order} entities assigned to the given professional
     */
    public List<Order> findByProfessionalId(final UUID professionalId, final int page, final int size) {
        return find("professional.id", professionalId)
                .page(page - 1, size)
                .list();
    }
}

