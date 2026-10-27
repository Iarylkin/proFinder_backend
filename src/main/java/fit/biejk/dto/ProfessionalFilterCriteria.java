package fit.biejk.dto;

import jakarta.ws.rs.QueryParam;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * DTO for filtering professional search results.
 * <p>
 * Extends {@link PageRequest} to include pagination along with search criteria.
 * </p>
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class ProfessionalFilterCriteria extends PageRequest {

    /**
     * Search query for professional name or description.
     */
    @QueryParam("query")
    private String query;

    /**
     * Location filter for professionals.
     */
    @QueryParam("location")
    private String location;

    /**
     * Checks if any filtering criteria are provided.
     *
     * @return true if at least one filter is not blank, false otherwise
     */
    public boolean hasFilters() {
        return (query != null && !query.isBlank())
                || (location != null && !location.isBlank());
    }
}
