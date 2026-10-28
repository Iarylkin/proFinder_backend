package fit.biejk.mapper;

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
     * Converts an OrderResponseDto to an OrderResponse entity.
     * Ignores nested order and professional IDs during mapping.
     *
     * @param orderResponseDto the DTO to convert
     * @return the mapped OrderResponse entity
     */
    @Mapping(target = "order.id", ignore = true)
    @Mapping(target = "professional.id", ignore = true)
    OrderResponse toEntity(OrderResponseDto orderResponseDto);

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
     * Converts a list of OrderResponseDto objects to a list of entities.
     *
     * @param orderResponseDtoList list of DTOs
     * @return list of OrderResponse entities
     */
    List<OrderResponse> toEntityList(List<OrderResponseDto> orderResponseDtoList);

    /**
     * Converts a list of OrderResponse entities to a list of DTOs.
     *
     * @param orderResponseList list of entities
     * @return list of OrderResponseDto objects
     */
    List<OrderResponseDto> toDtoList(List<OrderResponse> orderResponseList);
}
