package io.github.strogolsky.profinder.core.service;

import io.github.strogolsky.profinder.common.exception.StaleVersionException;
import io.github.strogolsky.profinder.common.exception.StatusConflictException;
import io.github.strogolsky.profinder.common.security.UserContext;
import io.github.strogolsky.profinder.core.entity.CustomerProfile;
import io.github.strogolsky.profinder.core.entity.Location;
import io.github.strogolsky.profinder.core.entity.Order;
import io.github.strogolsky.profinder.core.entity.OrderStatus;
import io.github.strogolsky.profinder.core.repository.OrderRepository;
import io.github.strogolsky.profinder.core.search.OrderSearchMapper;
import io.github.strogolsky.profinder.core.search.OrderSearchService;
import jakarta.ws.rs.ForbiddenException;
import jakarta.ws.rs.NotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class OrderServiceTest {

    private static final UUID ORDER_ID = UUID.randomUUID();
    private static final UUID MISSING_ORDER_ID = UUID.randomUUID();
    private static final UUID CUSTOMER_ID = UUID.randomUUID();
    private static final UUID LOCATION_ID = UUID.randomUUID();
    private static final int PRICE = 100;
    private static final int NEW_PRICE = 250;
    private static final long VERSION = 3L;

    private OrderRepository orderRepository;
    private OrderSearchService orderSearchService;
    private UserContext userContext;
    private OrderService service;
    private Order order;

    @BeforeEach
    void setUp() {
        orderRepository = mock(OrderRepository.class);
        orderSearchService = mock(OrderSearchService.class);
        userContext = mock(UserContext.class);
        service = new OrderService(
                orderSearchService,
                mock(OrderSearchMapper.class),
                orderRepository,
                mock(BidService.class),
                mock(ProfessionalService.class),
                userContext);

        CustomerProfile customer = new CustomerProfile();
        customer.setId(CUSTOMER_ID);
        Location location = new Location();
        location.setId(LOCATION_ID);

        order = new Order();
        order.setId(ORDER_ID);
        order.setCustomer(customer);
        order.setStatus(OrderStatus.CREATED);
        order.setTitle("Old title");
        order.setDescription("Old description");
        order.setPrice(PRICE);
        order.setLocation(location);
        order.setVersion(VERSION);

        when(orderRepository.findById(ORDER_ID)).thenReturn(order);
        when(userContext.is(CUSTOMER_ID)).thenReturn(true);
    }

    private static Order changes() {
        Order changes = new Order();
        changes.setVersion(VERSION);
        return changes;
    }

    @Test
    void updateWithAStaleVersionIsRejectedAndChangesNothing() {
        Order changes = changes();
        changes.setVersion(VERSION - 1);
        changes.setTitle("New title");

        assertThrows(StaleVersionException.class, () -> service.update(ORDER_ID, changes));

        assertEquals("Old title", order.getTitle());
        verify(orderRepository, never()).persist(any(Order.class));
        verify(orderSearchService, never()).save(any());
    }

    @Test
    void updateWithoutAVersionIsRejected() {
        Order changes = new Order();
        changes.setTitle("New title");

        assertThrows(StaleVersionException.class, () -> service.update(ORDER_ID, changes));
    }

    @Test
    void updateChangesOnlyTheSentFields() {
        Order changes = changes();
        changes.setTitle("New title");
        changes.setPrice(NEW_PRICE);

        Order result = service.update(ORDER_ID, changes);

        assertSame(order, result);
        assertEquals("New title", result.getTitle());
        assertEquals(NEW_PRICE, result.getPrice());
        assertEquals("Old description", result.getDescription());
        assertEquals(LOCATION_ID, result.getLocation().getId());
        assertEquals(OrderStatus.CREATED, result.getStatus());
        verify(orderRepository).persist(order);
        verify(orderSearchService).save(any());
    }

    @Test
    void updateWithStatusCancelledCancelsTheOrder() {
        Order changes = changes();
        changes.setStatus(OrderStatus.CANCELLED);

        Order result = service.update(ORDER_ID, changes);

        assertEquals(OrderStatus.CANCELLED, result.getStatus());
        assertEquals("Old title", result.getTitle());
        assertEquals(PRICE, result.getPrice());
    }

    @Test
    void updateRejectsAnyStatusExceptCancelled() {
        Order changes = changes();
        changes.setStatus(OrderStatus.COMPLETED);

        assertThrows(StatusConflictException.class, () -> service.update(ORDER_ID, changes));

        assertEquals(OrderStatus.CREATED, order.getStatus());
        verify(orderRepository, never()).persist(any(Order.class));
    }

    @Test
    void updateRejectsCancellingACompletedOrder() {
        order.setStatus(OrderStatus.COMPLETED);
        Order changes = changes();
        changes.setStatus(OrderStatus.CANCELLED);

        StatusConflictException exception =
                assertThrows(StatusConflictException.class, () -> service.update(ORDER_ID, changes));

        assertEquals(409, exception.getResponse().getStatus());
        assertEquals(OrderStatus.COMPLETED, order.getStatus());
    }

    @Test
    void updateIsForbiddenForANonOwner() {
        when(userContext.is(CUSTOMER_ID)).thenReturn(false);
        Order changes = changes();
        changes.setTitle("Hijacked");

        assertThrows(ForbiddenException.class, () -> service.update(ORDER_ID, changes));

        assertEquals("Old title", order.getTitle());
        verify(orderRepository, never()).persist(any(Order.class));
    }

    @Test
    void updateOfAMissingOrderIsNotFound() {
        Order changes = changes();

        assertThrows(NotFoundException.class, () -> service.update(MISSING_ORDER_ID, changes));
    }

    @Test
    void nextStatusKeepsTheCurrentStatusWhenNoneIsRequested() {
        assertEquals(OrderStatus.CUSTOMER_PENDING, OrderService.nextStatus(OrderStatus.CUSTOMER_PENDING, null));
    }

    @Test
    void nextStatusAllowsCancellingAnOrderWithBids() {
        assertEquals(OrderStatus.CANCELLED,
                OrderService.nextStatus(OrderStatus.CUSTOMER_PENDING, OrderStatus.CANCELLED));
    }
}
