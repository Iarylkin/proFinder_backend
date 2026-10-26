package fit.biejk.resource;

import fit.biejk.dto.ConfirmProposal;
import fit.biejk.entity.Order;
import fit.biejk.entity.OrderProposal;
import fit.biejk.mapper.OrderMapper;
import fit.biejk.mapper.OrderProposalMapper;
import fit.biejk.service.OrderProposalService;
import jakarta.annotation.security.RolesAllowed;
import jakarta.validation.Valid;
import jakarta.ws.rs.*;

import jakarta.ws.rs.core.Response;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * REST resource for managing order proposals submitted by specialists.
 * <p>
 * This resource allows retrieval of proposals by specialist, by proposal ID, or by order ID.
 * It also supports confirming proposals by the client and fetching related order data.
 * </p>
 */
@Slf4j
@Path("/v1/proposals")
@RequiredArgsConstructor
public class OrderProposalResource {

    /**
     * Service responsible for handling business logic related to order proposals.
     */
    private final OrderProposalService orderProposalService;

    /**
     * Mapper for converting between OrderProposal entities and DTOs.
     */
    private final OrderProposalMapper orderProposalMapper;

    /**
     * Mapper for converting between Order entities and DTOs.
     */
    private final OrderMapper orderMapper;

    /**
     * Retrieves a specific proposal by its ID.
     * Available only to the owner of the order and the specialist who made the proposal.
     *
     * @param proposalId the ID of the proposal
     * @return the corresponding proposal
     */
    @GET
    @Path("/{proposalId}")
    @RolesAllowed({"CLIENT", "SPECIALIST"})
    public Response getById(@PathParam("proposalId") final Long proposalId) {
        log.info("Get proposal: proposalId={}", proposalId);
        OrderProposal result = orderProposalService.getByIdForCurrentUser(proposalId);
        return Response.ok(orderProposalMapper.toDto(result)).build();
    }

    /**
     * Confirms an order proposal and transitions the order to the next stage.
     * <p>
     * This endpoint allows a client to accept a specific specialist's proposal.
     * By confirming, the system updates the order with the final agreed-upon terms
     * and assigns the specialist to the project.
     * </p>
     *
     * @param proposalId the unique ID of the proposal to be confirmed
     * @param confirm the DTO containing final price and deadline details
     * @return order
     */
    @POST
    @Path("/{proposalId}/confirm")
    @RolesAllowed("CLIENT")
    public Response confirm(@PathParam("proposalId") final Long proposalId,
                            @Valid final ConfirmProposal confirm) {

        Order result = orderProposalService.confirmAndAssign(proposalId, confirm);
        return Response.ok(orderMapper.toDto(result)).build();
    }
}
