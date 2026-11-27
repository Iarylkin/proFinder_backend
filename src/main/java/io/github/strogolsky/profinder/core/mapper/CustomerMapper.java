package io.github.strogolsky.profinder.core.mapper;

import io.github.strogolsky.profinder.core.dto.CustomerPublicResponse;
import io.github.strogolsky.profinder.core.dto.CustomerResponse;
import io.github.strogolsky.profinder.core.dto.CustomerUpdateRequest;
import io.github.strogolsky.profinder.core.entity.CustomerProfile;
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
     * @param email  the email of the account, which lives in the auth module
     * @return the corresponding CustomerResponse
     */
    @Mapping(target = "email", source = "email")
    CustomerResponse toDto(CustomerProfile entity, String email);

    /**
     * Converts a customer profile to its public DTO (without contact data).
     *
     * @param entity the customer profile
     * @return the corresponding CustomerPublicResponse
     */
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
    @Mapping(target = "createAt", ignore = true)
    @Mapping(target = "avatarKey", ignore = true)
    @Mapping(target = "orders", ignore = true)
    @Mapping(target = "reviews", ignore = true)
    CustomerProfile toEntity(CustomerUpdateRequest request);
}
