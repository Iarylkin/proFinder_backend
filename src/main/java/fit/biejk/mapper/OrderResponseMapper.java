package fit.biejk.mapper;

import fit.biejk.dto.OrderResponseCreateRequest;
import fit.biejk.dto.OrderResponseDto;
import fit.biejk.entity.OrderResponse;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

/**
 * MapStruct mapper for converting between {@link OrderResponse}
 * entities and {@link OrderResponseDto} data transfer objects.
 */
@Mapper(componentModel = "jakarta")
public interface OrderResponseMapper {

    /**
     * Converts the request to respond to an order to a new OrderResponse entity. The identifier, version, order,
     * professional and status are set by the server, so they are not mapped.
     *
     * @param request the request to convert
     * @return the mapped OrderResponse entity
     */
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "version", ignore = true)
    @Mapping(target = "order", ignore = true)
    @Mapping(target = "professional", ignore = true)
    @Mapping(target = "status", ignore = true)
    OrderResponse toEntity(OrderResponseCreateRequest request);

    /**
     * Converts an OrderResponse entity to an OrderResponseDto.
     * Maps the IDs of associated order and professional.
     *
     * @param orderResponse the entity to convert
     * @return the mapped OrderResponseDto
     */
    @Mapping(target = "orderId", source = "order.id")
    @Mapping(target = "professionalId", source = "professional.id")
    OrderResponseDto toDto(OrderResponse orderResponse);

    /**
     * Converts a list of OrderResponse entities to a list of DTOs.
     *
     * @param orderResponseList list of entities
     * @return list of OrderResponseDto objects
     */
    List<OrderResponseDto> toDtoList(List<OrderResponse> orderResponseList);
}
