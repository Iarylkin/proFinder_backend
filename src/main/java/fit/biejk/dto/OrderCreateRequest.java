package fit.biejk.dto;

import fit.biejk.entity.Category;
import fit.biejk.validation.Limits;
import fit.biejk.validation.NoControlChars;
import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Request to create an order. It holds only what the client may choose: the identifier, status, version, customer
 * and location are set by the server.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class OrderCreateRequest {

    /**
     * Categories of the order.
     */
    private List<Category> categories;

    /**
     * Title of the order.
     */
    @NotBlank
    @Size(min = Limits.ORDER_TITLE_MIN_LENGTH, max = Limits.ORDER_TITLE_MAX_LENGTH)
    @NoControlChars
    private String title;

    /**
     * Description of the order.
     */
    @NotBlank
    @Size(min = Limits.ORDER_DESCRIPTION_MIN_LENGTH, max = Limits.ORDER_DESCRIPTION_MAX_LENGTH)
    @NoControlChars(multiline = true)
    private String description;

    /**
     * Price offered by the customer.
     */
    @NotNull(message = "Price is required")
    @Positive(message = "Price must be positive")
    @Max(Limits.PRICE_MAX)
    private Integer price;

    /**
     * Date by which the work should be done; it must not be in the past.
     */
    @FutureOrPresent
    private LocalDateTime deadline;
}
