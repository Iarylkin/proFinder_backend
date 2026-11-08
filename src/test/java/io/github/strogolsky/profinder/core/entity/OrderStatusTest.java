package io.github.strogolsky.profinder.core.entity;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class OrderStatusTest {

    @Test
    void openOrdersAcceptBids() {
        assertTrue(OrderStatus.CREATED.acceptsBids());
        assertTrue(OrderStatus.CUSTOMER_PENDING.acceptsBids());
    }

    @Test
    void closedOrdersDoNotAcceptBids() {
        assertFalse(OrderStatus.CANCELLED.acceptsBids());
        assertFalse(OrderStatus.COMPLETED.acceptsBids());
    }
}
