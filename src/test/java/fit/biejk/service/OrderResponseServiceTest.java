package fit.biejk.service;

import fit.biejk.entity.Customer;
import fit.biejk.entity.Order;
import fit.biejk.entity.OrderResponse;
import fit.biejk.entity.OrderStatus;
import fit.biejk.entity.OrderResponseStatus;
import fit.biejk.exception.StatusConflictException;
import fit.biejk.repository.OrderResponseRepository;
import fit.biejk.repository.OrderRepository;
import fit.biejk.search.OrderSearchMapper;
import fit.biejk.search.OrderSearchService;
import jakarta.ws.rs.ForbiddenException;
import jakarta.ws.rs.NotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class OrderResponseServiceTest {

    private static final long ORDER_ID = 10L;
    private static final long RESPONSE_ID = 20L;
    private static final UUID CUSTOMER_ID = UUID.randomUUID();
    private static final int FINAL_PRICE = 300;

    private OrderResponseRepository responseRepository;
    private AuthService authService;
    private OrderResponseService service;
    private Order order;
    private OrderResponse orderResponse;

    @BeforeEach
    void setUp() {
        responseRepository = mock(OrderResponseRepository.class);
        authService = mock(AuthService.class);
        service = new OrderResponseService(
                mock(OrderRepository.class),
                authService,
                mock(OrderSearchService.class),
                mock(OrderSearchMapper.class),
                responseRepository);

        Customer customer = new Customer();
        customer.setId(CUSTOMER_ID);

        order = new Order();
        order.setId(ORDER_ID);
        order.setCustomer(customer);
        order.setStatus(OrderStatus.CUSTOMER_PENDING);

        orderResponse = new OrderResponse();
        orderResponse.setId(RESPONSE_ID);
        orderResponse.setOrder(order);
        orderResponse.setStatus(OrderResponseStatus.CREATED);

        when(responseRepository.findByIdOptional(RESPONSE_ID)).thenReturn(Optional.of(orderResponse));
        when(authService.isCurrentUser(CUSTOMER_ID)).thenReturn(true);
    }

    private Order updateStatus(final long responseId, final OrderResponseStatus status) {
        return service.updateStatus(responseId, status, FINAL_PRICE, LocalDateTime.now().plusDays(1));
    }

    @Test
    void approvingAResponseCompletesTheOrderAndRejectsTheOthers() {
        Order result = updateStatus(RESPONSE_ID, OrderResponseStatus.APPROVED);

        assertSame(order, result);
        assertEquals(OrderStatus.COMPLETED, order.getStatus());
        assertEquals(FINAL_PRICE, order.getPrice());
        assertEquals(OrderResponseStatus.APPROVED, orderResponse.getStatus());
        verify(responseRepository).rejectOthersForOrder(ORDER_ID, RESPONSE_ID);
    }

    @Test
    void anyStatusExceptApprovedIsAConflict() {
        assertThrows(StatusConflictException.class,
                () -> updateStatus(RESPONSE_ID, OrderResponseStatus.REJECTED));

        assertEquals(OrderStatus.CUSTOMER_PENDING, order.getStatus());
        assertEquals(OrderResponseStatus.CREATED, orderResponse.getStatus());
        verify(responseRepository, never()).rejectOthersForOrder(any(), any());
    }

    @Test
    void onlyTheOrderOwnerCanApprove() {
        when(authService.isCurrentUser(CUSTOMER_ID)).thenReturn(false);

        assertThrows(ForbiddenException.class,
                () -> updateStatus(RESPONSE_ID, OrderResponseStatus.APPROVED));

        assertEquals(OrderStatus.CUSTOMER_PENDING, order.getStatus());
    }

    @Test
    void approvingAMissingResponseIsNotFound() {
        assertThrows(NotFoundException.class,
                () -> updateStatus(999L, OrderResponseStatus.APPROVED));
    }
}
