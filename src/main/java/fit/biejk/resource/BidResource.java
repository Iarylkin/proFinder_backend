package fit.biejk.resource;

import fit.biejk.dto.BidUpdateRequest;
import fit.biejk.entity.Order;
import fit.biejk.entity.Bid;
import fit.biejk.mapper.OrderMapper;
import fit.biejk.mapper.BidMapper;
import fit.biejk.service.BidService;
import jakarta.annotation.security.RolesAllowed;
import jakarta.validation.Valid;
import jakarta.ws.rs.*;

import jakarta.ws.rs.core.Response;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.UUID;

/**
 * REST resource for managing order bids submitted by professionals.
 * <p>
 * This resource allows retrieval of bids by professional, by bid ID, or by order ID.
 * It also supports approving bids by the customer and fetching related order data.
 * </p>
 */
@Slf4j
@Path("/v1/bids")
@RequiredArgsConstructor
public class BidResource {

    /**
     * Service responsible for handling business logic related to bids.
     */
    private final BidService bidService;

    /**
     * Mapper for converting between Bid entities and DTOs.
     */
    private final BidMapper bidMapper;

    /**
     * Mapper for converting between Order entities and DTOs.
     */
    private final OrderMapper orderMapper;

    /**
     * Retrieves a specific bid by its ID.
     * Available only to the owner of the order and the professional who made the bid.
     *
     * @param bidId the ID of the bid
     * @return the corresponding bid
     */
    @GET
    @Path("/{bidId}")
    @RolesAllowed({"CUSTOMER", "PROFESSIONAL"})
    public Response getById(@PathParam("bidId") final UUID bidId) {
        log.info("Get bid: bidId={}", bidId);
        Bid result = bidService.getByIdForCurrentUser(bidId);
        return Response.ok(bidMapper.toDto(result)).build();
    }

    /**
     * Accepts an order bid and transitions the order to the next stage.
     * <p>
     * This endpoint allows a customer to accept a specific professional's bid by sending
     * {@code "status": "APPROVED"} together with the final price and deadline.
     * By approving, the system updates the order with the final agreed-upon terms
     * and assigns the professional to the project.
     * </p>
     *
     * @param bidId the unique ID of the bid to be accepted
     * @param request the status, final price and deadline
     * @return order
     */
    @PATCH
    @Path("/{bidId}")
    @RolesAllowed("CUSTOMER")
    public Response update(@PathParam("bidId") final UUID bidId,
                            @Valid final BidUpdateRequest request) {

        Order result = bidService.updateStatus(
                bidId, request.status(), request.finalPrice(), request.finalDeadline(),
                request.version());
        return Response.ok(orderMapper.toDto(result)).build();
    }
}
