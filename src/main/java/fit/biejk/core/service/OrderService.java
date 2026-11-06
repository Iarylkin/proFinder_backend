package fit.biejk.core.service;

import fit.biejk.auth.service.AuthService;
import fit.biejk.common.exception.StatusConflictException;
import fit.biejk.common.service.VersionCheck;
import fit.biejk.core.entity.Bid;
import fit.biejk.core.entity.Order;
import fit.biejk.core.entity.OrderStatus;
import fit.biejk.core.entity.ProfessionalProfile;
import fit.biejk.core.exception.OrderClosedException;
import fit.biejk.core.repository.OrderRepository;
import fit.biejk.core.search.OrderSearchDto;
import fit.biejk.core.search.OrderSearchMapper;
import fit.biejk.core.search.OrderSearchService;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.ClientErrorException;
import jakarta.ws.rs.ForbiddenException;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.core.Response;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.UUID;

/**
 * Service class for managing {@link Order} entities.
 * <p>
 * Handles business logic for creating, updating, cancelling, and deleting orders,
 * as well as working with related {@link Bid} objects.
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
     * Service for managing bids.
     */
    private final BidService bidService;

    /** Service for handling business logic related to professionals. */
    private final ProfessionalService professionalService;

    /**
     * Service for user identity validation.
     */
    private final AuthService authService;

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
     * Partially updates an existing order: only the non-null fields of {@code changes} are applied.
     * <p>
     * The status of {@code changes} is the requested status. The only status that can be requested
     * is {@code CANCELLED}; any other status, or a transition the order cannot make from its current
     * status, is rejected with 409.
     * </p>
     *
     * @param orderId ID of the order to update
     * @param changes an order holding only the requested changes
     * @return the updated order
     * @throws ForbiddenException      if the current user is not the owner of the order
     * @throws fit.biejk.common.exception.StaleVersionException if the order was changed after the client read it
     * @throws StatusConflictException if the requested status is not allowed
     */
    @Transactional
    public Order update(final UUID orderId, final Order changes) {
        log.info("Update order: orderId={}", orderId);
        Order order = getById(orderId);
        if (!authService.isCurrentUser(order.getCustomer().getId())) {
            log.error(NOT_ORDER_OWNER_MESSAGE,
                    orderId, order.getCustomer().getId());
            throw new ForbiddenException("You are not the owner of this order");
        }
        VersionCheck.require(order, changes.getVersion());

        order.setStatus(nextStatus(order.getStatus(), changes.getStatus()));
        if (changes.getCategories() != null) {
            order.setCategories(changes.getCategories());
        }
        if (changes.getTitle() != null) {
            order.setTitle(changes.getTitle());
        }
        if (changes.getDescription() != null) {
            order.setDescription(changes.getDescription());
        }
        if (changes.getPrice() != null) {
            order.setPrice(changes.getPrice());
        }
        if (changes.getDeadline() != null) {
            order.setDeadline(changes.getDeadline());
        }
        if (changes.getLocation() != null) {
            order.setLocation(changes.getLocation());
        }
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
    public void delete(final UUID orderId) {
        log.info("Delete order: orderId={}", orderId);
        Order order = getById(orderId);
        if (!authService.isCurrentUser(order.getCustomer().getId())) {
            log.error(NOT_ORDER_OWNER_MESSAGE,
                    orderId, order.getCustomer().getId());
            throw new ForbiddenException("You are not the owner of this order");
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
    public Order getById(final UUID orderId) {
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
     * Adds a new bid to an order.
     *
     * @param orderId  ID of the order
     * @param professionalId ID of the professional
     * @param bid bid entity
     * @return created bid
     */
    @Transactional
    public Bid placeBid(final UUID orderId, final UUID professionalId, final Bid bid) {
        log.info("Create bid: orderId={}, professionalId={}", orderId, bid.getProfessional().getId());
        Order order = getById(orderId);
        if (!order.getStatus().acceptsBids()) {
            log.warn("Bid rejected, order is closed: orderId={}, status={}", orderId, order.getStatus());
            throw new OrderClosedException();
        }
        if (haveProfessionalResponse(order, bid)) {
            log.error("Order bid already exists for this professional. orderId={}, professionalId={}",
                    orderId, bid.getProfessional().getId());
            throw new ClientErrorException("Order bid already exists", Response.Status.CONFLICT);
        }
        bid.setOrder(order);

        ProfessionalProfile professional = professionalService.getById(professionalId);
        bid.setProfessional(professional);

        bidService.create(bid);
        order.getBids().add(bid);
        order.setStatus(order.getStatus().transitionTo(OrderStatus.CUSTOMER_PENDING));
        orderRepository.persist(order);

        OrderSearchDto dto = orderSearchMapper.toDto(order);
        orderSearchService.save(dto);

        log.debug("Response created with ID={}", bid.getId());
        return bid;
    }

    /**
     * Checks if the given professional already has a bid for the order.
     *
     * @param order    the order
     * @param bid the bid to check
     * @return true if bid already exists, false otherwise
     */
    public boolean haveProfessionalResponse(final Order order, final Bid bid) {
        log.info("Check if professional bid already exists: orderId={}, professionalId={}",
                order.getId(), bid.getProfessional().getId());
        List<Bid> bids = order.getBids();
        for (Bid existing : bids) {
            if (existing.getProfessional().getId().equals(bid.getProfessional().getId())) {
                log.debug("Bid already exists for professionalId={}", existing.getProfessional().getId());
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
     * @throws ForbiddenException if the current user is not the same as the customer
     */
    public List<Order> getByCustomerId(final UUID userId, final int page, final int size) {
        log.info("Get orders by customerId={}", userId);
        if (!authService.isCurrentUser(userId)) {
            log.warn("Orders of another customer requested: customerId={}", userId);
            throw new ForbiddenException("You can only view your own orders");
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
     * @throws ForbiddenException if the current user is not the same as the professional
     */
    public List<Order> getByProfessionalId(final UUID professionalId, final int page, final int size) {
        log.info("Get orders by professionalId={}", professionalId);
        if (!authService.isCurrentUser(professionalId)) {
            log.warn("Orders of another professional requested: professionalId={}", professionalId);
            throw new ForbiddenException("You can only view your own orders");
        }
        return orderRepository.findByProfessionalId(professionalId, page, size);
    }


}
