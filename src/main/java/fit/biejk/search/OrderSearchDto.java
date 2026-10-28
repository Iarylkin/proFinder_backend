package fit.biejk.search;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.UUID;

/**
 * Data Transfer Object for searching and returning Order information in search operations.
 * <p>
 * Contains minimal details needed for search indexing and results presentation.
 * </p>
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class OrderSearchDto {

    /**
     * Unique identifier of the order.
     */
    private UUID id;

    /**
     * Current status of the order, e.g. CREATED, COMPLETED.
     */
    private String status;

    /**
     * List of service names associated with the order.
     */
    private List<String> categories;

    /**
     * Name of the location where the order is to be performed.
     */
    private String location;

    /**
     * Title of the order, searched by keyword.
     */
    private String title;

    /**
     * Description of the order, searched by keyword.
     */
    private String description;

    /**
     * Creation time of the order in ISO-8601 format, used for sorting.
     */
    private String createdAt;
}
