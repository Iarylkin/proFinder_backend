package fit.biejk.core.mapper;

import fit.biejk.core.dto.OrderCreateRequest;
import fit.biejk.core.dto.OrderResponse;
import fit.biejk.core.dto.OrderUpdateRequest;
import fit.biejk.core.entity.Order;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

/**
 * MapStruct mapper for converting between {@link Order}
 * entities and {@link OrderResponse} data transfer objects.
 */
@Mapper(componentModel = "jakarta")
public interface OrderMapper {

    /**
     * Converts the request to create an order to a new Order entity. The identifier, version, status, customer
     * and location are set by the server, so they are not mapped.
     *
     * @param request the request to convert
     * @return the mapped Order entity
     */
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "version", ignore = true)
    @Mapping(target = "customer", ignore = true)
    @Mapping(target = "status", ignore = true)
    @Mapping(target = "location", ignore = true)
    @Mapping(target = "bids", ignore = true)
    Order toEntity(OrderCreateRequest request);

    /**
     * Converts an Order entity to an OrderResponse.
     * Maps the customer ID from the nested customer entity.
     *
     * @param order the entity to convert
     * @return the mapped OrderResponse
     */
    @Mapping(target = "customerId", source = "customer.id")
    OrderResponse toDto(Order order);

    /**
     * Converts a list of Order entities to a list of OrderResponse objects.
     *
     * @param entitiyList list of Order entities
     * @return list of OrderResponse objects
     */
    List<OrderResponse> toDtoList(List<Order> entitiyList);

    /**
     * Converts an update request to an Order that holds only the requested changes.
     * Fields that were not sent stay {@code null}; the service copies only the non-null ones.
     *
     * @param request the update request
     * @return an Order carrying the requested changes
     */
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "customer", ignore = true)
    @Mapping(target = "bids", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    Order toEntity(OrderUpdateRequest request);
}
