package fit.biejk.mapper;

import fit.biejk.dto.ProfessionalPublicResponse;
import fit.biejk.dto.ProfessionalResponse;
import fit.biejk.dto.ProfessionalUpdateRequest;
import fit.biejk.entity.ProfessionalProfile;
import fit.biejk.validation.Text;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;
import java.util.Optional;

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

    /**
     * Writes a text field of a partial update into the entity that holds the changes: {@code null} (not sent) stays
     * {@code null}, a text is the text and the empty {@link Optional} (clear the field) is the empty text, which the
     * service reads as "clear".
     *
     * @param value the field of the request
     * @return the text for the entity
     */
    default String toChange(final Optional<String> value) {
        return Text.toChange(value);
    }
}
