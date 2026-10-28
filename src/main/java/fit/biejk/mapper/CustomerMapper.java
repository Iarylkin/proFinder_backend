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
 * The account fields (email, name, location) are read from, and written to, the user of the profile.
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
    @Mapping(target = "firstName", source = "user.firstName")
    @Mapping(target = "lastName", source = "user.lastName")
    @Mapping(target = "phoneNumber", source = "user.phoneNumber")
    @Mapping(target = "location", source = "user.location")
    @Mapping(target = "createAt", source = "user.createAt")
    CustomerDto toDto(CustomerProfile entity);

    /**
     * Converts a customer profile to its public DTO (without contact data).
     *
     * @param entity the customer profile
     * @return the corresponding CustomerPublicDto
     */
    @Mapping(target = "firstName", source = "user.firstName")
    @Mapping(target = "lastName", source = "user.lastName")
    @Mapping(target = "location", source = "user.location")
    @Mapping(target = "createAt", source = "user.createAt")
    CustomerPublicDto toPublicDto(CustomerProfile entity);

    /**
     * Converts a CustomerDto to a customer profile with its user.
     *
     * @param dto the CustomerDto
     * @return the corresponding customer profile
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
     * The account fields sit in the nested user; every field the request does not carry stays {@code null}.
     * </p>
     *
     * @param request the profile update request
     * @return a customer profile with the requested changes
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
    @Mapping(target = "orders", ignore = true)
    @Mapping(target = "reviews", ignore = true)
    CustomerProfile toEntity(ProfileUpdateRequest request);
}
