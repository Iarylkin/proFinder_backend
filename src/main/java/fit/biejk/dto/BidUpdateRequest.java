package fit.biejk.dto;

import fit.biejk.entity.BidStatus;
import fit.biejk.validation.Limits;
import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Data Transfer Object representing a status change of a bid, such as its approval.
 * <p>
 * Contains the final agreed deadline and price for a given bid.
 * </p>
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class BidUpdateRequest {

    /**
     * Status to set on the bid. Only {@code APPROVED} is supported.
     */
    @NotNull(message = "Status cannot be null")
    private BidStatus status;
    /**
     * The final agreed deadline for the bid.
     * Must not be null.
     */
    @NotNull(message = "Final deadline cannot be null")
    @FutureOrPresent
    private LocalDateTime finalDeadline;

    /**
     * The final agreed price for the bid.
     * Must be a non-negative integer.
     */
    @Min(value = 0, message = "Final price must be non-negative")
    @Max(Limits.PRICE_MAX)
    private int finalPrice;

    /**
     * The version the client has seen. A different current version means someone changed the resource since,
     * and the request is rejected with 409.
     */
    @NotNull
    @PositiveOrZero
    private Long version;
}
