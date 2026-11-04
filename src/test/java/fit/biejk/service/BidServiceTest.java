package fit.biejk.service;

import fit.biejk.entity.CustomerProfile;
import fit.biejk.entity.Order;
import fit.biejk.entity.Bid;
import fit.biejk.entity.OrderStatus;
import fit.biejk.entity.BidStatus;
import fit.biejk.exception.StaleVersionException;
import fit.biejk.exception.StatusConflictException;
import fit.biejk.repository.BidRepository;
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

class BidServiceTest {

    private static final UUID ORDER_ID = UUID.randomUUID();
    private static final UUID RESPONSE_ID = UUID.randomUUID();
    private static final UUID MISSING_RESPONSE_ID = UUID.randomUUID();
    private static final UUID CUSTOMER_ID = UUID.randomUUID();
    private static final int FINAL_PRICE = 300;
    private static final long VERSION = 5L;

    private BidRepository responseRepository;
    private AuthService authService;
    private BidService service;
    private Order order;
    private Bid bid;

    @BeforeEach
    void setUp() {
        responseRepository = mock(BidRepository.class);
        authService = mock(AuthService.class);
        service = new BidService(
                mock(OrderRepository.class),
                authService,
                mock(OrderSearchService.class),
                mock(OrderSearchMapper.class),
                responseRepository);

        CustomerProfile customer = new CustomerProfile();
        customer.setId(CUSTOMER_ID);

        order = new Order();
        order.setId(ORDER_ID);
        order.setCustomer(customer);
        order.setStatus(OrderStatus.CUSTOMER_PENDING);

        bid = new Bid();
        bid.setId(RESPONSE_ID);
        bid.setOrder(order);
        bid.setStatus(BidStatus.CREATED);
        bid.setVersion(VERSION);

        when(responseRepository.findByIdOptional(RESPONSE_ID)).thenReturn(Optional.of(bid));
        when(authService.isCurrentUser(CUSTOMER_ID)).thenReturn(true);
    }

    private Order updateStatus(final UUID responseId, final BidStatus status) {
        return service.updateStatus(responseId, status, FINAL_PRICE, LocalDateTime.now().plusDays(1), VERSION);
    }

    @Test
    void approvingAResponseCompletesTheOrderAndRejectsTheOthers() {
        Order result = updateStatus(RESPONSE_ID, BidStatus.APPROVED);

        assertSame(order, result);
        assertEquals(OrderStatus.COMPLETED, order.getStatus());
        assertEquals(FINAL_PRICE, order.getPrice());
        assertEquals(BidStatus.APPROVED, bid.getStatus());
        verify(responseRepository).rejectOthersForOrder(ORDER_ID, RESPONSE_ID);
    }

    @Test
    void approvingWithAStaleVersionIsRejectedAndChangesNothing() {
        assertThrows(StaleVersionException.class, () -> service.updateStatus(
                RESPONSE_ID, BidStatus.APPROVED, FINAL_PRICE, LocalDateTime.now().plusDays(1), VERSION - 1));

        assertEquals(OrderStatus.CUSTOMER_PENDING, order.getStatus());
        assertEquals(BidStatus.CREATED, bid.getStatus());
        verify(responseRepository, never()).rejectOthersForOrder(any(), any());
    }

    @Test
    void anyStatusExceptApprovedIsAConflict() {
        assertThrows(StatusConflictException.class,
                () -> updateStatus(RESPONSE_ID, BidStatus.REJECTED));

        assertEquals(OrderStatus.CUSTOMER_PENDING, order.getStatus());
        assertEquals(BidStatus.CREATED, bid.getStatus());
        verify(responseRepository, never()).rejectOthersForOrder(any(), any());
    }

    @Test
    void onlyTheOrderOwnerCanApprove() {
        when(authService.isCurrentUser(CUSTOMER_ID)).thenReturn(false);

        assertThrows(ForbiddenException.class,
                () -> updateStatus(RESPONSE_ID, BidStatus.APPROVED));

        assertEquals(OrderStatus.CUSTOMER_PENDING, order.getStatus());
    }

    @Test
    void approvingAMissingResponseIsNotFound() {
        assertThrows(NotFoundException.class,
                () -> updateStatus(MISSING_RESPONSE_ID, BidStatus.APPROVED));
    }
}
