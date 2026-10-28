package fit.biejk.repository;

import fit.biejk.entity.OrderResponse;
import fit.biejk.entity.OrderResponseStatus;
import io.quarkus.hibernate.orm.panache.PanacheRepositoryBase;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.List;
import java.util.UUID;

/**
 * Repository class for performing CRUD operations on {@link OrderResponse} entities.
 * <p>
 * Provides custom query methods for accessing responses by order ID and professional ID.
 * Uses Quarkus Panache to simplify database interactions.
 * </p>
 */
@ApplicationScoped
public class OrderResponseRepository implements PanacheRepositoryBase<OrderResponse, UUID> {

    /**
     * Retrieves all responses associated with a specific order.
     *
     * @param orderId the ID of the order
     * @param page    page number for pagination
     * @param size    number of responses per page
     * @return a list of {@link OrderResponse} entities related to the given order
     */
    public List<OrderResponse> findByOrderId(final UUID orderId, final int page, final int size) {
        return find("order.id", orderId)
                .page(page - 1, size)
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
    public List<OrderResponse> findByProfessionalId(final UUID professionalId, final int page, final int size) {
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
    public void rejectOthersForOrder(final UUID orderId, final UUID approvedResponseId) {
        update("status = ?1 where order.id = ?2 and id != ?3",
                OrderResponseStatus.REJECTED, orderId, approvedResponseId);
    }
}

