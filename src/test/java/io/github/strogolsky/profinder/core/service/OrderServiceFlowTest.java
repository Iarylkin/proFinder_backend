package io.github.strogolsky.profinder.core.service;

import io.github.strogolsky.profinder.common.security.UserContext;
import io.github.strogolsky.profinder.core.entity.Bid;
import io.github.strogolsky.profinder.core.entity.CustomerProfile;
import io.github.strogolsky.profinder.core.entity.Order;
import io.github.strogolsky.profinder.core.entity.OrderStatus;
import io.github.strogolsky.profinder.core.entity.ProfessionalProfile;
import io.github.strogolsky.profinder.core.exception.OrderClosedException;
import io.github.strogolsky.profinder.core.repository.OrderRepository;
import io.github.strogolsky.profinder.core.search.OrderSearchDto;
import io.github.strogolsky.profinder.core.search.OrderSearchMapper;
import io.github.strogolsky.profinder.core.search.OrderSearchService;
import io.quarkus.hibernate.orm.panache.PanacheQuery;
import jakarta.ws.rs.ClientErrorException;
import jakarta.ws.rs.ForbiddenException;
import jakarta.ws.rs.NotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * The life of an order outside of its partial update (that one is in {@link OrderServiceTest}): creating,
 * deleting, reading, and the responses (bids) of professionals. The expected behaviour is the one of the
 * order lifecycle in CLAUDE.md: a new order is {@code CREATED}; a bid moves it to {@code CUSTOMER_PENDING};
 * a {@code COMPLETED} or {@code CANCELLED} order takes no bids; a professional bids once per order.
 */
class OrderServiceFlowTest {

    private static final UUID ORDER_ID = UUID.randomUUID();
    private static final UUID CUSTOMER_ID = UUID.randomUUID();
    private static final UUID PROFESSIONAL_ID = UUID.randomUUID();
    private static final UUID OTHER_PROFESSIONAL_ID = UUID.randomUUID();
    private static final int PAGE = 2;
    private static final int SIZE = 10;

    private OrderRepository orderRepository;
    private OrderSearchService orderSearchService;
    private OrderSearchMapper orderSearchMapper;
    private BidService bidService;
    private ProfessionalService professionalService;
    private UserContext userContext;
    private OrderService service;

    private Order order;
    private OrderSearchDto document;

    @BeforeEach
    void setUp() {
        orderRepository = mock(OrderRepository.class);
        orderSearchService = mock(OrderSearchService.class);
        orderSearchMapper = mock(OrderSearchMapper.class);
        bidService = mock(BidService.class);
        professionalService = mock(ProfessionalService.class);
        userContext = mock(UserContext.class);
        service = new OrderService(orderSearchService, orderSearchMapper, orderRepository, bidService,
                professionalService, userContext, PassThroughCategories.service());

        CustomerProfile customer = new CustomerProfile();
        customer.setId(CUSTOMER_ID);
        order = new Order();
        order.setId(ORDER_ID);
        order.setCustomer(customer);
        order.setStatus(OrderStatus.CREATED);
        order.setBids(new ArrayList<>());

        document = new OrderSearchDto();
        when(orderSearchMapper.toDto(order)).thenReturn(document);
        when(orderRepository.findById(ORDER_ID)).thenReturn(order);
        when(userContext.is(CUSTOMER_ID)).thenReturn(true);
    }

    private static ProfessionalProfile professional(final UUID id) {
        ProfessionalProfile professional = new ProfessionalProfile();
        professional.setId(id);
        return professional;
    }

    /** The bid as the resource builds it: the mapper leaves the professional and the order empty. */
    private static Bid bidFromTheRequest() {
        Bid bid = new Bid();
        bid.setPrice(500);
        bid.setDescription("I can do it");
        return bid;
    }

    private Bid bidOf(final UUID professionalId) {
        Bid bid = new Bid();
        bid.setProfessional(professional(professionalId));
        return bid;
    }

    // ---- create ----

    @Test
    void newOrderStartsAsCreatedIsSavedAndIndexed() {
        Order created = new Order();
        created.setCustomer(order.getCustomer());
        created.setStatus(OrderStatus.COMPLETED);
        when(orderSearchMapper.toDto(created)).thenReturn(document);

        Order result = service.create(created);

        assertSame(created, result);
        assertEquals(OrderStatus.CREATED, result.getStatus());
        verify(orderRepository).persist(created);
        verify(orderSearchService).save(document);
    }

    // ---- delete ----

    @Test
    void ownerDeletesTheOrderAndItLeavesTheSearchIndex() {
        service.delete(ORDER_ID);

        verify(orderRepository).delete(order);
        verify(orderSearchService).delete(ORDER_ID);
    }

    @Test
    void anotherCustomerCannotDeleteTheOrder() {
        when(userContext.is(CUSTOMER_ID)).thenReturn(false);

        assertThrows(ForbiddenException.class, () -> service.delete(ORDER_ID));

        verify(orderRepository, never()).delete(any(Order.class));
        verify(orderSearchService, never()).delete(any(UUID.class));
    }

    @Test
    void deletingAMissingOrderIsNotFound() {
        UUID unknownId = UUID.randomUUID();
        assertThrows(NotFoundException.class, () -> service.delete(unknownId));

        verify(orderRepository, never()).delete(any(Order.class));
    }

    // ---- read ----

    @Test
    void orderIsFoundById() {
        assertSame(order, service.getById(ORDER_ID));
    }

    @Test
    void missingOrderIsNotFound() {
        UUID unknownId = UUID.randomUUID();
        assertThrows(NotFoundException.class, () -> service.getById(unknownId));
    }

    @SuppressWarnings("unchecked")
    @Test
    void allOrdersAreReadByPageWhichStartsAtOne() {
        PanacheQuery<Order> query = mock(PanacheQuery.class);
        when(orderRepository.findAll()).thenReturn(query);
        when(query.page(PAGE - 1, SIZE)).thenReturn(query);
        when(query.list()).thenReturn(List.of(order));

        assertEquals(List.of(order), service.getAll(PAGE, SIZE));
    }

    @Test
    void customerReadsOwnOrders() {
        when(orderRepository.findByCustomerId(CUSTOMER_ID, PAGE, SIZE)).thenReturn(List.of(order));

        assertEquals(List.of(order), service.getByCustomerId(CUSTOMER_ID, PAGE, SIZE));
    }

    @Test
    void customerCannotReadOrdersOfAnotherCustomer() {
        UUID other = UUID.randomUUID();

        assertThrows(ForbiddenException.class, () -> service.getByCustomerId(other, PAGE, SIZE));

        verify(orderRepository, never()).findByCustomerId(other, PAGE, SIZE);
    }

    // ---- bids ----

    @Test
    void firstBidIsSavedLinkedToTheOrderAndMovesItToCustomerPending() {
        ProfessionalProfile professional = professional(PROFESSIONAL_ID);
        when(professionalService.getById(PROFESSIONAL_ID)).thenReturn(professional);
        Bid bid = bidFromTheRequest();

        Bid result = service.placeBid(ORDER_ID, PROFESSIONAL_ID, bid);

        assertSame(bid, result);
        assertSame(order, bid.getOrder());
        assertSame(professional, bid.getProfessional());
        assertTrue(order.getBids().contains(bid));
        assertEquals(OrderStatus.CUSTOMER_PENDING, order.getStatus());
        verify(bidService).create(bid);
        verify(orderRepository).persist(order);
        verify(orderSearchService).save(document);
    }

    @Test
    void secondProfessionalCanBidOnAnOrderThatIsAlreadyPending() {
        order.setStatus(OrderStatus.CUSTOMER_PENDING);
        order.getBids().add(bidOf(OTHER_PROFESSIONAL_ID));
        when(professionalService.getById(PROFESSIONAL_ID)).thenReturn(professional(PROFESSIONAL_ID));

        service.placeBid(ORDER_ID, PROFESSIONAL_ID, bidFromTheRequest());

        assertEquals(2, order.getBids().size());
        assertEquals(OrderStatus.CUSTOMER_PENDING, order.getStatus());
    }

    @Test
    void professionalCannotBidTwiceOnTheSameOrder() {
        order.getBids().add(bidOf(PROFESSIONAL_ID));
        when(professionalService.getById(PROFESSIONAL_ID)).thenReturn(professional(PROFESSIONAL_ID));

        Bid bid = bidFromTheRequest();
        ClientErrorException thrown = assertThrows(ClientErrorException.class,
                () -> service.placeBid(ORDER_ID, PROFESSIONAL_ID, bid));

        assertEquals(409, thrown.getResponse().getStatus());
        verify(bidService, never()).create(any(Bid.class));
        assertEquals(1, order.getBids().size());
    }

    @Test
    void completedOrderTakesNoBids() {
        order.setStatus(OrderStatus.COMPLETED);

        Bid bid = bidFromTheRequest();
        assertThrows(OrderClosedException.class, () -> service.placeBid(ORDER_ID, PROFESSIONAL_ID, bid));

        verify(bidService, never()).create(any(Bid.class));
        assertEquals(OrderStatus.COMPLETED, order.getStatus());
    }

    @Test
    void cancelledOrderTakesNoBids() {
        order.setStatus(OrderStatus.CANCELLED);

        Bid bid = bidFromTheRequest();
        assertThrows(OrderClosedException.class, () -> service.placeBid(ORDER_ID, PROFESSIONAL_ID, bid));

        verify(bidService, never()).create(any(Bid.class));
    }

    @Test
    void bidOnAMissingOrderIsNotFound() {
        UUID unknownId = UUID.randomUUID();
        Bid bid = bidFromTheRequest();
        assertThrows(NotFoundException.class, () -> service.placeBid(unknownId, PROFESSIONAL_ID, bid));

        verify(bidService, never()).create(any(Bid.class));
    }

    @Test
    void professionalWhoBidIsRecognised() {
        order.getBids().add(bidOf(PROFESSIONAL_ID));

        assertTrue(service.haveProfessionalResponse(order, bidOf(PROFESSIONAL_ID)));
    }

    @Test
    void professionalWhoDidNotBidIsNotRecognised() {
        order.getBids().add(bidOf(OTHER_PROFESSIONAL_ID));

        assertFalse(service.haveProfessionalResponse(order, bidOf(PROFESSIONAL_ID)));
    }
}
