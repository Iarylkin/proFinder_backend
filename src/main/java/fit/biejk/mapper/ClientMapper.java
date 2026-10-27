package fit.biejk.mapper;

import fit.biejk.dto.ClientDto;
import fit.biejk.dto.ClientPublicDto;
import fit.biejk.entity.Client;
import org.mapstruct.Mapper;

import java.util.List;

/**
 * MapStruct mapper for converting between {@link Client}
 * entities and {@link ClientDto} data transfer objects.
 */
@Mapper(componentModel = "jakarta")
public interface ClientMapper {

    /**
     * Converts a Client entity to its corresponding DTO.
     *
     * @param entity the Client entity
     * @return the corresponding ClientDto
     */
    ClientDto toDto(Client entity);

    /**
     * Converts a Client entity to its public DTO (without contact data).
     *
     * @param entity the Client entity
     * @return the corresponding ClientPublicDto
     */
    ClientPublicDto toPublicDto(Client entity);

    /**
     * Converts a Client DTO to its corresponding entity.
     *
     * @param dto the ClientDto
     * @return the corresponding Client entity
     */
    Client toEntity(ClientDto dto);

    /**
     * Converts a list of Client entities to a list of DTOs.
     *
     * @param entitiyList list of Client entities
     * @return list of ClientDto objects
     */
    List<ClientDto> toDtoList(List<Client> entitiyList);

    /**
     * Converts a list of Client DTOs to a list of entities.
     *
     * @param dtoList list of ClientDto objects
     * @return list of Client entities
     */
    List<Client> toEntityList(List<ClientDto> dtoList);
}
