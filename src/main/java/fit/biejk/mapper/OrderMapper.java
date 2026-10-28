package fit.biejk.mapper;

import fit.biejk.dto.OrderDto;
import fit.biejk.dto.OrderUpdateRequest;
import fit.biejk.entity.Order;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

/**
 * MapStruct mapper for converting between {@link Order}
 * entities and {@link OrderDto} data transfer objects.
 */
@Mapper(componentModel = "jakarta")
public interface OrderMapper {

    /**
     * Converts an OrderDto to an Order entity.
     * Ignores the 'customer' and 'orderResponses' fields during mapping.
     *
     * @param orderDto the DTO to convert
     * @return the mapped Order entity
     */
    @Mapping(target = "customer", ignore = true)
    @Mapping(target = "orderResponses", ignore = true)
    Order toEntity(OrderDto orderDto);

    /**
     * Converts an Order entity to an OrderDto.
     * Maps the customer ID from the nested customer entity.
     *
     * @param order the entity to convert
     * @return the mapped OrderDto
     */
    @Mapping(target = "customerId", source = "customer.id")
    OrderDto toDto(Order order);

    /**
     * Converts a list of Order entities to a list of OrderDto objects.
     *
     * @param entitiyList list of Order entities
     * @return list of OrderDto objects
     */
    List<OrderDto> toDtoList(List<Order> entitiyList);

    /**
     * Converts a list of OrderDto objects to a list of Order entities.
     *
     * @param dtoList list of OrderDto objects
     * @return list of Order entities
     */
    List<Order> toEntityList(List<OrderDto> dtoList);

    /**
     * Converts an update request to an Order that holds only the requested changes.
     * Fields that were not sent stay {@code null}; the service copies only the non-null ones.
     *
     * @param request the update request
     * @return an Order carrying the requested changes
     */
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "customer", ignore = true)
    @Mapping(target = "orderResponses", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    Order toEntity(OrderUpdateRequest request);
}
