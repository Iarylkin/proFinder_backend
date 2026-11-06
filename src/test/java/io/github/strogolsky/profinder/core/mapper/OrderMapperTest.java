package io.github.strogolsky.profinder.core.mapper;

import io.github.strogolsky.profinder.core.dto.OrderCreateRequest;
import io.github.strogolsky.profinder.core.dto.OrderUpdateRequest;
import io.github.strogolsky.profinder.core.entity.Order;
import io.github.strogolsky.profinder.core.entity.OrderStatus;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class OrderMapperTest {

    private static final int PRICE = 250;

    private final OrderMapper mapper = new OrderMapperImpl();

    @Test
    void createRequestBecomesANewOrderWithoutTheFieldsOfTheServer() {
        OrderCreateRequest request = new OrderCreateRequest(null, "New title", "New description", PRICE, null);

        Order order = mapper.toEntity(request);

        assertEquals("New title", order.getTitle());
        assertEquals("New description", order.getDescription());
        assertEquals(PRICE, order.getPrice());
        assertNull(order.getId());
        assertNull(order.getVersion());
        assertNull(order.getStatus());
        assertNull(order.getCustomer());
        assertNull(order.getLocation());
    }

    @Test
    void updateRequestKeepsUnsentFieldsNull() {
        OrderUpdateRequest request = new OrderUpdateRequest(null, null, null, "New title", null, null, null, 0L);

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
        OrderUpdateRequest request = new OrderUpdateRequest(
                null, OrderStatus.CANCELLED, null, null, PRICE, null, null, 0L);

        Order changes = mapper.toEntity(request);

        assertEquals(PRICE, changes.getPrice());
        assertEquals(OrderStatus.CANCELLED, changes.getStatus());
    }
}
