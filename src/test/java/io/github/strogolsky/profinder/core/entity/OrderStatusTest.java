package io.github.strogolsky.profinder.core.entity;

import io.github.strogolsky.profinder.common.exception.StatusConflictException;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
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

    @Test
    void allowedTransitionsAreExactlyTheOnesOfTheLifecycle() {
        Map<OrderStatus, Set<OrderStatus>> allowed = Map.of(
                OrderStatus.CREATED, Set.of(OrderStatus.CANCELLED, OrderStatus.CUSTOMER_PENDING),
                OrderStatus.CUSTOMER_PENDING,
                Set.of(OrderStatus.CUSTOMER_PENDING, OrderStatus.CANCELLED, OrderStatus.COMPLETED),
                OrderStatus.COMPLETED, Set.of(),
                OrderStatus.CANCELLED, Set.of());

        for (OrderStatus from : OrderStatus.values()) {
            for (OrderStatus to : OrderStatus.values()) {
                assertEquals(allowed.get(from).contains(to), from.isValidTransition(to), from + " -> " + to);
            }
        }
    }

    @Test
    void transitionReturnsTheNewStatusWhenItIsAllowed() {
        assertEquals(OrderStatus.COMPLETED, OrderStatus.CUSTOMER_PENDING.transitionTo(OrderStatus.COMPLETED));
        assertEquals(OrderStatus.CUSTOMER_PENDING, OrderStatus.CREATED.transitionTo(OrderStatus.CUSTOMER_PENDING));
    }

    @Test
    void forbiddenTransitionIsAConflictThatNamesBothStatuses() {
        StatusConflictException thrown = assertThrows(StatusConflictException.class,
                () -> OrderStatus.CANCELLED.transitionTo(OrderStatus.COMPLETED));

        assertEquals(409, thrown.getResponse().getStatus());
        assertEquals("Cannot change the status from CANCELLED to COMPLETED", thrown.getMessage());
    }

    @Test
    void finalStatusesCannotBeLeft() {
        for (OrderStatus to : OrderStatus.values()) {
            assertThrows(StatusConflictException.class, () -> OrderStatus.COMPLETED.transitionTo(to));
            assertThrows(StatusConflictException.class, () -> OrderStatus.CANCELLED.transitionTo(to));
        }
    }
}
