package fit.biejk.service;

import fit.biejk.entity.*;
import fit.biejk.repository.OrderRepository;
import fit.biejk.search.OrderSearchDto;
import fit.biejk.search.OrderSearchMapper;
import fit.biejk.search.OrderSearchService;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.ClientErrorException;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.core.Response;
import lombok.extern.slf4j.Slf4j;

import java.util.List;

/**
 * Service class for managing {@link Order} entities.
 * <p>
 * Handles business logic for creating, updating, deleting, and confirming orders,
 * as well as working with related {@link OrderProposal} objects.
 * </p>
 */
@Slf4j
@ApplicationScoped
public class OrderService {

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
     * Repository for accessing order data.
     */
    @Inject
    private OrderRepository orderRepository;

    /**
     * Service for managing order proposals.
     */
    @Inject
    private OrderProposalService orderProposalService;

    /** Service for handling business logic related to specialists. */
    @Inject
    private SpecialistService specialistService;

    /**
     * Service for user identity validation.
     */
    @Inject
    private AuthService authService;

    /**
     * Creates a new order with default status.
     *
     * @param order order entity
     * @return created order
     */
    @Transactional
    public Order create(final Order order) {
        log.info("Create order: clientId={}, description={}", order.getClient().getId(), order.getDescription());
        order.setStatus(OrderStatus.getStartOrder());
        orderRepository.persist(order);
        log.debug("Order created with ID={}", order.getId());

        OrderSearchDto dto = orderSearchMapper.toDto(order);
        orderSearchService.save(dto);

        return order;
    }

    /**
     * Updates the fields of an existing order.
     *
     * @param orderId ID of the order to update
     * @param order   new order data
     * @return original order (before update)
     */
    @Transactional
    public Order update(final Long orderId, final Order order) {
        log.info("Update order: orderId={}, newDescription={}", orderId, order.getDescription());
        Order old = getById(orderId);
        if (!authService.isCurrentUser(old.getClient().getId())) {
            log.error("User is not the owner of this order. orderId={}, clientId={}",
                    orderId, old.getClient().getId());
            throw new IllegalArgumentException();
        }
        old.setServiceOfferings(order.getServiceOfferings());
        old.setLocation(order.getLocation());
        old.setTitle(order.getTitle());
        old.setDescription(order.getDescription());
        old.setPrice(order.getPrice());
        old.setDeadline(order.getDeadline());

        OrderSearchDto dto = orderSearchMapper.toDto(old);
        orderSearchService.save(dto);

        log.debug("Order updated with ID={}", orderId);
        return old;
    }

    /**
     * Deletes an order by ID.
     *
     * @param orderId ID of the order
     */
    @Transactional
    public void delete(final Long orderId) {
        log.info("Delete order: orderId={}", orderId);
        Order order = getById(orderId);
        if (!authService.isCurrentUser(order.getClient().getId())) {
            log.error("User is not the owner of this order. orderId={}, clientId={}",
                    orderId, order.getClient().getId());
            throw new IllegalArgumentException();
        }
        orderRepository.delete(order);

        orderSearchService.delete(orderId);

        log.debug("Order deleted with ID={}", orderId);
    }

    /**
     * Cancels an order, transitioning its status to CANCELLED.
     *
     * @param orderId ID of the order
     * @return updated order
     */
    @Transactional
    public Order cancel(final Long orderId) {
        log.info("Cancel order: orderId={}", orderId);
        Order order = getById(orderId);
        if (!authService.isCurrentUser(order.getClient().getId())) {
            log.error("User is not the owner of this order. orderId={}, clientId={}",
                    orderId, order.getClient().getId());
            throw new IllegalArgumentException();
        }
        order.setStatus(order.getStatus().transitionTo(OrderStatus.CANCELLED));
        orderRepository.persist(order);

        OrderSearchDto dto = orderSearchMapper.toDto(order);
        orderSearchService.save(dto);

        log.debug("Order canceled with ID={}", orderId);
        return order;
    }

    /**
     * Gets an order by ID.
     *
     * @param orderId ID of the order
     * @return found order
     * @throws NotFoundException if not found
     */
    public Order getById(final Long orderId) {
        log.info("Get order by ID={}", orderId);
        Order order = orderRepository.findById(orderId);
        if (order == null) {
            log.error("Order with ID={} not found", orderId);
            throw new NotFoundException("Order not found");
        }
        return order;
    }

    /**
     * Retrieves all orders.
     *
     * @param page page number for pagination
     * @param size number of orders per page
     * @return list of orders
     */
    public List<Order> getAll(final int page, final int size) {
        log.info("Get all orders");
        List<Order> orders = orderRepository.findAll().page(page - 1, size).list();
        log.debug("Found {} orders", orders.size());
        return orders;
    }

    /**
     * Adds a new proposal to an order.
     *
     * @param orderId  ID of the order
     * @param specialistId ID of the specialist
     * @param proposal proposal entity
     * @return created proposal
     */
    @Transactional
    public OrderProposal proposal(final Long orderId, final Long specialistId, final OrderProposal proposal) {
        log.info("Create proposal: orderId={}, specialistId={}", orderId, proposal.getSpecialist().getId());
        Order order = getById(orderId);
        if (!order.getStatus().acceptsProposals()) {
            log.warn("Proposal rejected, order is closed: orderId={}, status={}", orderId, order.getStatus());
            throw new ClientErrorException("This order is closed", Response.Status.CONFLICT);
        }
        if (haveSpecialistProposal(order, proposal)) {
            log.error("Order proposal already exists for this specialist. orderId={}, specialistId={}",
                    orderId, proposal.getSpecialist().getId());
            throw new IllegalArgumentException("Order proposal already exists");
        }
        proposal.setOrder(order);

        Specialist specialist = specialistService.getById(specialistId);
        proposal.setSpecialist(specialist);

        orderProposalService.create(proposal);
        order.getOrderProposals().add(proposal);
        order.setStatus(order.getStatus().transitionTo(OrderStatus.CLIENT_PENDING));
        orderRepository.persist(order);

        OrderSearchDto dto = orderSearchMapper.toDto(order);
        orderSearchService.save(dto);

        log.debug("Proposal created with ID={}", proposal.getId());
        return proposal;
    }

    /**
     * Checks if the given specialist already has a proposal for the order.
     *
     * @param order    the order
     * @param proposal the proposal to check
     * @return true if proposal already exists, false otherwise
     */
    public boolean haveSpecialistProposal(final Order order, final OrderProposal proposal) {
        log.info("Check if specialist proposal already exists: orderId={}, specialistId={}",
                order.getId(), proposal.getSpecialist().getId());
        List<OrderProposal> orderProposals = order.getOrderProposals();
        for (OrderProposal orderProposal : orderProposals) {
            if (orderProposal.getSpecialist().getId().equals(proposal.getSpecialist().getId())) {
                log.debug("Proposal already exists for specialistId={}", proposal.getSpecialist().getId());
                return true;
            }
        }
        return false;
    }

    /**
     * Retrieves all orders created by the specified client.
     * <p>
     * Ensures that the requesting user is the same as the target client.
     * </p>
     *
     * @param userId ID of the client
     * @param page   page number for pagination
     * @param size   number of orders per page
     * @return list of orders belonging to the client
     * @throws IllegalArgumentException if the current user is not the same as the client
     */
    public List<Order> getByClientId(final Long userId, final int page, final int size) {
        log.info("Get orders by clientId={}", userId);
        if (!authService.isCurrentUser(userId)) {
            log.warn("User is not logged in");
            throw new IllegalArgumentException("User is not logged in");
        } // todo delete, when admin system will be created
        return orderRepository.findByClientId(userId, page, size);
    }

    /**
     * Retrieves all orders assigned to the specified specialist.
     * <p>
     * Ensures that the requesting user is the same as the target specialist.
     * </p>
     *
     * @param specialistId ID of the specialist
     * @param page         page number for pagination
     * @param size         number of orders per page
     * @return list of orders currently assigned to the specialist
     * @throws IllegalArgumentException if the current user is not the same as the specialist
     */
    public List<Order> getBySpecialistId(final Long specialistId, final int page, final int size) {
        log.info("Get orders by specialistId={}", specialistId);
        if (!authService.isCurrentUser(specialistId)) {
            log.warn("User is not logged in");
            throw new IllegalArgumentException("User is not logged in");
        }
        return orderRepository.findBySpecialistId(specialistId, page, size);
    }


}
