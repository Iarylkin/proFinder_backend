package fit.biejk.core.mapper;

import fit.biejk.core.dto.ProfessionalPublicResponse;
import fit.biejk.core.dto.ProfessionalResponse;
import fit.biejk.core.dto.ProfessionalUpdateRequest;
import fit.biejk.core.entity.ProfessionalProfile;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;
/**
 * MapStruct mapper for converting between {@link ProfessionalProfile}
 * entities and {@link ProfessionalResponse} data transfer objects.
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
     * @return the corresponding ProfessionalResponse
     */
    @Mapping(target = "email", source = "user.email")
    @Mapping(target = "createAt", source = "user.createAt")
    ProfessionalResponse toDto(ProfessionalProfile entity);

    /**
     * Converts a professional profile to its public DTO.
     *
     * @param entity the professional profile
     * @return the corresponding ProfessionalPublicResponse
     */
    @Mapping(target = "createAt", source = "user.createAt")
    ProfessionalPublicResponse toPublicDto(ProfessionalProfile entity);

    /**
     * Converts a list of professional profiles to a list of public DTOs.
     *
     * @param entityList the professional profiles
     * @return the corresponding public DTOs
     */
    List<ProfessionalPublicResponse> toPublicDtoList(List<ProfessionalProfile> entityList);

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
    @Mapping(target = "bids", ignore = true)
    @Mapping(target = "reviews", ignore = true)
    ProfessionalProfile toEntity(ProfessionalUpdateRequest request);
}
