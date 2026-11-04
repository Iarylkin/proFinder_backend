package fit.biejk.resource;

import fit.biejk.dto.PageRequest;
import fit.biejk.dto.ProfessionalUpdateRequest;
import fit.biejk.dto.ReviewCreateRequest;
import fit.biejk.entity.*;
import fit.biejk.mapper.OrderMapper;
import fit.biejk.mapper.BidMapper;
import fit.biejk.mapper.ProfessionalMapper;
import fit.biejk.mapper.ReviewMapper;
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
 * REST resource for managing professionals.
 * <p>
 * Provides endpoints for retrieving, updating, and deleting professional data,
 * as well as accessing and modifying the current professional's profile.
 * </p>
 */
@Path("/v1/professionals")
@Slf4j
@RequiredArgsConstructor
public class ProfessionalResource {

    /**
     * Service responsible for handling business logic related to bids.
     */
    private final BidService bidService;

    /**
     * Service for managing order-related operations.
     */
    private final OrderService orderService;

    /**
     * Mapper for converting between Order entities and DTOs.
     */
    private final OrderMapper orderMapper;

    /**
     * Mapper for converting between Bid entities and DTOs.
     */
    private final BidMapper bidMapper;

    /** Service for handling business logic related to professionals. */
    private final ProfessionalService professionalService;

    /** Mapper for converting between Professional entities and DTOs. */
    private final ProfessionalMapper professionalMapper;

    /** Service for retrieving authenticated user context and identity. */
    private final AuthService authService;

    /** Mapper for converting between Review and ReviewDto. */
    private final ReviewMapper reviewMapper;

    /** Service for accessing customer reviews. */
    private final ReviewService reviewService;

    /**
     * Retrieves a professional by their ID.
     *
     * @param professionalId ID of the professional
     * @return HTTP response containing the professional
     */
    @GET
    @Path("/{professionalId}")
    @PermitAll
    public Response getById(@PathParam("professionalId") final UUID professionalId) {
        log.info("Get professional by ID={}", professionalId);
        ProfessionalProfile result = professionalService.getById(professionalId);
        return Response.ok(professionalMapper.toPublicDto(result)).build();
    }

    /**
     * Returns the profile of the currently authenticated professional.
     *
     * @return HTTP response containing the professional
     */
    @GET
    @Path("/me")
    @RolesAllowed("PROFESSIONAL")
    public Response getMe() {
        UUID id = authService.getCurrentUserId();
        log.info("Get profile for professional ID={}", id);
        return Response.ok(professionalMapper.toDto(professionalService.getById(id))).build();
    }

    /**
     * Partially updates the profile of the currently authenticated professional: only the fields that are sent
     * change.
     *
     * @param request the fields to change
     * @return HTTP response containing the updated professional
     */
    @PATCH
    @Path("/me")
    @RolesAllowed("PROFESSIONAL")
    public Response updateMe(@Valid final ProfessionalUpdateRequest request) {
        UUID id = authService.getCurrentUserId();
        log.info("Update profile for professional ID={}", id);
        ProfessionalProfile result = professionalService.updateProfile(id, professionalMapper.toEntity(request));
        return Response.ok(professionalMapper.toDto(result)).build();
    }

    /**
     * Deletes the profile of the currently authenticated professional.
     *
     * @return HTTP response
     */
    @DELETE
    @Path("/me")
    @RolesAllowed("PROFESSIONAL")
    public Response deleteProfile() {
        UUID id = authService.getCurrentUserId();
        log.info("Delete profile for professional ID={}", id);
        professionalService.delete(id);
        return Response.noContent().build();
    }

    /**
     * Retrieves all reviews submitted by id professional.
     *
     * @param professionalId professional ID
     * @param pagination   the pagination parameters (page and size)
     * @return HTTP response containing list of reviews
     */
    @GET
    @Path("/{professionalId}/reviews")
    @PermitAll
    public Response getReviews(
            @PathParam("professionalId") final UUID professionalId,
            @BeanParam final PageRequest pagination) {
        List<Review> res = reviewService.getByProfessionalId(
                professionalId,
                pagination.getPage(),
                pagination.getSize());
        return Response.ok(reviewMapper.toDtoList(res)).build();
    }

    /**
     * Creates a review for the given professional.
     *
     * @param professionalId professional ID
     * @param request review data
     * @return HTTP 200 with saved review
     */
    @POST
    @Path("/{professionalId}/reviews")
    @RolesAllowed("CUSTOMER")
    public Response createReview(@PathParam("professionalId") final UUID professionalId,
                                 @Valid final ReviewCreateRequest request
    ) {
        Review review = reviewMapper.toEntity(request);
        Review result = professionalService.review(professionalId, review);

        return Response.status(Response.Status.CREATED).entity(reviewMapper.toDto(result)).build();
    }

    /**
     * Retrieves all bids submitted by the specified professional.
     *
     * @param professionalId the ID of the professional
     * @param pagination   the pagination parameters (page and size)
     * @return list of bids submitted by the professional
     */
    @GET
    @Path("/{professionalId}/bids")
    @RolesAllowed("PROFESSIONAL")
    public Response getResponses(
            @PathParam("professionalId") final UUID professionalId,
            @BeanParam final PageRequest pagination
    ) {
        List<Bid> result = bidService.getByProfessionalId(
                professionalId,
                pagination.getPage(),
                pagination.getSize());
        return Response.ok(bidMapper.toDtoList(result)).build();
    }

    /**
     * Retrieves all active orders assigned to a specific professional.
     * <p>
     * Only accessible to authenticated users with the PROFESSIONAL role.
     * </p>
     *
     * @param pagination the pagination parameters (page and size)
     * @return list of orders currently assigned to the professional
     */
    @GET
    @Path("/me/orders")
    @RolesAllowed("PROFESSIONAL")
    public Response getOrders(
            @BeanParam final PageRequest pagination
    ) {
        UUID professionalId = authService.getCurrentUserId();
        log.info("Get assigned by professional id: professionalId={}", professionalId);
        List<Order> result = orderService.getByProfessionalId(
                professionalId,
                pagination.getPage(),
                pagination.getSize()
        );
        return Response.ok(orderMapper.toDtoList(result)).build();
    }
}
