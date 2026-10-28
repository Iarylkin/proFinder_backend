package fit.biejk.service;

import fit.biejk.entity.Customer;
import fit.biejk.entity.Location;
import fit.biejk.entity.Order;
import fit.biejk.entity.OrderStatus;
import fit.biejk.exception.StatusConflictException;
import fit.biejk.repository.OrderRepository;
import fit.biejk.search.OrderSearchMapper;
import fit.biejk.search.OrderSearchService;
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

    private OrderRepository orderRepository;
    private OrderSearchService orderSearchService;
    private AuthService authService;
    private OrderService service;
    private Order order;

    @BeforeEach
    void setUp() {
        orderRepository = mock(OrderRepository.class);
        orderSearchService = mock(OrderSearchService.class);
        authService = mock(AuthService.class);
        service = new OrderService(
                orderSearchService,
                mock(OrderSearchMapper.class),
                orderRepository,
                mock(OrderResponseService.class),
                mock(ProfessionalService.class),
                authService);

        Customer customer = new Customer();
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

        when(orderRepository.findById(ORDER_ID)).thenReturn(order);
        when(authService.isCurrentUser(CUSTOMER_ID)).thenReturn(true);
    }

    @Test
    void updateChangesOnlyTheSentFields() {
        Order changes = new Order();
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
        Order changes = new Order();
        changes.setStatus(OrderStatus.CANCELLED);

        Order result = service.update(ORDER_ID, changes);

        assertEquals(OrderStatus.CANCELLED, result.getStatus());
        assertEquals("Old title", result.getTitle());
        assertEquals(PRICE, result.getPrice());
    }

    @Test
    void updateRejectsAnyStatusExceptCancelled() {
        Order changes = new Order();
        changes.setStatus(OrderStatus.COMPLETED);

        assertThrows(StatusConflictException.class, () -> service.update(ORDER_ID, changes));

        assertEquals(OrderStatus.CREATED, order.getStatus());
        verify(orderRepository, never()).persist(any(Order.class));
    }

    @Test
    void updateRejectsCancellingACompletedOrder() {
        order.setStatus(OrderStatus.COMPLETED);
        Order changes = new Order();
        changes.setStatus(OrderStatus.CANCELLED);

        StatusConflictException exception =
                assertThrows(StatusConflictException.class, () -> service.update(ORDER_ID, changes));

        assertEquals(409, exception.getResponse().getStatus());
        assertEquals(OrderStatus.COMPLETED, order.getStatus());
    }

    @Test
    void updateIsForbiddenForANonOwner() {
        when(authService.isCurrentUser(CUSTOMER_ID)).thenReturn(false);
        Order changes = new Order();
        changes.setTitle("Hijacked");

        assertThrows(ForbiddenException.class, () -> service.update(ORDER_ID, changes));

        assertEquals("Old title", order.getTitle());
        verify(orderRepository, never()).persist(any(Order.class));
    }

    @Test
    void updateOfAMissingOrderIsNotFound() {
        Order changes = new Order();

        assertThrows(NotFoundException.class, () -> service.update(MISSING_ORDER_ID, changes));
    }

    @Test
    void nextStatusKeepsTheCurrentStatusWhenNoneIsRequested() {
        assertEquals(OrderStatus.CUSTOMER_PENDING, OrderService.nextStatus(OrderStatus.CUSTOMER_PENDING, null));
    }

    @Test
    void nextStatusAllowsCancellingAnOrderWithResponses() {
        assertEquals(OrderStatus.CANCELLED,
                OrderService.nextStatus(OrderStatus.CUSTOMER_PENDING, OrderStatus.CANCELLED));
    }
}
