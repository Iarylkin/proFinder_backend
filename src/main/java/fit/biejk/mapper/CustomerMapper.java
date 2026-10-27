package fit.biejk.mapper;

import fit.biejk.dto.CustomerDto;
import fit.biejk.dto.CustomerPublicDto;
import fit.biejk.entity.Customer;
import org.mapstruct.Mapper;

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
}
