package fit.biejk.service;

import fit.biejk.dto.OrderUpdateRequest;
import fit.biejk.entity.*;
import fit.biejk.exception.OrderClosedException;
import fit.biejk.exception.StatusConflictException;
import fit.biejk.mapper.OrderMapper;
import fit.biejk.repository.OrderRepository;
import fit.biejk.search.OrderSearchDto;
import fit.biejk.search.OrderSearchMapper;
import fit.biejk.search.OrderSearchService;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.ForbiddenException;
import jakarta.ws.rs.NotFoundException;
import lombok.RequiredArgsConstructor;
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
@RequiredArgsConstructor
public class OrderService {

    /**
     * Log message for a user who tries to change an order that belongs to someone else.
     */
    private static final String NOT_ORDER_OWNER_MESSAGE =
            "User is not the owner of this order. orderId={}, customerId={}";

    /**
     * Service for indexing orders in Elasticsearch.
     */
    private final OrderSearchService orderSearchService;

    /**
     * Mapper responsible for converting Order to OrderSearchDto.
     */
    private final OrderSearchMapper orderSearchMapper;
    /**
     * Repository for accessing order data.
     */
    private final OrderRepository orderRepository;

    /**
     * Service for managing order proposals.
     */
    private final OrderProposalService orderProposalService;

    /** Service for handling business logic related to professionals. */
    private final ProfessionalService professionalService;

    /**
     * Service for user identity validation.
     */
    private final AuthService authService;

    /**
     * Mapper that applies partial updates to order entities.
     */
    private final OrderMapper orderMapper;

    /**
     * Creates a new order with default status.
     *
     * @param order order entity
     * @return created order
     */
    @Transactional
    public Order create(final Order order) {
        log.info("Create order: customerId={}, description={}", order.getCustomer().getId(), order.getDescription());
        order.setStatus(OrderStatus.getStartOrder());
        orderRepository.persist(order);
        log.debug("Order created with ID={}", order.getId());

        OrderSearchDto dto = orderSearchMapper.toDto(order);
        orderSearchService.save(dto);

        return order;
    }

    /**
     * Partially updates an existing order: only the fields present in the request change.
     * <p>
     * The only status that can be requested is {@code CANCELLED}; any other status, or a
     * transition the order cannot make from its current status, is rejected with 409.
     * </p>
     *
     * @param orderId ID of the order to update
     * @param request fields to change
     * @return the updated order
     * @throws ForbiddenException      if the current user is not the owner of the order
     * @throws StatusConflictException if the requested status is not allowed
     */
    @Transactional
    public Order update(final Long orderId, final OrderUpdateRequest request) {
        log.info("Update order: orderId={}", orderId);
        Order order = getById(orderId);
        if (!authService.isCurrentUser(order.getCustomer().getId())) {
            log.error(NOT_ORDER_OWNER_MESSAGE,
                    orderId, order.getCustomer().getId());
            throw new ForbiddenException("You are not the owner of this order");
        }

        OrderStatus newStatus = nextStatus(order.getStatus(), request.getStatus());
        orderMapper.updateEntity(request, order);
        order.setStatus(newStatus);
        orderRepository.persist(order);

        OrderSearchDto dto = orderSearchMapper.toDto(order);
        orderSearchService.save(dto);

        log.debug("Order updated with ID={}", orderId);
        return order;
    }

    /**
     * Decides the status of an order after an update request.
     *
     * @param current   current status of the order
     * @param requested status from the request, or {@code null} to keep the current one
     * @return the status the order must have after the update
     * @throws StatusConflictException if the requested status is not {@code CANCELLED}
     *                                 or the transition is not valid from the current status
     */
    static OrderStatus nextStatus(final OrderStatus current, final OrderStatus requested) {
        if (requested == null || requested == current) {
            return current;
        }
        if (requested != OrderStatus.CANCELLED) {
            throw new StatusConflictException("Only the CANCELLED status can be set on an order");
        }
        if (!current.isValidTransition(requested)) {
            throw new StatusConflictException("Cannot change the status from " + current + " to " + requested);
        }
        return requested;
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
        if (!authService.isCurrentUser(order.getCustomer().getId())) {
            log.error(NOT_ORDER_OWNER_MESSAGE,
                    orderId, order.getCustomer().getId());
            throw new IllegalArgumentException();
        }
        orderRepository.delete(order);

        orderSearchService.delete(orderId);

        log.debug("Order deleted with ID={}", orderId);
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
     * @param professionalId ID of the professional
     * @param proposal proposal entity
     * @return created proposal
     */
    @Transactional
    public OrderProposal proposal(final Long orderId, final Long professionalId, final OrderProposal proposal) {
        log.info("Create proposal: orderId={}, professionalId={}", orderId, proposal.getProfessional().getId());
        Order order = getById(orderId);
        if (!order.getStatus().acceptsProposals()) {
            log.warn("Proposal rejected, order is closed: orderId={}, status={}", orderId, order.getStatus());
            throw new OrderClosedException();
        }
        if (haveProfessionalProposal(order, proposal)) {
            log.error("Order proposal already exists for this professional. orderId={}, professionalId={}",
                    orderId, proposal.getProfessional().getId());
            throw new IllegalArgumentException("Order proposal already exists");
        }
        proposal.setOrder(order);

        Professional professional = professionalService.getById(professionalId);
        proposal.setProfessional(professional);

        orderProposalService.create(proposal);
        order.getOrderProposals().add(proposal);
        order.setStatus(order.getStatus().transitionTo(OrderStatus.CUSTOMER_PENDING));
        orderRepository.persist(order);

        OrderSearchDto dto = orderSearchMapper.toDto(order);
        orderSearchService.save(dto);

        log.debug("Proposal created with ID={}", proposal.getId());
        return proposal;
    }

    /**
     * Checks if the given professional already has a proposal for the order.
     *
     * @param order    the order
     * @param proposal the proposal to check
     * @return true if proposal already exists, false otherwise
     */
    public boolean haveProfessionalProposal(final Order order, final OrderProposal proposal) {
        log.info("Check if professional proposal already exists: orderId={}, professionalId={}",
                order.getId(), proposal.getProfessional().getId());
        List<OrderProposal> orderProposals = order.getOrderProposals();
        for (OrderProposal orderProposal : orderProposals) {
            if (orderProposal.getProfessional().getId().equals(proposal.getProfessional().getId())) {
                log.debug("Proposal already exists for professionalId={}", proposal.getProfessional().getId());
                return true;
            }
        }
        return false;
    }

    /**
     * Retrieves all orders created by the specified customer.
     * <p>
     * Ensures that the requesting user is the same as the target customer.
     * </p>
     *
     * @param userId ID of the customer
     * @param page   page number for pagination
     * @param size   number of orders per page
     * @return list of orders belonging to the customer
     * @throws IllegalArgumentException if the current user is not the same as the customer
     */
    public List<Order> getByCustomerId(final Long userId, final int page, final int size) {
        log.info("Get orders by customerId={}", userId);
        if (!authService.isCurrentUser(userId)) {
            log.warn("User is not logged in");
            throw new IllegalArgumentException("User is not logged in");
        } // todo delete, when admin system will be created
        return orderRepository.findByCustomerId(userId, page, size);
    }

    /**
     * Retrieves all orders assigned to the specified professional.
     * <p>
     * Ensures that the requesting user is the same as the target professional.
     * </p>
     *
     * @param professionalId ID of the professional
     * @param page         page number for pagination
     * @param size         number of orders per page
     * @return list of orders currently assigned to the professional
     * @throws IllegalArgumentException if the current user is not the same as the professional
     */
    public List<Order> getByProfessionalId(final Long professionalId, final int page, final int size) {
        log.info("Get orders by professionalId={}", professionalId);
        if (!authService.isCurrentUser(professionalId)) {
            log.warn("User is not logged in");
            throw new IllegalArgumentException("User is not logged in");
        }
        return orderRepository.findByProfessionalId(professionalId, page, size);
    }


}
