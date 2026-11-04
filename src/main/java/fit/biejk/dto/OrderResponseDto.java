package fit.biejk.dto;

import fit.biejk.entity.OrderResponseStatus;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

/**
 * Data Transfer Object representing a response related to an order, sent to the client. A response is created
 * with {@link OrderResponseCreateRequest}.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class OrderResponseDto {

    /**
     * Unique identifier of the response.
     */
    private UUID id;

    /**
     * Version of the resource for optimistic locking: the client sends it back in a {@code PATCH}.
     */
    private Long version;

    /**
     * Identifier of the associated order.
     */
    private UUID orderId;

    /**
     * Identifier of the professional making the response.
     */
    private UUID professionalId;

    /**
     * Description of the response.
     */
    private String description;

    /**
     * Proposed price.
     */
    private int price;

    /**
     * Current status of the response.
     */
    private OrderResponseStatus status;
}
