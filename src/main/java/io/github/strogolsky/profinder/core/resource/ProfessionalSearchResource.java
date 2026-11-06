package io.github.strogolsky.profinder.core.resource;

import io.github.strogolsky.profinder.common.dto.PageRequest;
import io.github.strogolsky.profinder.core.dto.ProfessionalFilterRequest;
import io.github.strogolsky.profinder.core.entity.ProfessionalProfile;
import io.github.strogolsky.profinder.core.mapper.ProfessionalMapper;
import io.github.strogolsky.profinder.core.search.ProfessionalSearchMapper;
import io.github.strogolsky.profinder.core.search.ProfessionalSearchService;
import io.github.strogolsky.profinder.core.service.ProfessionalService;
import jakarta.annotation.security.PermitAll;
import jakarta.ws.rs.BeanParam;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.core.Response;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.List;

/**
 * REST resource for searching professionals.
 */
@Slf4j
@Path("/v1/search/professionals")
@RequiredArgsConstructor
public class ProfessionalSearchResource {

    /**
     * Service for searching professionals in Elasticsearch.
     */
    private final ProfessionalSearchService professionalSearchService;

    /**
     * Mapper for converting search results back to professional entities.
     */
    private final ProfessionalSearchMapper professionalSearchMapper;

    /**
     * Service for listing professionals when no filter is given.
     */
    private final ProfessionalService professionalService;

    /**
     * Mapper for converting professionals to public DTOs.
     */
    private final ProfessionalMapper professionalMapper;

    /**
     * Searches for professionals by keyword and location, or lists all of them when no filter is given.
     *
     * @param criteria   the filtering criteria
     * @param pagination the pagination parameters (page and size)
     * @return list of public professional profiles
     */
    @GET
    @PermitAll
    public Response search(
            @BeanParam final ProfessionalFilterRequest criteria,
            @BeanParam final PageRequest pagination
    ) {
        if (criteria.hasFilters()) {
            var searchResults = professionalSearchService.search(
                    criteria.getQuery(),
                    criteria.getLocation(),
                    pagination.getPage(),
                    pagination.getSize()
            );
            var result = professionalSearchMapper.toEntityList(searchResults);
            return Response.ok(professionalMapper.toPublicDtoList(result)).build();
        }

        log.info("Get all professionals");
        List<ProfessionalProfile> result = professionalService.getAll(
                pagination.getPage(),
                pagination.getSize()
        );
        log.debug("Found {} professionals", result.size());
        return Response.ok(professionalMapper.toPublicDtoList(result)).build();
    }
}
