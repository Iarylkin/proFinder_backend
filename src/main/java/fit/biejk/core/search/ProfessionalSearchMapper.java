package fit.biejk.core.search;

import fit.biejk.core.entity.Category;
import fit.biejk.core.entity.ProfessionalProfile;
import fit.biejk.core.service.ProfessionalService;
import jakarta.enterprise.context.ApplicationScoped;
import lombok.RequiredArgsConstructor;

import java.util.ArrayList;
import java.util.List;

/**
 * Mapper for converting between {@link ProfessionalProfile} entities and {@link ProfessionalSearchDto} objects.
 * <p>
 * Used for transforming data between the database and the Elasticsearch index.
 * </p>
 */
@RequiredArgsConstructor
@ApplicationScoped
public class ProfessionalSearchMapper {

    /**
     * Service for retrieving professional entities from the database.
     * <p>
     * Used for reverse-mapping from DTOs back to entities by ID.
     * </p>
     */
    private final ProfessionalService professionalService;

    /**
     * Converts a {@link ProfessionalProfile} entity to a {@link ProfessionalSearchDto}.
     *
     * @param professional the professional entity
     * @return the corresponding DTO for search indexing
     */
    public ProfessionalSearchDto toDto(final ProfessionalProfile professional) {
        List<String> categoryNames = professional.getCategories() == null
                ? List.of()
                : professional.getCategories().stream()
                .map(Category::getName)
                .toList();

        return new ProfessionalSearchDto(
                professional.getId(),
                professional.getFirstName(),
                professional.getLastName(),
                professional.getDescription(),
                professional.getAverageRating(),
                categoryNames,
                professional.getLocation().getName()
        );
    }

    /**
     * Converts a {@link ProfessionalSearchDto} back to a {@link ProfessionalProfile} entity
     * using the database service.
     *
     * @param professionalDto the search DTO
     * @return the corresponding entity from the database
     */
    public ProfessionalProfile toEntity(final ProfessionalSearchDto professionalDto) {
        return professionalService.getById(professionalDto.getId());
    }

    /**
     * Converts a list of {@link ProfessionalSearchDto} to a list of {@link ProfessionalProfile} entities.
     *
     * @param professionalDtoList list of search DTOs
     * @return list of entities
     */
    public List<ProfessionalProfile> toEntityList(final List<ProfessionalSearchDto> professionalDtoList) {
        List<ProfessionalProfile> professionals = new ArrayList<>();
        for (ProfessionalSearchDto dto : professionalDtoList) {
            professionals.add(toEntity(dto));
        }
        return professionals;
    }
}
