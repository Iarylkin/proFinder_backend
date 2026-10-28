package fit.biejk.dto;

import jakarta.ws.rs.QueryParam;
import lombok.Data;
import lombok.EqualsAndHashCode;
import java.util.List;

/**
 * DTO for filtering order search results.
 * <p>
 * Combines search criteria such as categories, location, and text query
 * with pagination support by extending {@link PageRequest}.
 * </p>
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class OrderFilterCriteria extends PageRequest {

    /**
     * List of categories to filter orders by.
     */
    @QueryParam("categories")
    private List<String> categories;

    /**
     * Geographical location filter for the orders.
     */
    @QueryParam("location")
    private String location;

    /**
     * Search query for order title or description.
     */
    @QueryParam("query")
    private String query;

    /**
     * Checks if any filtering criteria are currently set.
     *
     * @return true if at least one filter is active, false otherwise
     */
    public boolean hasFilters() {
        return (categories != null && !categories.isEmpty())
                || (location != null && !location.isBlank())
                || (query != null && !query.isBlank());
    }
}
