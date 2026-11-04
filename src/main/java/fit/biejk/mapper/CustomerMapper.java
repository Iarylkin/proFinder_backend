package fit.biejk.mapper;

import fit.biejk.dto.CustomerDto;
import fit.biejk.dto.CustomerPublicDto;
import fit.biejk.dto.ProfileUpdateRequest;
import fit.biejk.entity.CustomerProfile;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

/**
 * MapStruct mapper for converting between {@link CustomerProfile}
 * entities and {@link CustomerDto} data transfer objects.
 * <p>
 * The personal data belongs to the profile; the email and the creation time are read from its user.
 * </p>
 */
@Mapper(componentModel = "jakarta")
public interface CustomerMapper {

    /**
     * Converts a customer profile to its corresponding DTO.
     *
     * @param entity the customer profile
     * @return the corresponding CustomerDto
     */
    @Mapping(target = "email", source = "user.email")
    @Mapping(target = "createAt", source = "user.createAt")
    CustomerDto toDto(CustomerProfile entity);

    /**
     * Converts a customer profile to its public DTO (without contact data).
     *
     * @param entity the customer profile
     * @return the corresponding CustomerPublicDto
     */
    @Mapping(target = "createAt", source = "user.createAt")
    CustomerPublicDto toPublicDto(CustomerProfile entity);

    /**
     * Converts a CustomerDto to a customer profile.
     *
     * @param dto the CustomerDto
     * @return the corresponding customer profile
     */
    @Mapping(target = "user", ignore = true)
    @Mapping(target = "version", ignore = true)
    @Mapping(target = "avatarKey", ignore = true)
    @Mapping(target = "orders", ignore = true)
    @Mapping(target = "reviews", ignore = true)
    CustomerProfile toEntity(CustomerDto dto);

    /**
     * Converts a list of customer profiles to a list of DTOs.
     *
     * @param entitiyList the customer profiles
     * @return the corresponding DTOs
     */
    List<CustomerDto> toDtoList(List<CustomerProfile> entitiyList);

    /**
     * Converts a list of DTOs to a list of customer profiles.
     *
     * @param dtoList the DTOs
     * @return the corresponding customer profiles
     */
    List<CustomerProfile> toEntityList(List<CustomerDto> dtoList);

    /**
     * Converts a profile update request to a customer profile that holds only the requested changes.
     * <p>
     * Every field the request does not carry stays {@code null}.
     * </p>
     *
     * @param request the profile update request
     * @return a customer profile with the requested changes
     */
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "user", ignore = true)
    @Mapping(target = "avatarKey", ignore = true)
    @Mapping(target = "orders", ignore = true)
    @Mapping(target = "reviews", ignore = true)
    CustomerProfile toEntity(ProfileUpdateRequest request);
}
