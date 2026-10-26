package fit.biejk.search;

import fit.biejk.entity.Order;
import fit.biejk.entity.OrderStatus;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class OrderSearchMapperTest {

    private final OrderSearchMapper mapper = new OrderSearchMapper();

    @Test
    void searchDocumentContainsTitleAndDescription() {
        Order order = new Order();
        order.setStatus(OrderStatus.CREATED);
        order.setTitle("Fix a leaking tap");
        order.setDescription("Kitchen tap leaks");

        OrderSearchDto dto = mapper.toDto(order);

        assertEquals("Fix a leaking tap", dto.getTitle());
        assertEquals("Kitchen tap leaks", dto.getDescription());
        assertNotNull(dto.getCreatedAt());
    }
}
