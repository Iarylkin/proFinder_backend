package fit.biejk.mapper;

import fit.biejk.dto.ProfessionalDto;
import fit.biejk.dto.ProfessionalPublicDto;
import fit.biejk.dto.ProfessionalUpdateRequest;
import fit.biejk.entity.ProfessionalProfile;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

/**
 * MapStruct mapper for converting between {@link ProfessionalProfile}
 * entities and {@link ProfessionalDto} data transfer objects.
 * <p>
 * The personal data belongs to the profile; the email and the creation time are read from its user.
 * </p>
 */
@Mapper(componentModel = "jakarta")
public interface ProfessionalMapper {

    /**
     * Converts a professional profile to its corresponding DTO.
     *
     * @param entity the professional profile
     * @return the corresponding ProfessionalDto
     */
    @Mapping(target = "email", source = "user.email")
    @Mapping(target = "createAt", source = "user.createAt")
    ProfessionalDto toDto(ProfessionalProfile entity);

    /**
     * Converts a professional profile to its public DTO.
     *
     * @param entity the professional profile
     * @return the corresponding ProfessionalPublicDto
     */
    @Mapping(target = "createAt", source = "user.createAt")
    ProfessionalPublicDto toPublicDto(ProfessionalProfile entity);

    /**
     * Converts a list of professional profiles to a list of public DTOs.
     *
     * @param entityList the professional profiles
     * @return the corresponding public DTOs
     */
    List<ProfessionalPublicDto> toPublicDtoList(List<ProfessionalProfile> entityList);

    /**
     * Converts a ProfessionalDto to a professional profile.
     *
     * @param dto the ProfessionalDto
     * @return the corresponding professional profile
     */
    @Mapping(target = "user", ignore = true)
    @Mapping(target = "version", ignore = true)
    @Mapping(target = "avatarKey", ignore = true)
    @Mapping(target = "orderResponses", ignore = true)
    @Mapping(target = "reviews", ignore = true)
    ProfessionalProfile toEntity(ProfessionalDto dto);

    /**
     * Converts a list of professional profiles to a list of DTOs.
     *
     * @param entitiyList the professional profiles
     * @return the corresponding DTOs
     */
    List<ProfessionalDto> toDtoList(List<ProfessionalProfile> entitiyList);

    /**
     * Converts a list of DTOs to a list of professional profiles.
     *
     * @param dtoList the DTOs
     * @return the corresponding professional profiles
     */
    List<ProfessionalProfile> toEntityList(List<ProfessionalDto> dtoList);

    /**
     * Converts a professional update request to a professional profile that holds only the requested changes.
     * <p>
     * Every field the request does not carry stays {@code null}.
     * </p>
     *
     * @param request the professional update request
     * @return a professional profile with the requested changes
     */
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "user", ignore = true)
    @Mapping(target = "avatarKey", ignore = true)
    @Mapping(target = "averageRating", ignore = true)
    @Mapping(target = "orderResponses", ignore = true)
    @Mapping(target = "reviews", ignore = true)
    ProfessionalProfile toEntity(ProfessionalUpdateRequest request);
}
