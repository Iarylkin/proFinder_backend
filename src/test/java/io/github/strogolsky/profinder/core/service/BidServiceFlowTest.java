package io.github.strogolsky.profinder.core.service;

import io.github.strogolsky.profinder.common.exception.StatusConflictException;
import io.github.strogolsky.profinder.common.security.UserContext;
import io.github.strogolsky.profinder.core.entity.Bid;
import io.github.strogolsky.profinder.core.entity.BidStatus;
import io.github.strogolsky.profinder.core.entity.CustomerProfile;
import io.github.strogolsky.profinder.core.entity.Order;
import io.github.strogolsky.profinder.core.entity.OrderStatus;
import io.github.strogolsky.profinder.core.entity.ProfessionalProfile;
import io.github.strogolsky.profinder.core.repository.BidRepository;
import io.github.strogolsky.profinder.core.repository.OrderRepository;
import io.github.strogolsky.profinder.core.search.OrderSearchDto;
import io.github.strogolsky.profinder.core.search.OrderSearchMapper;
import io.github.strogolsky.profinder.core.search.OrderSearchService;
import jakarta.ws.rs.ForbiddenException;
import jakarta.ws.rs.NotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * The bids of an order outside of the plain approval (that one is in {@link BidServiceTest}): creating a
 * bid, who may read it, and the rule that a bid cannot be approved on an order that is already closed.
 * An order is closed when it is {@code COMPLETED} or {@code CANCELLED}: those states are final
 * ({@code OrderStatus}), so neither can be left by an approval.
 */
class BidServiceFlowTest {

    private static final UUID ORDER_ID = UUID.randomUUID();
    private static final UUID BID_ID = UUID.randomUUID();
    private static final UUID CUSTOMER_ID = UUID.randomUUID();
    private static final UUID AUTHOR_ID = UUID.randomUUID();
    private static final UUID STRANGER_ID = UUID.randomUUID();
    private static final int OLD_PRICE = 100;
    private static final int FINAL_PRICE = 300;
    private static final int PAGE = 2;
    private static final int SIZE = 10;
    private static final long VERSION = 5L;

    private BidRepository bidRepository;
    private OrderRepository orderRepository;
    private OrderSearchService orderSearchService;
    private OrderSearchMapper orderSearchMapper;
    private UserContext userContext;
    private BidService service;

    private Order order;
    private Bid bid;

    @BeforeEach
    void setUp() {
        bidRepository = mock(BidRepository.class);
        orderRepository = mock(OrderRepository.class);
        orderSearchService = mock(OrderSearchService.class);
        orderSearchMapper = mock(OrderSearchMapper.class);
        userContext = mock(UserContext.class);
        service = new BidService(orderRepository, userContext, orderSearchService, orderSearchMapper,
                bidRepository);

        CustomerProfile customer = new CustomerProfile();
        customer.setId(CUSTOMER_ID);
        order = new Order();
        order.setId(ORDER_ID);
        order.setCustomer(customer);
        order.setStatus(OrderStatus.CUSTOMER_PENDING);
        order.setPrice(OLD_PRICE);

        ProfessionalProfile author = new ProfessionalProfile();
        author.setId(AUTHOR_ID);
        bid = new Bid();
        bid.setId(BID_ID);
        bid.setOrder(order);
        bid.setProfessional(author);
        bid.setStatus(BidStatus.CREATED);
        bid.setVersion(VERSION);

        when(bidRepository.findByIdOptional(BID_ID)).thenReturn(Optional.of(bid));
        when(bidRepository.findById(BID_ID)).thenReturn(bid);
        when(orderRepository.findById(ORDER_ID)).thenReturn(order);
        when(userContext.is(CUSTOMER_ID)).thenReturn(true);
    }

    private Order approve() {
        return service.updateStatus(BID_ID, BidStatus.APPROVED, FINAL_PRICE,
                LocalDateTime.now(ZoneId.systemDefault()).plusDays(1), VERSION);
    }

    // ---- create ----

    @Test
    void newBidIsSavedWithStatusCreated() {
        Bid created = new Bid();
        created.setOrder(order);
        created.setProfessional(bid.getProfessional());
        created.setStatus(BidStatus.APPROVED);

        Bid result = service.create(created);

        assertSame(created, result);
        assertEquals(BidStatus.CREATED, result.getStatus());
        verify(bidRepository).persist(created);
    }

    // ---- approval ----

    @Test
    void approvedOrderIsSavedAndIndexedAndGetsTheFinalTerms() {
        OrderSearchDto document = new OrderSearchDto();
        when(orderSearchMapper.toDto(order)).thenReturn(document);
        LocalDateTime deadline = LocalDateTime.now(ZoneId.systemDefault()).plusDays(3);

        service.updateStatus(BID_ID, BidStatus.APPROVED, FINAL_PRICE, deadline, VERSION);

        assertEquals(deadline, order.getDeadline());
        verify(orderRepository).persist(order);
        verify(orderSearchService).save(document);
    }

    @Test
    void bidOfACancelledOrderCannotBeApproved() {
        order.setStatus(OrderStatus.CANCELLED);

        assertThrows(StatusConflictException.class, this::approve);

        assertEquals(OrderStatus.CANCELLED, order.getStatus());
        assertEquals(OLD_PRICE, order.getPrice());
        assertEquals(BidStatus.CREATED, bid.getStatus());
        verify(bidRepository, never()).rejectOthersForOrder(any(UUID.class), any(UUID.class));
        verify(orderSearchService, never()).save(any(OrderSearchDto.class));
    }

    @Test
    void bidOfACompletedOrderCannotBeApprovedAgain() {
        order.setStatus(OrderStatus.COMPLETED);

        assertThrows(StatusConflictException.class, this::approve);

        assertEquals(OLD_PRICE, order.getPrice());
        verify(bidRepository, never()).rejectOthersForOrder(any(UUID.class), any(UUID.class));
    }

    // ---- read one ----

    @Test
    void bidIsFoundById() {
        assertSame(bid, service.getById(BID_ID));
    }

    @Test
    void missingBidIsNotFound() {
        assertThrows(NotFoundException.class, () -> service.getById(UUID.randomUUID()));
    }

    @Test
    void ownerOfTheOrderMayReadTheBid() {
        assertSame(bid, service.getByIdForCurrentUser(BID_ID));
    }

    @Test
    void authorOfTheBidMayReadTheBid() {
        when(userContext.is(CUSTOMER_ID)).thenReturn(false);
        when(userContext.is(AUTHOR_ID)).thenReturn(true);

        assertSame(bid, service.getByIdForCurrentUser(BID_ID));
    }

    @Test
    void anotherUserMayNotReadTheBid() {
        when(userContext.is(CUSTOMER_ID)).thenReturn(false);
        when(userContext.is(STRANGER_ID)).thenReturn(true);

        assertThrows(ForbiddenException.class, () -> service.getByIdForCurrentUser(BID_ID));
    }

    // ---- read lists ----

    @Test
    void ownerOfTheOrderReadsItsBids() {
        when(bidRepository.findByOrderId(ORDER_ID, PAGE, SIZE)).thenReturn(List.of(bid));

        assertEquals(List.of(bid), service.getByOrderId(ORDER_ID, PAGE, SIZE));
    }

    @Test
    void anotherCustomerMayNotReadTheBidsOfTheOrder() {
        when(userContext.is(CUSTOMER_ID)).thenReturn(false);

        assertThrows(ForbiddenException.class, () -> service.getByOrderId(ORDER_ID, PAGE, SIZE));

        verify(bidRepository, never()).findByOrderId(any(UUID.class), anyInt(), anyInt());
    }

    @Test
    void bidsOfAMissingOrderAreNotFound() {
        assertThrows(NotFoundException.class, () -> service.getByOrderId(UUID.randomUUID(), PAGE, SIZE));
    }

    @Test
    void professionalReadsOwnBids() {
        when(userContext.is(AUTHOR_ID)).thenReturn(true);
        when(bidRepository.findByProfessionalId(AUTHOR_ID, PAGE, SIZE)).thenReturn(List.of(bid));

        assertEquals(List.of(bid), service.getByProfessionalId(AUTHOR_ID, PAGE, SIZE));
    }

    @Test
    void professionalMayNotReadBidsOfAnotherProfessional() {
        assertThrows(ForbiddenException.class, () -> service.getByProfessionalId(AUTHOR_ID, PAGE, SIZE));

        verify(bidRepository, never()).findByProfessionalId(any(UUID.class), anyInt(), anyInt());
    }
}
