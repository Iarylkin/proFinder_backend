package fit.biejk.dto;

import fit.biejk.entity.OrderResponseStatus;
import fit.biejk.validation.Limits;
import fit.biejk.validation.NoControlChars;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
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
     * Description of the response. Must not be blank.
     */
    @NotBlank
    @Size(max = Limits.RESPONSE_DESCRIPTION_MAX_LENGTH)
    @NoControlChars(multiline = true)
    private String description;

    /**
     * Proposed price. Must not be null.
     */
    @Min(0)
    @Max(Limits.PRICE_MAX)
    private int price;

    /**
     * Current status of the response.
     */
    private OrderResponseStatus status;
}
