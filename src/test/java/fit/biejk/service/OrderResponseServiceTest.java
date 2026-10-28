package fit.biejk.service;

import fit.biejk.dto.OrderResponseUpdateRequest;
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
    private static final long CUSTOMER_ID = 1L;
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

    private OrderResponseUpdateRequest request(final OrderResponseStatus status) {
        return new OrderResponseUpdateRequest(status, LocalDateTime.now().plusDays(1), FINAL_PRICE);
    }

    @Test
    void approvingAResponseCompletesTheOrderAndRejectsTheOthers() {
        Order result = service.approve(RESPONSE_ID, request(OrderResponseStatus.APPROVED));

        assertSame(order, result);
        assertEquals(OrderStatus.COMPLETED, order.getStatus());
        assertEquals(FINAL_PRICE, order.getPrice());
        assertEquals(OrderResponseStatus.APPROVED, orderResponse.getStatus());
        verify(responseRepository).rejectOthersForOrder(ORDER_ID, RESPONSE_ID);
    }

    @Test
    void anyStatusExceptApprovedIsAConflict() {
        OrderResponseUpdateRequest request = request(OrderResponseStatus.REJECTED);

        assertThrows(StatusConflictException.class, () -> service.approve(RESPONSE_ID, request));

        assertEquals(OrderStatus.CUSTOMER_PENDING, order.getStatus());
        assertEquals(OrderResponseStatus.CREATED, orderResponse.getStatus());
        verify(responseRepository, never()).rejectOthersForOrder(any(), any());
    }

    @Test
    void onlyTheOrderOwnerCanApprove() {
        when(authService.isCurrentUser(CUSTOMER_ID)).thenReturn(false);
        OrderResponseUpdateRequest request = request(OrderResponseStatus.APPROVED);

        assertThrows(ForbiddenException.class, () -> service.approve(RESPONSE_ID, request));

        assertEquals(OrderStatus.CUSTOMER_PENDING, order.getStatus());
    }

    @Test
    void approvingAMissingResponseIsNotFound() {
        OrderResponseUpdateRequest request = request(OrderResponseStatus.APPROVED);

        assertThrows(NotFoundException.class, () -> service.approve(999L, request));
    }
}
