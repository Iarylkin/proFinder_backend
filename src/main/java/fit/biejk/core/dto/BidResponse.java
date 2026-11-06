package fit.biejk.core.dto;

import fit.biejk.core.entity.BidStatus;

import java.util.UUID;

/**
 * Represents a response related to an order, sent to the client. A response is created with {@link
 * BidCreateRequest}.
 *
 * @param id Unique identifier of the response.
 * @param version Version of the resource for optimistic locking: the client sends it back in a {@code PATCH}.
 * @param orderId Identifier of the associated order.
 * @param professionalId Identifier of the professional making the response.
 * @param description Description of the response.
 * @param price Proposed price.
 * @param status Current status of the response.
 */
public record BidResponse(
        UUID id,
        Long version,
        UUID orderId,
        UUID professionalId,
        String description,
        int price,
        BidStatus status) {
}
