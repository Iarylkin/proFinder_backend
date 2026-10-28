package fit.biejk.dto;

import fit.biejk.entity.OrderResponseStatus;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Data Transfer Object representing the confirmation of a response.
 * <p>
 * Contains the final agreed deadline and price for a given response.
 * </p>
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class OrderResponseUpdateRequest {

    /**
     * Status to set on the response. Only {@code APPROVED} is supported.
     */
    @NotNull(message = "Status cannot be null")
    private OrderResponseStatus status;
    /**
     * The final agreed deadline for the response.
     * Must not be null.
     */
    @NotNull(message = "Final deadline cannot be null")
    private LocalDateTime finalDeadline;

    /**
     * The final agreed price for the response.
     * Must be a non-negative integer.
     */
    @Min(value = 0, message = "Final price must be non-negative")
    private int finalPrice;
}
