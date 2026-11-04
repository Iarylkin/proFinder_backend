package fit.biejk.dto;

import fit.biejk.validation.Limits;
import fit.biejk.validation.NoControlChars;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request of a professional to place a bid on an order. The identifier, order, professional, status and version are
 * set by the server.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class BidCreateRequest {

    /**
     * Description of the bid. Must not be blank.
     */
    @NotBlank
    @Size(max = Limits.BID_DESCRIPTION_MAX_LENGTH)
    @NoControlChars(multiline = true)
    private String description;

    /**
     * Price offered by the professional.
     */
    @NotNull(message = "Price is required")
    @Min(0)
    @Max(Limits.PRICE_MAX)
    private Integer price;
}
