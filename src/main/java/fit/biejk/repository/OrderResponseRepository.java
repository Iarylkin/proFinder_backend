package fit.biejk.repository;

import fit.biejk.entity.OrderResponse;
import fit.biejk.entity.OrderResponseStatus;
import io.quarkus.hibernate.orm.panache.PanacheRepository;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.List;

/**
 * Repository class for performing CRUD operations on {@link OrderResponse} entities.
 * <p>
 * Provides custom query methods for accessing responses by order ID and professional ID.
 * Uses Quarkus Panache to simplify database interactions.
 * </p>
 */
@ApplicationScoped
public class OrderResponseRepository implements PanacheRepository<OrderResponse> {

    /**
     * Retrieves all responses associated with a specific order.
     *
     * @param orderId the ID of the order
     * @param page    page number for pagination
     * @param size    number of responses per page
     * @return a list of {@link OrderResponse} entities related to the given order
     */
    public List<OrderResponse> findByOrderId(final Long orderId, final int page, final int size) {
        return find("order.id", orderId)
                .page(page - 1, size)
                .list();
    }

    /**
     * Retrieves all responses for a specific order without pagination.
     *
     * @param orderId the ID of the order
     * @return list of all responses for the order
     */
    public List<OrderResponse> findAllByOrderId(final Long orderId) {
        return find("order.id", orderId)
                .list();
    }

    /**
     * Retrieves all responses submitted by a specific professional.
     *
     * @param professionalId the ID of the professional
     * @param page         page number for pagination
     * @param size         number of responses per page
     * @return a list of {@link OrderResponse} entities submitted by the given professional
     */
    public List<OrderResponse> findByProfessionalId(final Long professionalId, final int page, final int size) {
        return find("professional.id", professionalId)
                .page(page - 1, size)
                .list();
    }

    /**
     * Rejects all responses associated with a specific order, except for the one that was approved.
     * <p>
     * This method performs a bulk update in the database to efficiently transition the status
     * of multiple responses to {@link OrderResponseStatus#REJECTED} in a single query.
     * </p>
     *
     * @param orderId the ID of the order whose responses should be rejected
     * @param approvedResponseId the ID of the response that is excluded from rejection
     */
    public void rejectOthersForOrder(final Long orderId, final Long approvedResponseId) {
        update("status = ?1 where order.id = ?2 and id != ?3",
                OrderResponseStatus.REJECTED, orderId, approvedResponseId);
    }
}

