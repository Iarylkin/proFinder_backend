package fit.biejk.search;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.SortOrder;
import co.elastic.clients.elasticsearch.core.SearchResponse;
import co.elastic.clients.elasticsearch.core.search.Hit;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.util.List;

/**
 * Service for indexing, deleting, and searching professionals in Elasticsearch.
 */
@Slf4j
@ApplicationScoped
@RequiredArgsConstructor
public class ProfessionalSearchService {

    /**
     * Elasticsearch client used for communication with the search index.
     */
    private final ElasticsearchClient elasticsearchClient;

    /**
     * Indexes a professional document in Elasticsearch.
     *
     * @param dto the professional data to be indexed
     */
    @Transactional
    public void save(final ProfessionalSearchDto dto) {
        try {
            log.info("Saving Professional with id {} for search", dto.getId());
            elasticsearchClient.index(i -> i
                    .index("professionals")
                    .id(String.valueOf(dto.getId()))
                    .document(dto)
            );
        } catch (IOException e) {
            log.warn(e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Deletes a professional document from Elasticsearch by ID.
     *
     * @param id the ID of the professional to delete
     */
    @Transactional
    public void delete(final Long id) {
        try {
            log.info("Deleting Professional with id {} for search", id);
            elasticsearchClient.delete(d -> d
                    .index("professionals")
                    .id(String.valueOf(id))
            );
        } catch (IOException e) {
            log.warn(e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Searches for professionals using keyword and location.
     * The results are sorted by averageRating in descending order.
     * Supports fuzzy matching for keyword.
     *
     * @param keyword  the search keyword (e.g., service name or description)
     * @param location the city to filter professionals by
     * @param page     page number for pagination (1-based)
     * @param size     number of professionals per page
     * @return a list of matching professionals
     */
    public List<ProfessionalSearchDto> search(final String keyword,
                                            final String location,
                                            final int page,
                                            final int size) {

        int from = (page - 1) * size;

        try {
            log.info("Searching for Professional with keyword {} and location {}", keyword, location);
            SearchResponse<ProfessionalSearchDto> response = elasticsearchClient.search(s -> s
                            .index("professionals")
                            .from(from)
                            .size(size)
                            .query(q -> q
                                    .bool(b -> {
                                        if (hasText(keyword)) {
                                            b.must(m -> m
                                                    .multiMatch(mm -> mm
                                                            .fields("firstName", "lastName",
                                                                    "description", "services")
                                                            .query(keyword)
                                                            .fuzziness("AUTO")
                                                    )
                                            );
                                        }
                                        if (hasText(location)) {
                                            b.filter(f -> f
                                                    .term(t -> t
                                                            .field("location.keyword")
                                                            .value(location)
                                                    )
                                            );
                                        }
                                        return b;
                                    })
                            )
                            .sort(so -> so
                                    .field(f -> f
                                            .field("averageRating")
                                            .order(SortOrder.Desc)
                                    )
                            ),
                    ProfessionalSearchDto.class
            );

            return response.hits().hits().stream()
                    .map(Hit::source)
                    .toList();

        } catch (IOException e) {
            log.warn(e.getMessage());
            e.printStackTrace();
            return List.of();
        }
    }

    private static boolean hasText(final String value) {
        return value != null && !value.isBlank();
    }
}
