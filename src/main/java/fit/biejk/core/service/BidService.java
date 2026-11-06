package fit.biejk.core.service;

import fit.biejk.auth.service.AuthService;
import fit.biejk.common.exception.StatusConflictException;
import fit.biejk.common.service.VersionCheck;
import fit.biejk.core.entity.Bid;
import fit.biejk.core.entity.BidStatus;
import fit.biejk.core.entity.Order;
import fit.biejk.core.entity.OrderStatus;
import fit.biejk.core.repository.BidRepository;
import fit.biejk.core.repository.OrderRepository;
import fit.biejk.core.search.OrderSearchDto;
import fit.biejk.core.search.OrderSearchMapper;
import fit.biejk.core.search.OrderSearchService;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.ForbiddenException;
import jakarta.ws.rs.NotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Service class for managing {@link Bid} entities.
 * <p>
 * Responsible for creating, approving, and retrieving bids,
 * as well as updating the associated {@link Order} based on bid status.
 * </p>
 */
@Slf4j
@ApplicationScoped
@RequiredArgsConstructor
public class BidService {
    /**
     * Repository for accessing order data.
     */
    private final OrderRepository orderRepository;
    /**
     * Service for checking the identity of the currently authenticated user.
     */
    private final AuthService authService;

    /**
     * Service for indexing orders in Elasticsearch.
     */
    private final OrderSearchService orderSearchService;

    /**
     * Mapper responsible for converting Order to OrderSearchDto.
     */
    private final OrderSearchMapper orderSearchMapper;

    /**
     * Repository for managing order bid persistence.
     */
    private final BidRepository bidRepository;
    /**
     * Creates a new bid for an order and assigns it the {@link BidStatus#CREATED} status.
     *
     * @param bid the bid to create
     * @return the newly persisted bid
     */
    public Bid create(final Bid bid) {
        log.info("Creating new bid: orderId={}, professionalId={}",
                bid.getOrder().getId(),
                bid.getProfessional().getId());

        bid.setStatus(BidStatus.CREATED);
        bidRepository.persist(bid);

        log.debug("Response created with ID={}, status={}", bid.getId(), bid.getStatus());
        return bid;
    }

    /**
     * Changes the status of a bid. Only {@code APPROVED} is supported: the bid is approved,
     * the associated order gets the final terms and is marked as completed.
     * <p>
     * This method validates that the current user is the owner of the order. It approves
     * the selected bid, automatically rejects all other bids for the same order,
     * and synchronizes the changes with the search index.
     * </p>
     *
     * @param bidId the unique ID of the bid to be approved
     * @param status the new status of the bid, must be {@code APPROVED}
     * @param finalPrice the final agreed price of the order
     * @param finalDeadline the final agreed deadline of the order
     * @param version the version of the bid that the client has seen
     * @return the updated and persisted order
     * @throws NotFoundException if the bid does not exist
     * @throws ForbiddenException if the current user is not the customer who created the order
     * @throws fit.biejk.common.exception.StaleVersionException if the bid was changed after the client read it
     * @throws StatusConflictException if the status is not {@code APPROVED}
     */
    @Transactional
    public Order updateStatus(final UUID bidId, final BidStatus status,
                              final int finalPrice, final LocalDateTime finalDeadline, final Long version) {
        Bid bid = bidRepository.findByIdOptional(bidId)
                .orElseThrow(() -> new NotFoundException("Bid not found"));

        Order order = bid.getOrder();

        if (!authService.isCurrentUser(order.getCustomer().getId())) {
            throw new ForbiddenException("You are not the owner of this order");
        }
        VersionCheck.require(bid, version);
        if (status != BidStatus.APPROVED) {
            throw new StatusConflictException("Only the APPROVED status can be set on a bid");
        }

        order.setPrice(finalPrice);
        order.setDeadline(finalDeadline);
        order.setStatus(OrderStatus.COMPLETED);

        bid.setStatus(BidStatus.APPROVED);

        bidRepository.rejectOthersForOrder(order.getId(), bidId);

        orderRepository.persist(order);

        updateSearchIndex(order);

        return order;
    }

    /**
     * Synchronizes the order state with the external search index.
     *
     * @param order the order entity to be indexed
     */
    private void updateSearchIndex(final Order order) {
        OrderSearchDto dto = orderSearchMapper.toDto(order);
        orderSearchService.save(dto);
    }

    /**
     * Retrieves a single bid by its ID.
     *
     * @param bidId the ID of the bid to retrieve
     * @return the bid if found
     * @throws NotFoundException if no bid exists with the given ID
     */
    public Bid getById(final UUID bidId) {
        log.info("Retrieving bid by ID={}", bidId);

        Bid bid = bidRepository.findById(bidId);
        if (bid == null) {
            log.error("Bid with ID={} not found", bidId);
            throw new NotFoundException("Bid with ID " + bidId + " not found");
        }

        log.debug("Bid found: ID={}, status={}", bid.getId(), bid.getStatus());
        return bid;
    }

    /**
     * Retrieves a bid visible to the current user: the owner of the order
     * or the professional who submitted the bid.
     *
     * @param bidId the ID of the bid
     * @return the bid
     * @throws NotFoundException  if no bid exists with the given ID
     * @throws ForbiddenException if the current user is neither the order owner nor the author
     */
    public Bid getByIdForCurrentUser(final UUID bidId) {
        Bid bid = getById(bidId);
        boolean isOrderOwner = authService.isCurrentUser(bid.getOrder().getCustomer().getId());
        boolean isAuthor = authService.isCurrentUser(bid.getProfessional().getId());
        if (!isOrderOwner && !isAuthor) {
            throw new ForbiddenException("You do not have access to this bid");
        }
        return bid;
    }

    /**
     * Retrieves all bids associated with a specific order.
     * The caller must be the owner of the order.
     *
     * @param orderId the ID of the order
     * @param page    page number for pagination
     * @param size    number of bids per page
     * @return list of bids for the order
     * @throws NotFoundException  if the order does not exist
     * @throws ForbiddenException if the current user is not the owner of the order
     */
    public List<Bid> getByOrderId(final UUID orderId, final int page, final int size) {
        log.info("Fetching all bids for order ID={}", orderId);

        Order order = orderRepository.findById(orderId);
        if (order == null) {
            throw new NotFoundException("Order not found");
        }
        if (!authService.isCurrentUser(order.getCustomer().getId())) {
            throw new ForbiddenException("You are not the owner of this order");
        }

        List<Bid> bids = bidRepository.findByOrderId(orderId, page, size);
        log.debug("Found {} bid(s) for order ID={}", bids.size(), orderId);

        return bids;
    }

    /**
     * Retrieves all bids submitted by a specific professional.
     * <p>
     * The caller must be the same as the professional whose ID is provided.
     * </p>
     *
     * @param professionalId the ID of the professional
     * @param page         page number for pagination
     * @param size         number of bids per page
     * @return list of bids submitted by the professional
     * @throws ForbiddenException if the caller is not the same as the professional
     */
    public List<Bid> getByProfessionalId(final UUID professionalId, final int page, final int size) {
        log.info("Searching for bid by professional ID={}", professionalId);
        if (!authService.isCurrentUser(professionalId)) {
            throw new ForbiddenException("You can only view your own bids");
        }
        return bidRepository.findByProfessionalId(professionalId, page, size);
    }
}
