package io.github.strogolsky.profinder.core.service;

import io.github.strogolsky.profinder.auth.service.UserService;
import io.github.strogolsky.profinder.common.security.UserContext;
import io.github.strogolsky.profinder.core.entity.Category;
import io.github.strogolsky.profinder.core.entity.CustomerProfile;
import io.github.strogolsky.profinder.core.entity.Order;
import io.github.strogolsky.profinder.core.entity.OrderStatus;
import io.github.strogolsky.profinder.core.entity.ProfessionalProfile;
import io.github.strogolsky.profinder.core.repository.OrderRepository;
import io.github.strogolsky.profinder.core.repository.ProfessionalRepository;
import io.github.strogolsky.profinder.core.search.OrderSearchMapper;
import io.github.strogolsky.profinder.core.search.OrderSearchService;
import io.github.strogolsky.profinder.core.search.ProfessionalSearchMapper;
import io.github.strogolsky.profinder.core.search.ProfessionalSearchService;
import jakarta.ws.rs.NotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * The categories of an order and of a professional come from a request as stubs with an id; the services must
 * store the categories that {@link CategoryService#resolve} returns, never the stubs, or the save fails with a 500.
 */
class ServicesResolveCategoriesTest {

    private static final UUID ORDER_ID = UUID.randomUUID();
    private static final UUID CUSTOMER_ID = UUID.randomUUID();
    private static final UUID PROFESSIONAL_ID = UUID.randomUUID();
    private static final long VERSION = 3L;

    private CategoryService categoryService;
    private OrderRepository orderRepository;
    private ProfessionalRepository professionalRepository;
    private OrderService orderService;
    private ProfessionalService professionalService;

    private Category stub;
    private Category stored;
    private Order order;
    private ProfessionalProfile professional;

    @BeforeEach
    void setUp() {
        categoryService = mock(CategoryService.class);
        orderRepository = mock(OrderRepository.class);
        professionalRepository = mock(ProfessionalRepository.class);
        UserContext userContext = mock(UserContext.class);
        when(userContext.is(CUSTOMER_ID)).thenReturn(true);

        orderService = new OrderService(mock(OrderSearchService.class), mock(OrderSearchMapper.class),
                orderRepository, mock(BidService.class), mock(ProfessionalService.class), userContext,
                categoryService);
        professionalService = new ProfessionalService(professionalRepository, mock(UserService.class),
                mock(ProfessionalSearchService.class), mock(ProfessionalSearchMapper.class),
                mock(ReviewService.class), userContext, mock(CustomerService.class), mock(ProfileService.class),
                categoryService);

        stub = new Category();
        stub.setId(UUID.randomUUID());
        stored = new Category();
        stored.setId(stub.getId());
        stored.setVersion(0L);
        when(categoryService.resolve(List.of(stub))).thenReturn(List.of(stored));

        CustomerProfile customer = new CustomerProfile();
        customer.setId(CUSTOMER_ID);
        order = new Order();
        order.setId(ORDER_ID);
        order.setCustomer(customer);
        order.setStatus(OrderStatus.CREATED);
        order.setVersion(VERSION);
        when(orderRepository.findById(ORDER_ID)).thenReturn(order);

        professional = new ProfessionalProfile();
        professional.setId(PROFESSIONAL_ID);
        professional.setVersion(VERSION);
        professional.setCategories(List.of());
        when(professionalRepository.findById(PROFESSIONAL_ID)).thenReturn(professional);
    }

    @Test
    void newOrderIsSavedWithTheStoredCategories() {
        Order created = new Order();
        created.setCustomer(order.getCustomer());
        created.setCategories(List.of(stub));

        orderService.create(created);

        assertEquals(List.of(stored), created.getCategories());
        verify(orderRepository).persist(created);
    }

    @Test
    void newOrderWithoutCategoriesKeepsNone() {
        when(categoryService.resolve(null)).thenReturn(null);
        Order created = new Order();
        created.setCustomer(order.getCustomer());

        orderService.create(created);

        assertNull(created.getCategories());
    }

    @Test
    void unknownCategoryStopsTheCreationOfAnOrder() {
        Category unknown = new Category();
        unknown.setId(UUID.randomUUID());
        when(categoryService.resolve(List.of(unknown))).thenThrow(new NotFoundException("no category"));
        Order created = new Order();
        created.setCustomer(order.getCustomer());
        created.setCategories(List.of(unknown));

        assertThrows(NotFoundException.class, () -> orderService.create(created));

        verify(orderRepository, never()).persist(created);
    }

    @Test
    void updatedOrderGetsTheStoredCategories() {
        Order changes = new Order();
        changes.setVersion(VERSION);
        changes.setCategories(List.of(stub));

        Order result = orderService.update(ORDER_ID, changes);

        assertSame(order, result);
        assertEquals(List.of(stored), order.getCategories());
    }

    @Test
    void updatedProfessionalGetsTheStoredCategories() {
        ProfessionalProfile changes = new ProfessionalProfile();
        changes.setVersion(VERSION);
        changes.setCategories(List.of(stub));

        professionalService.updateProfile(PROFESSIONAL_ID, changes);

        assertEquals(List.of(stored), professional.getCategories());
    }
}
