package fit.biejk.search;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import fit.biejk.repository.ProfessionalRepository;
import io.quarkus.runtime.Startup;
import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.ApplicationScoped;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;

/**
 * Initializes the Elasticsearch index for storing professional data.
 * <p>
 * This class deletes the existing index on startup, recreates it with the correct mappings,
 * and populates it from the database using the {@link ProfessionalRepository}.
 * </p>
 */
@Startup
@ApplicationScoped
@Slf4j
@RequiredArgsConstructor
public class ProfessionalIndexInitializer {

    /**
     * Elasticsearch client used to interact with the Elasticsearch cluster.
     */
    private final ElasticsearchClient elasticsearchClient;

    /**
     * Service used to retrieve professional data from the PostgreSQL database.
     */
    private final ProfessionalRepository professionalRepository;

    /**
     * Service used to persist professional data to the Elasticsearch index.
     */
    private final ProfessionalSearchService professionalSearchService;

    /**
     * Mapper for converting {@link fit.biejk.entity.Professional} entities into {@link ProfessionalSearchDto}.
     */
    private final ProfessionalSearchMapper professionalSearchMapper;

    /**
     * Called after the bean is constructed.
     * <p>
     * This method checks if the index already exists. If it does, it deletes the index,
     * then creates a new one with the necessary field mappings and loads data into it.
     * </p>
     */
    @PostConstruct
    void init() {
        String indexName = ProfessionalSearchService.INDEX_NAME;

        try {
            boolean exists = elasticsearchClient.indices().exists(e -> e.index(indexName)).value();
            if (exists) {
                log.info("Deleting index " + indexName);
                elasticsearchClient.indices().delete(d -> d.index(indexName));
            }
            log.info("Creating index " + indexName);
            elasticsearchClient.indices().create(c -> c
                    .index(indexName)
                    .mappings(m -> m
                            .properties("id", p -> p.long_(x -> x))
                            .properties("firstName", p -> p.text(t -> t))
                            .properties("lastName", p -> p.text(t -> t))
                            .properties("description", p -> p.text(t -> t))
                            .properties("averageRating", p -> p.double_(d -> d))
                            .properties("services", p -> p.text(t -> t))
                            .properties("location", p -> p
                                    .text(t -> t
                                            .fields("keyword", k -> k.keyword(kk -> kk))
                                    )
                            )
                    )
            );
            load();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Loads all professionals from the database and saves them into the Elasticsearch index.
     */
    void load() {
        log.info("Loading professionals from database");
        professionalRepository.listAll()
                .forEach(professional ->
                        professionalSearchService.save(professionalSearchMapper.toDto(professional))
                );
    }
}
