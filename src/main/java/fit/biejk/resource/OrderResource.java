package fit.biejk.resource;

import fit.biejk.dto.*;
import fit.biejk.entity.*;
import fit.biejk.mapper.OrderMapper;
import fit.biejk.mapper.BidMapper;
import fit.biejk.search.OrderSearchMapper;
import fit.biejk.search.OrderSearchService;
import fit.biejk.service.*;
import jakarta.annotation.security.PermitAll;
import jakarta.annotation.security.RolesAllowed;
import jakarta.validation.Valid;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.Response;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import java.util.List;
import java.util.UUID;

/**
 * REST resource for managing orders and bids.
 * <p>
 * Supports creating, updating, cancelling and retrieving orders, as well as submitting and listing bids.
 * </p>
 */
@Path("/v1/orders")
@Slf4j
@RequiredArgsConstructor
public class OrderResource {

    /** Service for managing bids. */
    private final BidService bidService;

    /**
     * Service handling order-related business logic.
     */
    private final OrderService orderService;

    /**
     * Service for searching and filtering orders.
     */
    private final OrderSearchService orderSearchService;

    /**
     * Mapper for converting order search results and criteria.
     */
    private final OrderSearchMapper orderSearchMapper;

    /**
     * Mapper for converting between Order entities and DTOs.
     */
    private final OrderMapper orderMapper;

    /**
     * Service for accessing customer information.
     */
    private final CustomerService customerService;

    /**
     * Service for authentication and identity resolution.
     */
    private final AuthService authService;

    /**
     * Mapper for converting between Bid entities and DTOs.
     */
    private final BidMapper bidMapper;


    /**
     * Creates a new order for the authenticated customer.
     *
     * @param request order data
     * @return created order
     */
    @POST
    @RolesAllowed("CUSTOMER")
    public Response create(@Valid final OrderCreateRequest request) {
        log.info("Create order request: {}", request);
        Order order = orderMapper.toEntity(request);
        UUID customerId = authService.getCurrentUserId();
        CustomerProfile customer = customerService.getById(customerId);
        order.setCustomer(customer);
        order.setLocation(customer.getLocation());
        Order result = orderService.create(order);
        log.debug("Order created with ID={}", result.getId());

        return Response.status(Response.Status.CREATED).entity(orderMapper.toDto(result)).build();
    }

    /**
     * Partially updates an order of the authenticated customer.
     * <p>
     * Only the sent fields change. Sending {@code "status": "CANCELLED"} cancels the order.
     * </p>
     *
     * @param orderId order ID
     * @param request fields to change
     * @return updated order
     */
    @PATCH
    @Path("/{orderId}")
    @RolesAllowed("CUSTOMER")
    public Response update(@PathParam("orderId") final UUID orderId, @Valid final OrderUpdateRequest request) {
        log.info("Update order request: orderId={}, request={}", orderId, request);
        Order result = orderService.update(orderId, orderMapper.toEntity(request));
        log.debug("Order updated with ID={}", result.getId());
        return Response.ok(orderMapper.toDto(result)).build();
    }

    /**
     * Submits a bid for an order by the authenticated professional.
     *
     * @param orderId      ID of the order to propose on
     * @param request       bid data
     * @return created bid
     */
    @POST
    @Path("/{orderId}/bids")
    @RolesAllowed("PROFESSIONAL")
    public Response createBid(
            @PathParam("orderId") final UUID orderId,
            @Valid final BidCreateRequest request) {
        log.info("Create bid request: orderId={}, request={}", orderId, request);
        Bid bid = bidMapper.toEntity(request);
        UUID professionalId = authService.getCurrentUserId();

        Bid result = orderService.placeBid(orderId, professionalId, bid);

        log.debug("Response created with ID={}", result.getId());
        return Response.status(Response.Status.CREATED).entity(bidMapper.toDto(result)).build();
    }

    /**
     * Retrieves all bids associated with a specific order.
     *
     * @param orderId    the ID of the order
     * @param pagination the pagination parameters (page and size)
     * @return list of bids for the order
     */
    @GET
    @Path("/{orderId}/bids")
    @RolesAllowed("CUSTOMER")
    public Response getResponses(
            @PathParam("orderId") final UUID orderId,
            @BeanParam final PageRequest pagination
    ) {
        log.info("Get all bids for orderId={}", orderId);
        List<Bid> bids = bidService.getByOrderId(
                orderId,
                pagination.getPage(),
                pagination.getSize());
        log.debug("Bids found: {}", bids.size());
        return Response.ok(bidMapper.toDtoList(bids)).build();
    }

    /**
     * Retrieves an order by ID.
     *
     * @param orderId order ID
     * @return order data
     */
    @GET
    @Path("/{orderId}")
    @PermitAll
    public Response getById(@PathParam("orderId") final UUID orderId) {
        log.info("Get order by ID: {}", orderId);
        Order result = orderService.getById(orderId);
        return Response.ok(orderMapper.toDto(result)).build();
    }

    /**
     * Retrieves a list of all orders.
     *
     * @param criteria   the filtering criteria for searching orders
     * @param pagination the pagination parameters (page and size)
     * @return list of all orders
     */
    @GET
    @PermitAll
    public Response getOrders(
            @BeanParam final OrderFilterCriteria criteria,
            @BeanParam final PageRequest pagination) {

        if (criteria.hasFilters()) {
            var searchResults = orderSearchService.search(
                    criteria.getQuery(),
                    criteria.getLocation(),
                    criteria.getCategories(),
                    pagination.getPage(),
                    pagination.getSize()
            );

            List<Order> result = orderSearchMapper.toEntityList(searchResults);

            return Response.ok(orderMapper.toDtoList(result)).build();
        }

        log.info("Get all orders");
        List<Order> result = orderService.getAll(
                pagination.getPage(),
                pagination.getSize()
        );
        log.debug("Orders found: {}", result.size());
        return Response.ok(orderMapper.toDtoList(result)).build();
    }

    /**
     * Deletes an order by ID.
     *
     * @param orderId order ID
     * @return empty response
     */
    @DELETE
    @Path("/{orderId}")
    @RolesAllowed("CUSTOMER")
    public Response delete(@PathParam("orderId") final UUID orderId) {
        log.info("Delete order request: orderId={}", orderId);
        orderService.delete(orderId);
        return Response.noContent().build();
    }

}
