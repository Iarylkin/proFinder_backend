package fit.biejk.resource;

import fit.biejk.dto.PageRequest;
import fit.biejk.dto.ReviewDto;
import fit.biejk.dto.SpecialistDto;
import fit.biejk.dto.SpecialistFilterCriteria;
import fit.biejk.entity.*;
import fit.biejk.mapper.OrderMapper;
import fit.biejk.mapper.OrderProposalMapper;
import fit.biejk.mapper.ReviewMapper;
import fit.biejk.mapper.SpecialistMapper;
import fit.biejk.search.SpecialistSearchMapper;
import fit.biejk.search.SpecialistSearchService;
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
 * REST resource for managing specialists.
 * <p>
 * Provides endpoints for retrieving, updating, and deleting specialist data,
 * as well as accessing and modifying the current specialist's profile.
 * </p>
 */
@Path("/v1/specialists")
@Slf4j
@RequiredArgsConstructor
public class SpecialistResource {

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
     * Service for searching and filtering specialists.
     */
    private final SpecialistSearchService specialistSearchService;

    /**
     * Mapper for converting specialist search results to DTOs.
     */
    private final SpecialistSearchMapper specialistSearchMapper;

    /**
     * Mapper for converting between OrderProposal entities and DTOs.
     */
    private final OrderProposalMapper orderProposalMapper;

    /** Service for handling business logic related to specialists. */
    private final SpecialistService specialistService;

    /** Mapper for converting between Specialist entities and DTOs. */
    private final SpecialistMapper specialistMapper;

    /** Service for retrieving authenticated user context and identity. */
    private final AuthService authService;

    /** Mapper for converting between Review and ReviewDto. */
    private final ReviewMapper reviewMapper;

    /** Service for accessing client reviews. */
    private final ReviewService reviewService;

    /**
     * Retrieves a list of all specialists.
     *
     * @param criteria   the filtering criteria for searching specialists
     * @param pagination the pagination parameters (page and size)
     * @return HTTP response containing a list of all specialists
     */
    @GET
    @PermitAll
    public Response getSpecialists(
            @BeanParam final SpecialistFilterCriteria criteria,
            @BeanParam final PageRequest pagination
    ) {

        if (criteria.hasFilters()) {
            var searchResults = specialistSearchService.search(
                    criteria.getQuery(),
                    criteria.getLocation(),
                    pagination.getPage(),
                    pagination.getSize()
            );
            var result = specialistSearchMapper.toEntityList(searchResults);
            return Response.ok(specialistMapper.toPublicDtoList(result)).build();
        }

        log.info("Get all specialists");
        List<Specialist> result = specialistService.getAll(
                pagination.getPage(),
                pagination.getSize()
        );
        log.debug("Found {} specialists", result.size());
        return Response.ok(specialistMapper.toPublicDtoList(result)).build();
    }

    /**
     * Retrieves a specialist by their ID.
     *
     * @param specialistId ID of the specialist
     * @return HTTP response containing the specialist
     */
    @GET
    @Path("/{specialistId}")
    @PermitAll
    public Response getById(@PathParam("specialistId") final Long specialistId) {
        log.info("Get specialist by ID={}", specialistId);
        Specialist result = specialistService.getById(specialistId);
        return Response.ok(specialistMapper.toPublicDto(result)).build();
    }

    /**
     * Updates a specialist by ID.
     * Currently disabled with {@code @DenyAll}.
     *
     * @param specialistId  ID of the specialist to update
     * @param dto updated data
     * @return HTTP response with updated specialist
     */
    @PUT
    @Path("/{specialistId}")
    @DenyAll
    public Response update(@PathParam("specialistId") final Long specialistId, @Valid final SpecialistDto dto) {
        log.info("Update specialist request: ID={}, dto={}", specialistId, dto);
        Specialist entity = specialistMapper.toEntity(dto);
        Specialist result = specialistService.update(specialistId, entity);
        log.debug("Specialist updated with ID={}", result.getId());
        return Response.ok(specialistMapper.toDto(result)).build();
    }

    /**
     * Deletes a specialist by ID.
     * Currently disabled with {@code @DenyAll}.
     *
     * @param specialistId ID of the specialist to delete
     * @return HTTP response
     */
    @DELETE
    @Path("/{specialistId}")
    @DenyAll
    public Response delete(@PathParam("specialistId") final Long specialistId) {
        log.info("Delete specialist request: ID={}", specialistId);
        specialistService.delete(specialistId);
        return Response.ok().build();
    }

    /**
     * Retrieves the profile of the currently authenticated specialist.
     *
     * @return HTTP response containing the specialist's profile
     */
    @GET
    @Path("/me")
    @RolesAllowed("SPECIALIST")
    public Response getProfile() {
        Long specialistId = authService.getCurrentUserId();
        log.info("Get profile for specialist ID={}", specialistId);
        Specialist specialist = specialistService.getById(specialistId);
        return Response.ok(specialistMapper.toDto(specialist)).build();
    }

    /**
     * Updates the profile of the currently authenticated specialist.
     *
     * @param dto updated profile data
     * @return HTTP response with updated profile
     */
    @PUT
    @Path("/me")
    @RolesAllowed("SPECIALIST")
    public Response updateProfile(@Valid final SpecialistDto dto) {
        Long id = authService.getCurrentUserId();
        log.info("Update profile for specialist ID={} with dto={}", id, dto);
        Specialist specialist = specialistService.update(id, specialistMapper.toEntity(dto));
        log.debug("Specialist profile updated, ID={}", specialist.getId());
        return Response.ok(specialistMapper.toDto(specialist)).build();
    }

    /**
     * Deletes the profile of the currently authenticated specialist.
     *
     * @return HTTP response
     */
    @DELETE
    @Path("/me")
    @RolesAllowed("SPECIALIST")
    public Response deleteProfile() {
        Long id = authService.getCurrentUserId();
        log.info("Delete profile for specialist ID={}", id);
        specialistService.delete(id);
        return Response.ok().build();
    }

    /**
     * Retrieves all reviews submitted by id specialist.
     *
     * @param specialistId specialist ID
     * @param pagination   the pagination parameters (page and size)
     * @return HTTP response containing list of reviews
     */
    @GET
    @Path("/{specialistId}/reviews")
    @PermitAll
    public Response getReviews(
            @PathParam("specialistId") final Long specialistId,
            @BeanParam final PageRequest pagination) {
        List<Review> res = reviewService.getBySpecialistId(
                specialistId,
                pagination.getPage(),
                pagination.getSize());
        return Response.ok(reviewMapper.toDtoList(res)).build();
    }

    /**
     * Updates the list of service offerings for the currently authenticated specialist.
     *
     * @param serviceOfferings list of updated service offerings
     * @return HTTP response with updated specialist data
     */
    @PUT
    @Path("/me/services")
    @RolesAllowed("SPECIALIST")
    public Response updateServiceOffering(final List<ServiceOffering> serviceOfferings) {
        Long id = authService.getCurrentUserId();
        log.info("Add serviceOffering for specialist ID={}", id);
        Specialist specialist = specialistService.updateServiceOfferings(id, serviceOfferings);
        return Response.ok(specialistMapper.toDto(specialist)).build();
    }

    /**
     * Creates a review for the given specialist.
     *
     * @param specialistId specialist ID
     * @param dto review data
     * @return HTTP 200 with saved review
     */
    @POST
    @Path("/{specialistId}/reviews")
    @RolesAllowed("CLIENT")
    public Response createReview(@PathParam("specialistId") final Long specialistId,
                                 @Valid final ReviewDto dto
    ) {
        Review review = reviewMapper.toEntity(dto);
        Review result = specialistService.review(specialistId, review);

        return Response.status(Response.Status.CREATED).entity(reviewMapper.toDto(result)).build();
    }

    /**
     * Retrieves all proposals submitted by the specified specialist.
     *
     * @param specialistId the ID of the specialist
     * @param pagination   the pagination parameters (page and size)
     * @return list of proposals submitted by the specialist
     */
    @GET
    @Path("/{specialistId}/proposals")
    @RolesAllowed("SPECIALIST")
    public Response getProposals(
            @PathParam("specialistId") final Long specialistId,
            @BeanParam final PageRequest pagination
    ) {
        List<OrderProposal> result = orderProposalService.getBySpecialistId(
                specialistId,
                pagination.getPage(),
                pagination.getSize());
        return Response.ok(orderProposalMapper.toDtoList(result)).build();
    }

    /**
     * Retrieves all active orders assigned to a specific specialist.
     * <p>
     * Only accessible to authenticated users with the SPECIALIST role.
     * </p>
     *
     * @param pagination the pagination parameters (page and size)
     * @return list of orders currently assigned to the specialist
     */
    @GET
    @Path("/me/orders")
    @RolesAllowed("SPECIALIST")
    public Response getOrders(
            @BeanParam final PageRequest pagination
    ) {
        Long specialistId = authService.getCurrentUserId();
        log.info("Get assigned by specialist id: specialistId={}", specialistId);
        List<Order> result = orderService.getBySpecialistId(
                specialistId,
                pagination.getPage(),
                pagination.getSize()
        );
        return Response.ok(orderMapper.toDtoList(result)).build();
    }
}
