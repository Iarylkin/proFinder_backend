package fit.biejk.entity;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class OrderStatusTest {

    @Test
    void openOrdersAcceptResponses() {
        assertTrue(OrderStatus.CREATED.acceptsResponses());
        assertTrue(OrderStatus.CUSTOMER_PENDING.acceptsResponses());
    }

    @Test
    void closedOrdersDoNotAcceptResponses() {
        assertFalse(OrderStatus.CANCELLED.acceptsResponses());
        assertFalse(OrderStatus.COMPLETED.acceptsResponses());
    }
}
