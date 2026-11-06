package fit.biejk.mapper;

import fit.biejk.dto.BidCreateRequest;
import fit.biejk.dto.BidResponse;
import fit.biejk.entity.Bid;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

/**
 * MapStruct mapper for converting between {@link Bid}
 * entities and {@link BidResponse} data transfer objects.
 */
@Mapper(componentModel = "jakarta")
public interface BidMapper {

    /**
     * Converts the request to place a bid on an order to a new Bid entity. The identifier, version, order,
     * professional and status are set by the server, so they are not mapped.
     *
     * @param request the request to convert
     * @return the mapped Bid entity
     */
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "version", ignore = true)
    @Mapping(target = "order", ignore = true)
    @Mapping(target = "professional", ignore = true)
    @Mapping(target = "status", ignore = true)
    Bid toEntity(BidCreateRequest request);

    /**
     * Converts an Bid entity to an BidResponse.
     * Maps the IDs of associated order and professional.
     *
     * @param bid the entity to convert
     * @return the mapped BidResponse
     */
    @Mapping(target = "orderId", source = "order.id")
    @Mapping(target = "professionalId", source = "professional.id")
    BidResponse toDto(Bid bid);

    /**
     * Converts a list of Bid entities to a list of DTOs.
     *
     * @param bidList list of entities
     * @return list of BidResponse objects
     */
    List<BidResponse> toDtoList(List<Bid> bidList);
}
