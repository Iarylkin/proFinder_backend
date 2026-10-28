package fit.biejk.resource;

import fit.biejk.dto.OrderResponseUpdateRequest;
import fit.biejk.entity.Order;
import fit.biejk.entity.OrderResponse;
import fit.biejk.mapper.OrderMapper;
import fit.biejk.mapper.OrderResponseMapper;
import fit.biejk.service.OrderResponseService;
import jakarta.annotation.security.RolesAllowed;
import jakarta.validation.Valid;
import jakarta.ws.rs.*;

import jakarta.ws.rs.core.Response;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * REST resource for managing order responses submitted by professionals.
 * <p>
 * This resource allows retrieval of responses by professional, by response ID, or by order ID.
 * It also supports approving responses by the customer and fetching related order data.
 * </p>
 */
@Slf4j
@Path("/v1/responses")
@RequiredArgsConstructor
public class OrderResponseResource {

    /**
     * Service responsible for handling business logic related to order responses.
     */
    private final OrderResponseService orderResponseService;

    /**
     * Mapper for converting between OrderResponse entities and DTOs.
     */
    private final OrderResponseMapper orderResponseMapper;

    /**
     * Mapper for converting between Order entities and DTOs.
     */
    private final OrderMapper orderMapper;

    /**
     * Retrieves a specific response by its ID.
     * Available only to the owner of the order and the professional who made the response.
     *
     * @param responseId the ID of the response
     * @return the corresponding response
     */
    @GET
    @Path("/{responseId}")
    @RolesAllowed({"CUSTOMER", "PROFESSIONAL"})
    public Response getById(@PathParam("responseId") final Long responseId) {
        log.info("Get response: responseId={}", responseId);
        OrderResponse result = orderResponseService.getByIdForCurrentUser(responseId);
        return Response.ok(orderResponseMapper.toDto(result)).build();
    }

    /**
     * Accepts an order response and transitions the order to the next stage.
     * <p>
     * This endpoint allows a customer to accept a specific professional's response by sending
     * {@code "status": "APPROVED"} together with the final price and deadline.
     * By approving, the system updates the order with the final agreed-upon terms
     * and assigns the professional to the project.
     * </p>
     *
     * @param responseId the unique ID of the response to be accepted
     * @param request the status, final price and deadline
     * @return order
     */
    @PATCH
    @Path("/{responseId}")
    @RolesAllowed("CUSTOMER")
    public Response update(@PathParam("responseId") final Long responseId,
                            @Valid final OrderResponseUpdateRequest request) {

        Order result = orderResponseService.updateStatus(
                responseId, request.getStatus(), request.getFinalPrice(), request.getFinalDeadline());
        return Response.ok(orderMapper.toDto(result)).build();
    }
}
