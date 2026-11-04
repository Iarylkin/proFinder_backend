package fit.biejk.resource;

import fit.biejk.dto.PageRequest;
import fit.biejk.dto.ProfileUpdateRequest;
import fit.biejk.entity.CustomerProfile;
import fit.biejk.entity.Order;
import fit.biejk.entity.Review;
import fit.biejk.mapper.CustomerMapper;
import fit.biejk.mapper.OrderMapper;
import fit.biejk.mapper.ReviewMapper;
import fit.biejk.service.AuthService;
import fit.biejk.service.CustomerService;
import fit.biejk.service.OrderService;
import fit.biejk.service.ReviewService;
import jakarta.annotation.security.RolesAllowed;
import jakarta.validation.Valid;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.Response;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.UUID;

/**
 * REST resource for managing customers.
 * <p>
 * Provides endpoints for retrieving and managing customer data.
 * </p>
 */
@Path("/v1/customers")
@Slf4j
@RequiredArgsConstructor
public class CustomerResource {
    /**
     * Mapper for converting between Customer entities and DTOs.
     */
    private final CustomerMapper customerMapper;

    /**
     * Service for managing order-related operations.
     */
    private final OrderService orderService;

    /**
     * Mapper for converting between Order entities and DTOs.
     */
    private final OrderMapper orderMapper;

    /**
     * Service layer for handling customer-related business logic.
     */
    private final CustomerService customerService;

    /**
     * Service for authentication and user identity operations.
     */
    private final AuthService authService;

    /** Maps Review entity to ReviewResponse and vice versa. */
    private final ReviewMapper reviewMapper;

    /** Service for accessing customer reviews. */
    private final ReviewService reviewService;

    /**
     * Retrieves the public view of a customer by ID (no contact data).
     * Available to any authenticated user.
     *
     * @param customerId the customer ID
     * @return public customer data
     */
    @GET
    @Path("/{customerId}")
    @RolesAllowed({"CUSTOMER", "PROFESSIONAL"})
    public Response getById(@PathParam("customerId") final UUID customerId) {
        log.info("getById request: {}", customerId);
        CustomerProfile result = customerService.getById(customerId);
        log.debug("Found customer with ID={}", result.getId());
        return Response.ok(customerMapper.toPublicDto(result)).build();
    }

    /**
     * Returns the profile of the currently authenticated customer.
     *
     * @return the customer
     */
    @GET
    @Path("/me")
    @RolesAllowed("CUSTOMER")
    public Response getMe() {
        UUID id = authService.getCurrentUserId();
        log.info("Get profile for customer ID={}", id);
        return Response.ok(customerMapper.toDto(customerService.getById(id))).build();
    }

    /**
     * Partially updates the profile of the currently authenticated customer: only the fields that are sent change.
     *
     * @param request the fields to change
     * @return the updated customer
     */
    @PATCH
    @Path("/me")
    @RolesAllowed("CUSTOMER")
    public Response updateMe(@Valid final ProfileUpdateRequest request) {
        UUID id = authService.getCurrentUserId();
        log.info("Update profile for customer ID={}", id);
        CustomerProfile result = customerService.updateProfile(id, customerMapper.toEntity(request));
        return Response.ok(customerMapper.toDto(result)).build();
    }

    /**
     * Deletes the profile of the currently authenticated customer.
     *
     * @return response status
     */
    @DELETE
    @Path("/me")
    @RolesAllowed("CUSTOMER")
    public Response deleteProfile() {
        log.info("deleteProfile request");
        UUID id = authService.getCurrentUserId();
        customerService.delete(id);
        log.debug("Deleted profile for customer with ID={}", id);
        return Response.noContent().build();
    }

    /**
     * Retrieves all reviews submitted by the currently authenticated customer.
     *
     * @param pagination the pagination parameters (page and size)
     * @return list of customer's reviews
     */
    @GET
    @Path("/me/reviews")
    @RolesAllowed("CUSTOMER")
    public Response getReviews(
            @BeanParam final PageRequest pagination
    ) {
        UUID customerId = authService.getCurrentUserId();
        List<Review> res = reviewService.getByCustomerId(
                customerId,
                pagination.getPage(),
                pagination.getSize());
        return Response.ok(reviewMapper.toDtoList(res)).build();
    }

    /**
     * Retrieves all orders created by a specific customer.
     * <p>
     * Only accessible to authenticated users with the CUSTOMER role.
     * </p>
     *
     * @param pagination the pagination parameters (page and size)
     * @return list of orders created by the customer
     */
    @GET
    @Path("/me/orders")
    @RolesAllowed("CUSTOMER")
    public Response getOrders(
            @BeanParam final PageRequest pagination
    ) {
        UUID customerId = authService.getCurrentUserId();
        log.info("Get customer request: customerId={}", customerId);
        List<Order> result = orderService.getByCustomerId(
                customerId,
                pagination.getPage(),
                pagination.getSize());
        return Response.ok(orderMapper.toDtoList(result)).build();
    }


}
