package io.github.strogolsky.profinder.core.dto;

import io.github.strogolsky.profinder.common.validation.Limits;
import io.github.strogolsky.profinder.core.entity.BidStatus;
import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

import java.time.LocalDateTime;

/**
 * Represents a status change of a bid, such as its approval. Contains the final agreed deadline and price for
 * a given bid.
 *
 * @param status Status to set on the bid. Only {@code APPROVED} is supported.
 * @param finalDeadline The final agreed deadline for the bid. Must not be null.
 * @param finalPrice The final agreed price for the bid. Must be a non-negative integer.
 * @param version The version the client has seen. A different current version means someone changed the resource
 *        since, and the request is rejected with 409.
 */
public record BidUpdateRequest(
        @NotNull(message = "Status cannot be null")
        BidStatus status,
        @NotNull(message = "Final deadline cannot be null")
        @FutureOrPresent
        LocalDateTime finalDeadline,
        @Min(value = 0, message = "Final price must be non-negative")
        @Max(Limits.PRICE_MAX)
        int finalPrice,
        @NotNull
        @PositiveOrZero
        Long version) {
}
