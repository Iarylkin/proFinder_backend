package fit.biejk.service;

import fit.biejk.dto.ConfirmProposal;
import fit.biejk.entity.*;
import fit.biejk.repository.OrderProposalRepository;
import fit.biejk.repository.OrderRepository;
import fit.biejk.search.OrderSearchDto;
import fit.biejk.search.OrderSearchMapper;
import fit.biejk.search.OrderSearchService;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.ForbiddenException;
import jakarta.ws.rs.NotFoundException;
import lombok.extern.slf4j.Slf4j;

import java.util.List;

/**
 * Service class for managing {@link OrderProposal} entities.
 * <p>
 * Responsible for creating, approving, confirming, and retrieving proposals,
 * as well as updating the associated {@link Order} based on proposal status.
 * </p>
 */
@Slf4j
@ApplicationScoped
public class OrderProposalService {
    /**
     * Repository for accessing order data.
     */
    @Inject
    private OrderRepository orderRepository;
    /**
     * Service for checking the identity of the currently authenticated user.
     */
    @Inject
    private AuthService authService;

    /**
     * Service for indexing orders in Elasticsearch.
     */
    @Inject
    private OrderSearchService orderSearchService;

    /**
     * Mapper responsible for converting Order to OrderSearchDto.
     */
    @Inject
    private OrderSearchMapper orderSearchMapper;

    /**
     * Repository for managing order proposal persistence.
     */
    @Inject
    private OrderProposalRepository orderProposalRepository;
    /**
     * Creates a new proposal for an order and assigns it the {@link ProposalStatus#CREATED} status.
     *
     * @param orderProposal the proposal to create
     * @return the newly persisted proposal
     */
    public OrderProposal create(final OrderProposal orderProposal) {
        log.info("Creating new proposal: orderId={}, specialistId={}",
                orderProposal.getOrder().getId(),
                orderProposal.getSpecialist().getId());

        orderProposal.setStatus(ProposalStatus.CREATED);
        orderProposalRepository.persist(orderProposal);

        log.debug("Proposal created with ID={}, status={}", orderProposal.getId(), orderProposal.getStatus());
        return orderProposal;
    }

    /**
     * Confirms a specific proposal, updates the associated order with final terms,
     * and marks the order as completed.
     * <p>
     * This method validates that the current user is the owner of the order. It approves
     * the selected proposal, automatically rejects all other proposals for the same order,
     * and synchronizes the changes with the search index.
     * </p>
     *
     * @param proposalId the unique ID of the proposal to be approved
     * @param dto the confirmation data containing the final agreed price and deadline
     * @return the updated and persisted order
     * @throws NotFoundException if the proposal does not exist
     * @throws ForbiddenException if the current user is not the client who created the order
     */
    @Transactional
    public Order confirmAndAssign(final Long proposalId, final ConfirmProposal dto) {
        OrderProposal proposal = orderProposalRepository.findByIdOptional(proposalId)
                .orElseThrow(() -> new NotFoundException("Proposal not found"));

        Order order = proposal.getOrder();

        if (!authService.isCurrentUser(order.getClient().getId())) {
            throw new ForbiddenException("You are not the owner of this order");
        }

        order.setPrice(dto.getFinalPrice());
        order.setDeadline(dto.getFinalDeadline());
        order.setStatus(OrderStatus.COMPLETED);

        proposal.setStatus(ProposalStatus.APPROVED);

        orderProposalRepository.rejectOthersForOrder(order.getId(), proposalId);

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
     * Retrieves a single proposal by its ID.
     *
     * @param proposalId the ID of the proposal to retrieve
     * @return the proposal if found
     * @throws NotFoundException if no proposal exists with the given ID
     */
    public OrderProposal getById(final Long proposalId) {
        log.info("Retrieving proposal by ID={}", proposalId);

        OrderProposal proposal = orderProposalRepository.findById(proposalId);
        if (proposal == null) {
            log.error("Proposal with ID={} not found", proposalId);
            throw new NotFoundException("OrderProposal with ID " + proposalId + " not found");
        }

        log.debug("Proposal found: ID={}, status={}", proposal.getId(), proposal.getStatus());
        return proposal;
    }

    /**
     * Retrieves a proposal visible to the current user: the owner of the order
     * or the specialist who submitted the proposal.
     *
     * @param proposalId the ID of the proposal
     * @return the proposal
     * @throws NotFoundException  if no proposal exists with the given ID
     * @throws ForbiddenException if the current user is neither the order owner nor the author
     */
    public OrderProposal getByIdForCurrentUser(final Long proposalId) {
        OrderProposal proposal = getById(proposalId);
        boolean isOrderOwner = authService.isCurrentUser(proposal.getOrder().getClient().getId());
        boolean isAuthor = authService.isCurrentUser(proposal.getSpecialist().getId());
        if (!isOrderOwner && !isAuthor) {
            throw new ForbiddenException("You do not have access to this proposal");
        }
        return proposal;
    }

    /**
     * Retrieves all proposals associated with a specific order.
     * The caller must be the owner of the order.
     *
     * @param orderId the ID of the order
     * @param page    page number for pagination
     * @param size    number of proposals per page
     * @return list of proposals for the order
     * @throws NotFoundException  if the order does not exist
     * @throws ForbiddenException if the current user is not the owner of the order
     */
    public List<OrderProposal> getByOrderId(final Long orderId, final int page, final int size) {
        log.info("Fetching all proposals for order ID={}", orderId);

        Order order = orderRepository.findById(orderId);
        if (order == null) {
            throw new NotFoundException("Order not found");
        }
        if (!authService.isCurrentUser(order.getClient().getId())) {
            throw new ForbiddenException("You are not the owner of this order");
        }

        List<OrderProposal> proposals = orderProposalRepository.findByOrderId(orderId, page, size);
        log.debug("Found {} proposal(s) for order ID={}", proposals.size(), orderId);

        return proposals;
    }

    /**
     * Retrieves all proposals for a specific order without pagination.
     *
     * @param orderId the ID of the order
     * @return list of all proposals for the order
     */
    public List<OrderProposal> getAllByOrderId(final Long orderId) {
        log.info("Fetching all proposals for order ID={}", orderId);

        List<OrderProposal> proposals = orderProposalRepository.findAllByOrderId(orderId);
        log.debug("Found {} proposal(s) for order ID={}", proposals.size(), orderId);

        return proposals;
    }

    /**
     * Returns the specialist whose proposal has been approved for a given order.
     *
     * @param orderId the ID of the order
     * @return the approved specialist, or {@code null} if no approved proposal is found
     */
    public Specialist getConfirmedSpecialist(final Long orderId) {
        log.info("Searching for confirmed specialist for order ID={}", orderId);

        List<OrderProposal> proposals = getAllByOrderId(orderId);
        for (OrderProposal proposal : proposals) {
            if (ProposalStatus.APPROVED.equals(proposal.getStatus())) {
                log.debug("Confirmed specialist found: specialistId={}, proposalId={}",
                        proposal.getSpecialist().getId(), proposal.getId());
                return proposal.getSpecialist();
            }
        }

        log.warn("No approved proposal found for order ID={}", orderId);
        return null;
    }

    /**
     * Retrieves all proposals submitted by a specific specialist.
     * <p>
     * The caller must be the same as the specialist whose ID is provided.
     * </p>
     *
     * @param specialistId the ID of the specialist
     * @param page         page number for pagination
     * @param size         number of proposals per page
     * @return list of proposals submitted by the specialist
     * @throws IllegalArgumentException if the caller is not the same as the specialist
     */
    public List<OrderProposal> getBySpecialistId(final Long specialistId, final int page, final int size) {
        log.info("Searching for proposal by specialist ID={}", specialistId);
        if (!authService.isCurrentUser(specialistId)) {
            throw new IllegalArgumentException("Specialist ID " + specialistId + " not authorized");
        }
        return orderProposalRepository.findBySpecialistId(specialistId, page, size);
    }
}
