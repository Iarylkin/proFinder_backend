package fit.biejk.core.mapper;

import fit.biejk.core.dto.CustomerPublicResponse;
import fit.biejk.core.dto.CustomerResponse;
import fit.biejk.core.dto.CustomerUpdateRequest;
import fit.biejk.core.entity.CustomerProfile;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

/**
 * MapStruct mapper for converting between {@link CustomerProfile}
 * entities and {@link CustomerResponse} data transfer objects.
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
     * @return the corresponding CustomerResponse
     */
    @Mapping(target = "email", source = "user.email")
    @Mapping(target = "createAt", source = "user.createAt")
    CustomerResponse toDto(CustomerProfile entity);

    /**
     * Converts a customer profile to its public DTO (without contact data).
     *
     * @param entity the customer profile
     * @return the corresponding CustomerPublicResponse
     */
    @Mapping(target = "createAt", source = "user.createAt")
    CustomerPublicResponse toPublicDto(CustomerProfile entity);

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
    CustomerProfile toEntity(CustomerUpdateRequest request);
}
