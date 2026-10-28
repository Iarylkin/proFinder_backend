package fit.biejk.mapper;

import fit.biejk.dto.ProfessionalDto;
import fit.biejk.dto.ProfessionalPublicDto;
import fit.biejk.dto.ProfileUpdateRequest;
import fit.biejk.entity.ProfessionalProfile;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

/**
 * MapStruct mapper for converting between {@link ProfessionalProfile}
 * entities and {@link ProfessionalDto} data transfer objects.
 * <p>
 * The account fields (email, name, location) are read from, and written to, the user of the profile.
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
    @Mapping(target = "firstName", source = "user.firstName")
    @Mapping(target = "lastName", source = "user.lastName")
    @Mapping(target = "phoneNumber", source = "user.phoneNumber")
    @Mapping(target = "location", source = "user.location")
    @Mapping(target = "createAt", source = "user.createAt")
    ProfessionalDto toDto(ProfessionalProfile entity);

    /**
     * Converts a professional profile to its public DTO.
     *
     * @param entity the professional profile
     * @return the corresponding ProfessionalPublicDto
     */
    @Mapping(target = "firstName", source = "user.firstName")
    @Mapping(target = "lastName", source = "user.lastName")
    @Mapping(target = "phoneNumber", source = "user.phoneNumber")
    @Mapping(target = "location", source = "user.location")
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
     * Converts a ProfessionalDto to a professional profile with its user.
     *
     * @param dto the ProfessionalDto
     * @return the corresponding professional profile
     */
    @Mapping(target = "user.email", source = "email")
    @Mapping(target = "user.firstName", source = "firstName")
    @Mapping(target = "user.lastName", source = "lastName")
    @Mapping(target = "user.phoneNumber", source = "phoneNumber")
    @Mapping(target = "user.location", source = "location")
    @Mapping(target = "user.id", ignore = true)
    @Mapping(target = "user.version", ignore = true)
    @Mapping(target = "user.password", ignore = true)
    @Mapping(target = "user.role", ignore = true)
    @Mapping(target = "user.avatarKey", ignore = true)
    @Mapping(target = "user.initiatedChats", ignore = true)
    @Mapping(target = "user.receivedChats", ignore = true)
    @Mapping(target = "user.profile", ignore = true)
    @Mapping(target = "version", ignore = true)
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
     * Converts a profile update request to a professional profile that holds only the requested changes.
     * <p>
     * The account fields sit in the nested user; every field the request does not carry stays {@code null}.
     * </p>
     *
     * @param request the profile update request
     * @return a professional profile with the requested changes
     */
    @Mapping(target = "user.firstName", source = "firstName")
    @Mapping(target = "user.lastName", source = "lastName")
    @Mapping(target = "user.phoneNumber", source = "phoneNumber")
    @Mapping(target = "user.location", source = "location")
    @Mapping(target = "user.id", ignore = true)
    @Mapping(target = "user.version", ignore = true)
    @Mapping(target = "user.password", ignore = true)
    @Mapping(target = "user.role", ignore = true)
    @Mapping(target = "user.avatarKey", ignore = true)
    @Mapping(target = "user.initiatedChats", ignore = true)
    @Mapping(target = "user.receivedChats", ignore = true)
    @Mapping(target = "user.profile", ignore = true)
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "version", ignore = true)
    @Mapping(target = "averageRating", ignore = true)
    @Mapping(target = "orderResponses", ignore = true)
    @Mapping(target = "reviews", ignore = true)
    ProfessionalProfile toEntity(ProfileUpdateRequest request);
}
