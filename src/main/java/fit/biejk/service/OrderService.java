package fit.biejk.service;

import fit.biejk.entity.*;
import fit.biejk.exception.OrderClosedException;
import fit.biejk.exception.StatusConflictException;
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
 * as well as working with related {@link OrderResponse} objects.
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
     * Service for managing order responses.
     */
    private final OrderResponseService orderResponseService;

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
     * @throws StatusConflictException if the requested status is not allowed
     */
    @Transactional
    public Order update(final Long orderId, final Order changes) {
        log.info("Update order: orderId={}", orderId);
        Order order = getById(orderId);
        if (!authService.isCurrentUser(order.getCustomer().getId())) {
            log.error(NOT_ORDER_OWNER_MESSAGE,
                    orderId, order.getCustomer().getId());
            throw new ForbiddenException("You are not the owner of this order");
        }

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
     * Adds a new response to an order.
     *
     * @param orderId  ID of the order
     * @param professionalId ID of the professional
     * @param orderResponse response entity
     * @return created response
     */
    @Transactional
    public OrderResponse respond(final Long orderId, final Long professionalId, final OrderResponse orderResponse) {
        log.info("Create response: orderId={}, professionalId={}", orderId, orderResponse.getProfessional().getId());
        Order order = getById(orderId);
        if (!order.getStatus().acceptsResponses()) {
            log.warn("Response rejected, order is closed: orderId={}, status={}", orderId, order.getStatus());
            throw new OrderClosedException();
        }
        if (haveProfessionalResponse(order, orderResponse)) {
            log.error("Order response already exists for this professional. orderId={}, professionalId={}",
                    orderId, orderResponse.getProfessional().getId());
            throw new IllegalArgumentException("Order response already exists");
        }
        orderResponse.setOrder(order);

        Professional professional = professionalService.getById(professionalId);
        orderResponse.setProfessional(professional);

        orderResponseService.create(orderResponse);
        order.getOrderResponses().add(orderResponse);
        order.setStatus(order.getStatus().transitionTo(OrderStatus.CUSTOMER_PENDING));
        orderRepository.persist(order);

        OrderSearchDto dto = orderSearchMapper.toDto(order);
        orderSearchService.save(dto);

        log.debug("Response created with ID={}", orderResponse.getId());
        return orderResponse;
    }

    /**
     * Checks if the given professional already has a response for the order.
     *
     * @param order    the order
     * @param orderResponse the response to check
     * @return true if response already exists, false otherwise
     */
    public boolean haveProfessionalResponse(final Order order, final OrderResponse orderResponse) {
        log.info("Check if professional response already exists: orderId={}, professionalId={}",
                order.getId(), orderResponse.getProfessional().getId());
        List<OrderResponse> orderResponses = order.getOrderResponses();
        for (OrderResponse existing : orderResponses) {
            if (existing.getProfessional().getId().equals(orderResponse.getProfessional().getId())) {
                log.debug("Response already exists for professionalId={}", existing.getProfessional().getId());
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
