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
 * REST resource for managing order proposals submitted by professionals.
 * <p>
 * This resource allows retrieval of proposals by professional, by proposal ID, or by order ID.
 * It also supports confirming proposals by the customer and fetching related order data.
 * </p>
 */
@Slf4j
@Path("/v1/responses")
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
     * Available only to the owner of the order and the professional who made the proposal.
     *
     * @param proposalId the ID of the proposal
     * @return the corresponding proposal
     */
    @GET
    @Path("/{proposalId}")
    @RolesAllowed({"CUSTOMER", "PROFESSIONAL"})
    public Response getById(@PathParam("proposalId") final Long proposalId) {
        log.info("Get proposal: proposalId={}", proposalId);
        OrderProposal result = orderProposalService.getByIdForCurrentUser(proposalId);
        return Response.ok(orderProposalMapper.toDto(result)).build();
    }

    /**
     * Accepts an order response and transitions the order to the next stage.
     * <p>
     * This endpoint allows a customer to accept a specific professional's response by sending
     * {@code "status": "APPROVED"} together with the final price and deadline.
     * By confirming, the system updates the order with the final agreed-upon terms
     * and assigns the professional to the project.
     * </p>
     *
     * @param proposalId the unique ID of the response to be accepted
     * @param confirm the DTO containing the status, final price and deadline details
     * @return order
     */
    @PATCH
    @Path("/{proposalId}")
    @RolesAllowed("CUSTOMER")
    public Response confirm(@PathParam("proposalId") final Long proposalId,
                            @Valid final ConfirmProposal confirm) {

        Order result = orderProposalService.confirmAndAssign(proposalId, confirm);
        return Response.ok(orderMapper.toDto(result)).build();
    }
}
