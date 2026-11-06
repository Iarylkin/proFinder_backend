package io.github.strogolsky.profinder.core.dto;

import io.github.strogolsky.profinder.common.validation.Limits;
import io.github.strogolsky.profinder.common.validation.NoControlChars;
import io.github.strogolsky.profinder.common.validation.Text;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Request of a professional to place a bid on an order. The identifier, order, professional, status and
 * version are set by the server.
 *
 * @param description Description of the bid. Must not be blank.
 * @param price Price offered by the professional.
 */
public record BidCreateRequest(
        @NotBlank
        @Size(max = Limits.BID_DESCRIPTION_MAX_LENGTH)
        @NoControlChars(multiline = true)
        String description,
        @NotNull(message = "Price is required")
        @Min(0)
        @Max(Limits.PRICE_MAX)
        Integer price) {

    /**
     * Cleans the text fields: the white space around a text is removed and an empty text is {@code null}.
     */
    public BidCreateRequest {
        description = Text.clean(description);
    }
}
