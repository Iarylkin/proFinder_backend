package io.github.strogolsky.profinder.core.repository;

import io.github.strogolsky.profinder.core.entity.Bid;
import io.github.strogolsky.profinder.core.entity.BidStatus;
import io.quarkus.hibernate.orm.panache.PanacheRepositoryBase;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.List;
import java.util.UUID;

/**
 * Repository class for performing CRUD operations on {@link Bid} entities.
 * <p>
 * Provides custom query methods for accessing bids by order ID and professional ID.
 * Uses Quarkus Panache to simplify database interactions.
 * </p>
 */
@ApplicationScoped
public class BidRepository implements PanacheRepositoryBase<Bid, UUID> {

    /**
     * Retrieves all bids associated with a specific order.
     *
     * @param orderId the ID of the order
     * @param page    page number for pagination
     * @param size    number of bids per page
     * @return a list of {@link Bid} entities related to the given order
     */
    public List<Bid> findByOrderId(final UUID orderId, final int page, final int size) {
        return find("order.id", orderId)
                .page(page - 1, size)
                .list();
    }

    /**
     * Retrieves all bids submitted by a specific professional.
     *
     * @param professionalId the ID of the professional
     * @param page         page number for pagination
     * @param size         number of bids per page
     * @return a list of {@link Bid} entities submitted by the given professional
     */
    public List<Bid> findByProfessionalId(final UUID professionalId, final int page, final int size) {
        return find("professional.id", professionalId)
                .page(page - 1, size)
                .list();
    }

    /**
     * Rejects all bids associated with a specific order, except for the one that was approved.
     * <p>
     * This method performs a bulk update in the database to efficiently transition the status
     * of multiple bids to {@link BidStatus#REJECTED} in a single query.
     * </p>
     *
     * @param orderId the ID of the order whose bids should be rejected
     * @param approvedBidId the ID of the bid that is excluded from rejection
     */
    public void rejectOthersForOrder(final UUID orderId, final UUID approvedBidId) {
        update("status = ?1 where order.id = ?2 and id != ?3",
                BidStatus.REJECTED, orderId, approvedBidId);
    }
}

