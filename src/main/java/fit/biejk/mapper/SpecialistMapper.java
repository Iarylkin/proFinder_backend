package fit.biejk.mapper;

import fit.biejk.dto.SpecialistDto;
import fit.biejk.dto.SpecialistPublicDto;
import fit.biejk.entity.Specialist;
import org.mapstruct.Mapper;

import java.util.List;

/**
 * MapStruct mapper for converting between {@link Specialist}
 * entities and {@link SpecialistDto} data transfer objects.
 */
@Mapper(componentModel = "jakarta")
public interface SpecialistMapper {

    /**
     * Converts a Specialist entity to its corresponding DTO.
     *
     * @param entity the Specialist entity
     * @return the mapped SpecialistDto
     */
    SpecialistDto toDto(Specialist entity);

    /**
     * Converts a Specialist entity to its public DTO (without the login email).
     *
     * @param entity the Specialist entity
     * @return the mapped SpecialistPublicDto
     */
    SpecialistPublicDto toPublicDto(Specialist entity);

    /**
     * Converts a list of Specialist entities to a list of public DTOs.
     *
     * @param entityList list of Specialist entities
     * @return list of SpecialistPublicDto objects
     */
    List<SpecialistPublicDto> toPublicDtoList(List<Specialist> entityList);

    /**
     * Converts a Specialist DTO to its corresponding entity.
     *
     * @param dto the SpecialistDto
     * @return the mapped Specialist entity
     */
    Specialist toEntity(SpecialistDto dto);

    /**
     * Converts a list of Specialist entities to a list of DTOs.
     *
     * @param entitiyList list of Specialist entities
     * @return list of SpecialistDto objects
     */
    List<SpecialistDto> toDtoList(List<Specialist> entitiyList);

    /**
     * Converts a list of Specialist DTOs to a list of entities.
     *
     * @param dtoList list of SpecialistDto objects
     * @return list of Specialist entities
     */
    List<Specialist> toEntityList(List<SpecialistDto> dtoList);
}
