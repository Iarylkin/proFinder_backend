package fit.biejk.dto;

import fit.biejk.entity.OrderResponseStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

/**
 * Data Transfer Object representing a response related to an order.
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
     * Identifier of the associated order.
     */
    private UUID orderId;

    /**
     * Identifier of the professional making the response.
     */
    private UUID professionalId;

    /**
     * Description of the response. Must not be blank.
     */
    @NotBlank
    private String description;

    /**
     * Proposed price. Must not be null.
     */
    @NotNull
    private int price;

    /**
     * Current status of the response.
     */
    private OrderResponseStatus status;
}
