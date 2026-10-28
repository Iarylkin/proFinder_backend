package fit.biejk.search;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.FieldValue;
import co.elastic.clients.elasticsearch._types.SortOrder;
import co.elastic.clients.elasticsearch.core.SearchResponse;
import co.elastic.clients.elasticsearch.core.search.Hit;
import jakarta.enterprise.context.ApplicationScoped;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.util.List;
import java.util.UUID;

/**
 * Service for managing the indexing, deletion, and searching of {@link OrderSearchDto}
 * documents in the Elasticsearch "orders" index.
 */
@ApplicationScoped
@Slf4j
@RequiredArgsConstructor
public class OrderSearchService {

    /**
     * Elasticsearch client used to perform operations on the search index.
     */
    private final ElasticsearchClient elasticsearchClient;

    /**
     * Saves or updates an {@link OrderSearchDto} document in the Elasticsearch "orders" index.
     *
     * @param dto the order data to be indexed
     */
    public void save(final OrderSearchDto dto) {
        try {
            log.info("Saving Order with id {} for search", dto.getId());
            elasticsearchClient.index(i -> i
                    .index("orders")
                    .id(String.valueOf(dto.getId()))
                    .document(dto)
            );
        } catch (IOException e) {
            log.warn("Failed to index order {}", dto.getId(), e);
        }
    }

    /**
     * Deletes an order document from the Elasticsearch "orders" index by its ID.
     *
     * @param id the ID of the order to delete
     */
    public void delete(final UUID id) {
        try {
            log.info("Deleting Order with id {} for search", id);
            elasticsearchClient.delete(d -> d
                    .index("orders")
                    .id(String.valueOf(id))
            );
        } catch (IOException e) {
            log.warn("Failed to delete order {} from the index", id, e);
        }
    }

    /**
     * Searches for orders that match the specified service names and location.
     * Only orders with status "CREATED" or "CUSTOMER_PENDING" are returned.
     *
     * @param keyword  the search keyword (e.g., title or description)
     * @param location the city/location to filter orders by
     * @param categories a list of service names to search for
     * @param page     page number for pagination (1-based)
     * @param size     number of orders per page
     * @return a list of matching {@link OrderSearchDto} objects
     */
    public List<OrderSearchDto> search(final String keyword,
                                       final String location,
                                       final List<String> categories,
                                       final int page,
                                       final int size) {

        int from = (page - 1) * size;

        try {
            log.info("Searching for Order with keyword '{}', location '{}'", keyword, location);

            SearchResponse<OrderSearchDto> response = elasticsearchClient.search(s -> s
                            .index("orders")
                            .from(from)
                            .size(size)
                            .query(q -> q
                                    .bool(b -> {
                                        if (hasText(keyword)) {
                                            b.must(m -> m
                                                    .multiMatch(mm -> mm
                                                            .fields("title", "description")
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
                                        if (categories != null && !categories.isEmpty()) {
                                            b.filter(f -> f
                                                    .terms(t -> t
                                                            .field("categories.keyword")
                                                            .terms(ts -> ts
                                                                    .value(categories.stream()
                                                                            .map(FieldValue::of)
                                                                            .toList())
                                                            )
                                                    )
                                            );
                                        }
                                        b.filter(f -> f
                                                .terms(t -> t
                                                        .field("status.keyword")
                                                        .terms(ts -> ts
                                                                .value(List.of(FieldValue.of("CREATED"),
                                                                        FieldValue.of("CUSTOMER_PENDING")))
                                                        )
                                                )
                                        );
                                        return b;
                                    })
                            )
                            .sort(so -> so
                                    .field(f -> f
                                            .field("createdAt")
                                            .order(SortOrder.Desc)
                                    )
                            ),
                    OrderSearchDto.class
            );

            return response.hits().hits().stream()
                    .map(Hit::source)
                    .toList();

        } catch (IOException e) {
            log.warn("Order search failed", e);
            return List.of();
        }
    }

    private static boolean hasText(final String value) {
        return value != null && !value.isBlank();
    }
}
