package io.github.strogolsky.profinder.core.service;

import io.github.strogolsky.profinder.common.exception.StaleVersionException;
import io.github.strogolsky.profinder.common.exception.StatusConflictException;
import io.github.strogolsky.profinder.common.security.UserContext;
import io.github.strogolsky.profinder.core.entity.Bid;
import io.github.strogolsky.profinder.core.entity.BidStatus;
import io.github.strogolsky.profinder.core.entity.CustomerProfile;
import io.github.strogolsky.profinder.core.entity.Order;
import io.github.strogolsky.profinder.core.entity.OrderStatus;
import io.github.strogolsky.profinder.core.repository.BidRepository;
import io.github.strogolsky.profinder.core.repository.OrderRepository;
import io.github.strogolsky.profinder.core.search.OrderSearchMapper;
import io.github.strogolsky.profinder.core.search.OrderSearchService;
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
    private UserContext userContext;
    private BidService service;
    private Order order;
    private Bid bid;

    @BeforeEach
    void setUp() {
        responseRepository = mock(BidRepository.class);
        userContext = mock(UserContext.class);
        service = new BidService(
                mock(OrderRepository.class),
                userContext,
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
        when(userContext.is(CUSTOMER_ID)).thenReturn(true);
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
        LocalDateTime deadline = LocalDateTime.now().plusDays(1);

        assertThrows(StaleVersionException.class,
                () -> service.updateStatus(RESPONSE_ID, BidStatus.APPROVED, FINAL_PRICE, deadline, VERSION - 1));

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
        when(userContext.is(CUSTOMER_ID)).thenReturn(false);

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
