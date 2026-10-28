package fit.biejk.search;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Data Transfer Object representing a professional in the search index.
 * <p>
 * Used for indexing and retrieving professional data from Elasticsearch.
 * </p>
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ProfessionalSearchDto {

    /**
     * Unique identifier of the professional.
     */
    private Long id;

    /**
     * First name of the professional.
     */
    private String firstName;

    /**
     * Last name of the professional.
     */
    private String lastName;

    /**
     * Short description or bio of the professional.
     */
    private String description;

    /**
     * Average rating of the professional (calculated from reviews).
     */
    private Double averageRating;

    /**
     * List of categories provided by the professional.
     */
    private List<String> categories;

    /**
     * Location (city or area) where the professional operates.
     */
    private String location;
}
