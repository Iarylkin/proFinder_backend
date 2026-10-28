package fit.biejk.search;

import fit.biejk.entity.Order;
import fit.biejk.entity.Category;
import fit.biejk.service.OrderService;
import jakarta.enterprise.context.ApplicationScoped;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;

/**
 * Mapper class responsible for converting between {@link Order} entities
 * and {@link OrderSearchDto} objects used for Elasticsearch indexing and searching.
 */
@Slf4j
@ApplicationScoped
@RequiredArgsConstructor
public class OrderSearchMapper {

    /**
     * Service providing access to {@link Order} data from the database.
     */
    private final OrderService orderService;

    /**
     * Converts an {@link Order} entity to a {@link OrderSearchDto} for Elasticsearch indexing.
     *
     * @param entity the {@link Order} entity to convert
     * @return a populated {@link OrderSearchDto} object
     */
    public OrderSearchDto toDto(final Order entity) {
        List<String> categoryNames = entity.getCategories() == null
                ? List.of()
                : entity.getCategories().stream()
                .map(Category::getName)
                .toList();

        String locationName = entity.getLocation() != null ? entity.getLocation().getName() : null;

        return new OrderSearchDto(
                entity.getId(),
                entity.getStatus().name(),
                categoryNames,
                locationName,
                entity.getTitle(),
                entity.getDescription(),
                entity.getCreatedAt().toString()
        );
    }

    /**
     * Retrieves an {@link Order} entity from the database using the ID from the {@link OrderSearchDto}.
     *
     * @param dto the {@link OrderSearchDto} containing the ID
     * @return the corresponding {@link Order} entity from the database
     */
    public Order toEntity(final OrderSearchDto dto) {
        return orderService.getById(dto.getId());
    }

    /**
     * Converts a list of {@link OrderSearchDto} objects to a list of {@link Order} entities.
     *
     * @param dtos the list of {@link OrderSearchDto} objects
     * @return a list of corresponding {@link Order} entities
     */
    public List<Order> toEntityList(final List<OrderSearchDto> dtos) {
        List<Order> entities = new ArrayList<>();
        for (OrderSearchDto dto : dtos) {
            entities.add(toEntity(dto));
        }
        return entities;
    }

    /**
     * Converts a list of {@link Order} entities to a list of {@link OrderSearchDto} objects.
     *
     * @param entities the list of {@link Order} entities
     * @return a list of corresponding {@link OrderSearchDto} objects
     */
    public List<OrderSearchDto> toDtoList(final List<Order> entities) {
        List<OrderSearchDto> dtos = new ArrayList<>();
        for (Order entity : entities) {
            dtos.add(toDto(entity));
        }
        return dtos;
    }
}
