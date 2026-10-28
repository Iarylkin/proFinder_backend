package fit.biejk.service;

import fit.biejk.entity.*;
import fit.biejk.exception.StatusConflictException;
import fit.biejk.repository.OrderResponseRepository;
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

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Service class for managing {@link OrderResponse} entities.
 * <p>
 * Responsible for creating, approving, and retrieving responses,
 * as well as updating the associated {@link Order} based on response status.
 * </p>
 */
@Slf4j
@ApplicationScoped
@RequiredArgsConstructor
public class OrderResponseService {
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
     * Repository for managing order response persistence.
     */
    private final OrderResponseRepository orderResponseRepository;
    /**
     * Creates a new response for an order and assigns it the {@link OrderResponseStatus#CREATED} status.
     *
     * @param orderResponse the response to create
     * @return the newly persisted response
     */
    public OrderResponse create(final OrderResponse orderResponse) {
        log.info("Creating new response: orderId={}, professionalId={}",
                orderResponse.getOrder().getId(),
                orderResponse.getProfessional().getId());

        orderResponse.setStatus(OrderResponseStatus.CREATED);
        orderResponseRepository.persist(orderResponse);

        log.debug("Response created with ID={}, status={}", orderResponse.getId(), orderResponse.getStatus());
        return orderResponse;
    }

    /**
     * Changes the status of a response. Only {@code APPROVED} is supported: the response is approved,
     * the associated order gets the final terms and is marked as completed.
     * <p>
     * This method validates that the current user is the owner of the order. It approves
     * the selected response, automatically rejects all other responses for the same order,
     * and synchronizes the changes with the search index.
     * </p>
     *
     * @param responseId the unique ID of the response to be approved
     * @param status the new status of the response, must be {@code APPROVED}
     * @param finalPrice the final agreed price of the order
     * @param finalDeadline the final agreed deadline of the order
     * @return the updated and persisted order
     * @throws NotFoundException if the response does not exist
     * @throws ForbiddenException if the current user is not the customer who created the order
     * @throws StatusConflictException if the status is not {@code APPROVED}
     */
    @Transactional
    public Order updateStatus(final UUID responseId, final OrderResponseStatus status,
                              final int finalPrice, final LocalDateTime finalDeadline) {
        OrderResponse orderResponse = orderResponseRepository.findByIdOptional(responseId)
                .orElseThrow(() -> new NotFoundException("Response not found"));

        Order order = orderResponse.getOrder();

        if (!authService.isCurrentUser(order.getCustomer().getId())) {
            throw new ForbiddenException("You are not the owner of this order");
        }
        if (status != OrderResponseStatus.APPROVED) {
            throw new StatusConflictException("Only the APPROVED status can be set on a response");
        }

        order.setPrice(finalPrice);
        order.setDeadline(finalDeadline);
        order.setStatus(OrderStatus.COMPLETED);

        orderResponse.setStatus(OrderResponseStatus.APPROVED);

        orderResponseRepository.rejectOthersForOrder(order.getId(), responseId);

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
     * Retrieves a single response by its ID.
     *
     * @param responseId the ID of the response to retrieve
     * @return the response if found
     * @throws NotFoundException if no response exists with the given ID
     */
    public OrderResponse getById(final UUID responseId) {
        log.info("Retrieving response by ID={}", responseId);

        OrderResponse orderResponse = orderResponseRepository.findById(responseId);
        if (orderResponse == null) {
            log.error("Response with ID={} not found", responseId);
            throw new NotFoundException("OrderResponse with ID " + responseId + " not found");
        }

        log.debug("Response found: ID={}, status={}", orderResponse.getId(), orderResponse.getStatus());
        return orderResponse;
    }

    /**
     * Retrieves a response visible to the current user: the owner of the order
     * or the professional who submitted the response.
     *
     * @param responseId the ID of the response
     * @return the response
     * @throws NotFoundException  if no response exists with the given ID
     * @throws ForbiddenException if the current user is neither the order owner nor the author
     */
    public OrderResponse getByIdForCurrentUser(final UUID responseId) {
        OrderResponse orderResponse = getById(responseId);
        boolean isOrderOwner = authService.isCurrentUser(orderResponse.getOrder().getCustomer().getId());
        boolean isAuthor = authService.isCurrentUser(orderResponse.getProfessional().getId());
        if (!isOrderOwner && !isAuthor) {
            throw new ForbiddenException("You do not have access to this response");
        }
        return orderResponse;
    }

    /**
     * Retrieves all responses associated with a specific order.
     * The caller must be the owner of the order.
     *
     * @param orderId the ID of the order
     * @param page    page number for pagination
     * @param size    number of responses per page
     * @return list of responses for the order
     * @throws NotFoundException  if the order does not exist
     * @throws ForbiddenException if the current user is not the owner of the order
     */
    public List<OrderResponse> getByOrderId(final UUID orderId, final int page, final int size) {
        log.info("Fetching all responses for order ID={}", orderId);

        Order order = orderRepository.findById(orderId);
        if (order == null) {
            throw new NotFoundException("Order not found");
        }
        if (!authService.isCurrentUser(order.getCustomer().getId())) {
            throw new ForbiddenException("You are not the owner of this order");
        }

        List<OrderResponse> orderResponses = orderResponseRepository.findByOrderId(orderId, page, size);
        log.debug("Found {} response(s) for order ID={}", orderResponses.size(), orderId);

        return orderResponses;
    }

    /**
     * Retrieves all responses submitted by a specific professional.
     * <p>
     * The caller must be the same as the professional whose ID is provided.
     * </p>
     *
     * @param professionalId the ID of the professional
     * @param page         page number for pagination
     * @param size         number of responses per page
     * @return list of responses submitted by the professional
     * @throws IllegalArgumentException if the caller is not the same as the professional
     */
    public List<OrderResponse> getByProfessionalId(final UUID professionalId, final int page, final int size) {
        log.info("Searching for response by professional ID={}", professionalId);
        if (!authService.isCurrentUser(professionalId)) {
            throw new IllegalArgumentException("Professional ID " + professionalId + " not authorized");
        }
        return orderResponseRepository.findByProfessionalId(professionalId, page, size);
    }
}
