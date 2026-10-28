package fit.biejk.mapper;

import fit.biejk.dto.ProfessionalDto;
import fit.biejk.dto.ProfileUpdateRequest;
import fit.biejk.dto.ProfessionalPublicDto;
import fit.biejk.entity.Professional;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

/**
 * MapStruct mapper for converting between {@link Professional}
 * entities and {@link ProfessionalDto} data transfer objects.
 */
@Mapper(componentModel = "jakarta")
public interface ProfessionalMapper {

    /**
     * Converts a Professional entity to its corresponding DTO.
     *
     * @param entity the Professional entity
     * @return the mapped ProfessionalDto
     */
    ProfessionalDto toDto(Professional entity);

    /**
     * Converts a Professional entity to its public DTO (without the login email).
     *
     * @param entity the Professional entity
     * @return the mapped ProfessionalPublicDto
     */
    ProfessionalPublicDto toPublicDto(Professional entity);

    /**
     * Converts a list of Professional entities to a list of public DTOs.
     *
     * @param entityList list of Professional entities
     * @return list of ProfessionalPublicDto objects
     */
    List<ProfessionalPublicDto> toPublicDtoList(List<Professional> entityList);

    /**
     * Converts a Professional DTO to its corresponding entity.
     *
     * @param dto the ProfessionalDto
     * @return the mapped Professional entity
     */
    Professional toEntity(ProfessionalDto dto);

    /**
     * Converts a list of Professional entities to a list of DTOs.
     *
     * @param entitiyList list of Professional entities
     * @return list of ProfessionalDto objects
     */
    List<ProfessionalDto> toDtoList(List<Professional> entitiyList);

    /**
     * Converts a list of Professional DTOs to a list of entities.
     *
     * @param dtoList list of ProfessionalDto objects
     * @return list of Professional entities
     */
    List<Professional> toEntityList(List<ProfessionalDto> dtoList);

    /**
     * Converts a profile update request to a Professional that holds only the requested changes.
     * Fields that were not sent stay {@code null}; the service copies only the non-null ones.
     *
     * @param request the profile update request
     * @return a Professional carrying the requested changes
     */
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "email", ignore = true)
    @Mapping(target = "password", ignore = true)
    @Mapping(target = "role", ignore = true)
    @Mapping(target = "avatarKey", ignore = true)
    @Mapping(target = "createAt", ignore = true)
    @Mapping(target = "averageRating", ignore = true)
    @Mapping(target = "orderResponses", ignore = true)
    @Mapping(target = "reviews", ignore = true)
    Professional toEntity(ProfileUpdateRequest request);
}
