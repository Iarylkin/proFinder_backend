package fit.biejk.entity;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class OrderStatusTest {

    @Test
    void openOrdersAcceptProposals() {
        assertTrue(OrderStatus.CREATED.acceptsProposals());
        assertTrue(OrderStatus.CLIENT_PENDING.acceptsProposals());
    }

    @Test
    void closedOrdersDoNotAcceptProposals() {
        assertFalse(OrderStatus.CANCELLED.acceptsProposals());
        assertFalse(OrderStatus.COMPLETED.acceptsProposals());
    }
}
