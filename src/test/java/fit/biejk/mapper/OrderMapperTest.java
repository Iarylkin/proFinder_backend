package fit.biejk.mapper;

import fit.biejk.dto.OrderUpdateRequest;
import fit.biejk.entity.Order;
import fit.biejk.entity.OrderStatus;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class OrderMapperTest {

    private static final int PRICE = 250;

    private final OrderMapper mapper = new OrderMapperImpl();

    @Test
    void updateRequestKeepsUnsentFieldsNull() {
        OrderUpdateRequest request = new OrderUpdateRequest();
        request.setTitle("New title");

        Order changes = mapper.toEntity(request);

        assertEquals("New title", changes.getTitle());
        assertNull(changes.getDescription());
        assertNull(changes.getPrice());
        assertNull(changes.getDeadline());
        assertNull(changes.getLocation());
        assertNull(changes.getCategories());
        assertNull(changes.getStatus());
    }

    @Test
    void updateRequestCarriesPriceAndRequestedStatus() {
        OrderUpdateRequest request = new OrderUpdateRequest();
        request.setPrice(PRICE);
        request.setStatus(OrderStatus.CANCELLED);

        Order changes = mapper.toEntity(request);

        assertEquals(PRICE, changes.getPrice());
        assertEquals(OrderStatus.CANCELLED, changes.getStatus());
    }
}
