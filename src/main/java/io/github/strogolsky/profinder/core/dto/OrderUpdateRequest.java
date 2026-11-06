package io.github.strogolsky.profinder.core.dto;

import io.github.strogolsky.profinder.common.validation.Limits;
import io.github.strogolsky.profinder.common.validation.NoControlChars;
import io.github.strogolsky.profinder.common.validation.Text;
import io.github.strogolsky.profinder.core.entity.Category;
import io.github.strogolsky.profinder.core.entity.Location;
import io.github.strogolsky.profinder.core.entity.OrderStatus;
import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Request body for a partial update of an order. Every field is optional: a missing or {@code null} field
 * keeps its current value.
 *
 * @param categories New list of categories for the order.
 * @param status Requested status. Only {@code CANCELLED} can be set through an order update.
 * @param description New description of the order.
 * @param title New title of the order.
 * @param price New price of the order.
 * @param deadline New deadline of the order.
 * @param location New location of the order.
 * @param version The version the client has seen. A different current version means someone changed the resource
 *        since, and the request is rejected with 409.
 */
public record OrderUpdateRequest(
        List<Category> categories,
        OrderStatus status,
        @Size(min = Limits.ORDER_DESCRIPTION_MIN_LENGTH, max = Limits.ORDER_DESCRIPTION_MAX_LENGTH)
        @NoControlChars(multiline = true)
        String description,
        @Size(min = Limits.ORDER_TITLE_MIN_LENGTH, max = Limits.ORDER_TITLE_MAX_LENGTH)
        @NoControlChars
        String title,
        @Positive(message = "Price must be positive")
        @Max(Limits.PRICE_MAX)
        Integer price,
        @FutureOrPresent
        LocalDateTime deadline,
        Location location,
        @NotNull
        @PositiveOrZero
        Long version) {

    /**
     * Cleans the text fields: the white space around a text is removed and an empty text is {@code null}.
     */
    public OrderUpdateRequest {
        title = Text.clean(title);
        description = Text.clean(description);
    }
}
