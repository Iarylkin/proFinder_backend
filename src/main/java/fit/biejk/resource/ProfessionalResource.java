package fit.biejk.resource;

import fit.biejk.dto.PageRequest;
import fit.biejk.dto.ReviewDto;
import fit.biejk.dto.ProfessionalDto;
import fit.biejk.dto.ProfessionalFilterCriteria;
import fit.biejk.entity.*;
import fit.biejk.mapper.OrderMapper;
import fit.biejk.mapper.OrderProposalMapper;
import fit.biejk.mapper.ReviewMapper;
import fit.biejk.mapper.ProfessionalMapper;
import fit.biejk.search.ProfessionalSearchMapper;
import fit.biejk.search.ProfessionalSearchService;
import fit.biejk.service.*;
import jakarta.annotation.security.DenyAll;
import jakarta.annotation.security.PermitAll;
import jakarta.annotation.security.RolesAllowed;
import jakarta.validation.Valid;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.Response;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.List;

/**
 * REST resource for managing professionals.
 * <p>
 * Provides endpoints for retrieving, updating, and deleting professional data,
 * as well as accessing and modifying the current professional's profile.
 * </p>
 */
@Path("/v1/specialists")
@Slf4j
@RequiredArgsConstructor
public class ProfessionalResource {

    /**
     * Service responsible for handling business logic related to order proposals.
     */
    private final OrderProposalService orderProposalService;

    /**
     * Service for managing order-related operations.
     */
    private final OrderService orderService;

    /**
     * Mapper for converting between Order entities and DTOs.
     */
    private final OrderMapper orderMapper;

    /**
     * Service for searching and filtering professionals.
     */
    private final ProfessionalSearchService professionalSearchService;

    /**
     * Mapper for converting professional search results to DTOs.
     */
    private final ProfessionalSearchMapper professionalSearchMapper;

    /**
     * Mapper for converting between OrderProposal entities and DTOs.
     */
    private final OrderProposalMapper orderProposalMapper;

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
     * Retrieves a list of all professionals.
     *
     * @param criteria   the filtering criteria for searching professionals
     * @param pagination the pagination parameters (page and size)
     * @return HTTP response containing a list of all professionals
     */
    @GET
    @PermitAll
    public Response getProfessionals(
            @BeanParam final ProfessionalFilterCriteria criteria,
            @BeanParam final PageRequest pagination
    ) {

        if (criteria.hasFilters()) {
            var searchResults = professionalSearchService.search(
                    criteria.getQuery(),
                    criteria.getLocation(),
                    pagination.getPage(),
                    pagination.getSize()
            );
            var result = professionalSearchMapper.toEntityList(searchResults);
            return Response.ok(professionalMapper.toPublicDtoList(result)).build();
        }

        log.info("Get all professionals");
        List<Professional> result = professionalService.getAll(
                pagination.getPage(),
                pagination.getSize()
        );
        log.debug("Found {} professionals", result.size());
        return Response.ok(professionalMapper.toPublicDtoList(result)).build();
    }

    /**
     * Retrieves a professional by their ID.
     *
     * @param professionalId ID of the professional
     * @return HTTP response containing the professional
     */
    @GET
    @Path("/{professionalId}")
    @PermitAll
    public Response getById(@PathParam("professionalId") final Long professionalId) {
        log.info("Get professional by ID={}", professionalId);
        Professional result = professionalService.getById(professionalId);
        return Response.ok(professionalMapper.toPublicDto(result)).build();
    }

    /**
     * Updates a professional by ID.
     * Currently disabled with {@code @DenyAll}.
     *
     * @param professionalId  ID of the professional to update
     * @param dto updated data
     * @return HTTP response with updated professional
     */
    @PUT
    @Path("/{professionalId}")
    @DenyAll
    public Response update(@PathParam("professionalId") final Long professionalId, @Valid final ProfessionalDto dto) {
        log.info("Update professional request: ID={}, dto={}", professionalId, dto);
        Professional entity = professionalMapper.toEntity(dto);
        Professional result = professionalService.update(professionalId, entity);
        log.debug("Professional updated with ID={}", result.getId());
        return Response.ok(professionalMapper.toDto(result)).build();
    }

    /**
     * Deletes a professional by ID.
     * Currently disabled with {@code @DenyAll}.
     *
     * @param professionalId ID of the professional to delete
     * @return HTTP response
     */
    @DELETE
    @Path("/{professionalId}")
    @DenyAll
    public Response delete(@PathParam("professionalId") final Long professionalId) {
        log.info("Delete professional request: ID={}", professionalId);
        professionalService.delete(professionalId);
        return Response.ok().build();
    }

    /**
     * Retrieves the profile of the currently authenticated professional.
     *
     * @return HTTP response containing the professional's profile
     */
    @GET
    @Path("/me")
    @RolesAllowed("PROFESSIONAL")
    public Response getProfile() {
        Long professionalId = authService.getCurrentUserId();
        log.info("Get profile for professional ID={}", professionalId);
        Professional professional = professionalService.getById(professionalId);
        return Response.ok(professionalMapper.toDto(professional)).build();
    }

    /**
     * Updates the profile of the currently authenticated professional.
     *
     * @param dto updated profile data
     * @return HTTP response with updated profile
     */
    @PUT
    @Path("/me")
    @RolesAllowed("PROFESSIONAL")
    public Response updateProfile(@Valid final ProfessionalDto dto) {
        Long id = authService.getCurrentUserId();
        log.info("Update profile for professional ID={} with dto={}", id, dto);
        Professional professional = professionalService.update(id, professionalMapper.toEntity(dto));
        log.debug("Professional profile updated, ID={}", professional.getId());
        return Response.ok(professionalMapper.toDto(professional)).build();
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
        Long id = authService.getCurrentUserId();
        log.info("Delete profile for professional ID={}", id);
        professionalService.delete(id);
        return Response.ok().build();
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
            @PathParam("professionalId") final Long professionalId,
            @BeanParam final PageRequest pagination) {
        List<Review> res = reviewService.getByProfessionalId(
                professionalId,
                pagination.getPage(),
                pagination.getSize());
        return Response.ok(reviewMapper.toDtoList(res)).build();
    }

    /**
     * Updates the list of service offerings for the currently authenticated professional.
     *
     * @param serviceOfferings list of updated service offerings
     * @return HTTP response with updated professional data
     */
    @PUT
    @Path("/me/services")
    @RolesAllowed("PROFESSIONAL")
    public Response updateServiceOffering(final List<ServiceOffering> serviceOfferings) {
        Long id = authService.getCurrentUserId();
        log.info("Add serviceOffering for professional ID={}", id);
        Professional professional = professionalService.updateServiceOfferings(id, serviceOfferings);
        return Response.ok(professionalMapper.toDto(professional)).build();
    }

    /**
     * Creates a review for the given professional.
     *
     * @param professionalId professional ID
     * @param dto review data
     * @return HTTP 200 with saved review
     */
    @POST
    @Path("/{professionalId}/reviews")
    @RolesAllowed("CUSTOMER")
    public Response createReview(@PathParam("professionalId") final Long professionalId,
                                 @Valid final ReviewDto dto
    ) {
        Review review = reviewMapper.toEntity(dto);
        Review result = professionalService.review(professionalId, review);

        return Response.status(Response.Status.CREATED).entity(reviewMapper.toDto(result)).build();
    }

    /**
     * Retrieves all proposals submitted by the specified professional.
     *
     * @param professionalId the ID of the professional
     * @param pagination   the pagination parameters (page and size)
     * @return list of proposals submitted by the professional
     */
    @GET
    @Path("/{professionalId}/proposals")
    @RolesAllowed("PROFESSIONAL")
    public Response getProposals(
            @PathParam("professionalId") final Long professionalId,
            @BeanParam final PageRequest pagination
    ) {
        List<OrderProposal> result = orderProposalService.getByProfessionalId(
                professionalId,
                pagination.getPage(),
                pagination.getSize());
        return Response.ok(orderProposalMapper.toDtoList(result)).build();
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
        Long professionalId = authService.getCurrentUserId();
        log.info("Get assigned by professional id: professionalId={}", professionalId);
        List<Order> result = orderService.getByProfessionalId(
                professionalId,
                pagination.getPage(),
                pagination.getSize()
        );
        return Response.ok(orderMapper.toDtoList(result)).build();
    }
}
