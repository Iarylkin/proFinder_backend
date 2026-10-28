package fit.biejk.mapper;

import fit.biejk.dto.CustomerDto;
import fit.biejk.dto.ProfileUpdateRequest;
import fit.biejk.dto.CustomerPublicDto;
import fit.biejk.entity.Customer;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

/**
 * MapStruct mapper for converting between {@link Customer}
 * entities and {@link CustomerDto} data transfer objects.
 */
@Mapper(componentModel = "jakarta")
public interface CustomerMapper {

    /**
     * Converts a Customer entity to its corresponding DTO.
     *
     * @param entity the Customer entity
     * @return the corresponding CustomerDto
     */
    CustomerDto toDto(Customer entity);

    /**
     * Converts a Customer entity to its public DTO (without contact data).
     *
     * @param entity the Customer entity
     * @return the corresponding CustomerPublicDto
     */
    CustomerPublicDto toPublicDto(Customer entity);

    /**
     * Converts a Customer DTO to its corresponding entity.
     *
     * @param dto the CustomerDto
     * @return the corresponding Customer entity
     */
    Customer toEntity(CustomerDto dto);

    /**
     * Converts a list of Customer entities to a list of DTOs.
     *
     * @param entitiyList list of Customer entities
     * @return list of CustomerDto objects
     */
    List<CustomerDto> toDtoList(List<Customer> entitiyList);

    /**
     * Converts a list of Customer DTOs to a list of entities.
     *
     * @param dtoList list of CustomerDto objects
     * @return list of Customer entities
     */
    List<Customer> toEntityList(List<CustomerDto> dtoList);

    /**
     * Converts a profile update request to a Customer that holds only the requested changes.
     * Fields that were not sent stay {@code null}; the service copies only the non-null ones.
     *
     * @param request the profile update request
     * @return a Customer carrying the requested changes
     */
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "email", ignore = true)
    @Mapping(target = "password", ignore = true)
    @Mapping(target = "role", ignore = true)
    @Mapping(target = "avatarKey", ignore = true)
    @Mapping(target = "createAt", ignore = true)
    @Mapping(target = "orders", ignore = true)
    @Mapping(target = "reviews", ignore = true)
    Customer toEntity(ProfileUpdateRequest request);
}
