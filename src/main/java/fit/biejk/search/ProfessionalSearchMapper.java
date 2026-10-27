package fit.biejk.search;

import fit.biejk.entity.ServiceOffering;
import fit.biejk.entity.Professional;
import fit.biejk.service.ProfessionalService;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import java.util.ArrayList;
import java.util.List;

/**
 * Mapper for converting between {@link Professional} entities and {@link ProfessionalSearchDto} objects.
 * <p>
 * Used for transforming data between the database and the Elasticsearch index.
 * </p>
 */
@ApplicationScoped
public class ProfessionalSearchMapper {

    /**
     * Service for retrieving professional entities from the database.
     * <p>
     * Used for reverse-mapping from DTOs back to entities by ID.
     * </p>
     */
    @Inject
    private ProfessionalService professionalService;

    /**
     * Converts a {@link Professional} entity to a {@link ProfessionalSearchDto}.
     *
     * @param professional the professional entity
     * @return the corresponding DTO for search indexing
     */
    public ProfessionalSearchDto toDto(final Professional professional) {
        List<String> serviceNames = professional.getServiceOfferings() == null
                ? List.of()
                : professional.getServiceOfferings().stream()
                .map(ServiceOffering::getName)
                .toList();

        return new ProfessionalSearchDto(
                professional.getId(),
                professional.getFirstName(),
                professional.getLastName(),
                professional.getDescription(),
                professional.getAverageRating(),
                serviceNames,
                professional.getLocation().getName()
        );
    }

    /**
     * Converts a {@link ProfessionalSearchDto} back to a {@link Professional} entity
     * using the database service.
     *
     * @param professionalDto the search DTO
     * @return the corresponding entity from the database
     */
    public Professional toEntity(final ProfessionalSearchDto professionalDto) {
        return professionalService.getById(professionalDto.getId());
    }

    /**
     * Converts a list of {@link ProfessionalSearchDto} to a list of {@link Professional} entities.
     *
     * @param professionalDtoList list of search DTOs
     * @return list of entities
     */
    public List<Professional> toEntityList(final List<ProfessionalSearchDto> professionalDtoList) {
        List<Professional> professionals = new ArrayList<>();
        for (ProfessionalSearchDto dto : professionalDtoList) {
            professionals.add(toEntity(dto));
        }
        return professionals;
    }
}
