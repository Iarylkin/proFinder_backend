package io.github.strogolsky.profinder.core.search;

import io.github.strogolsky.profinder.core.entity.Category;
import io.github.strogolsky.profinder.core.entity.Location;
import io.github.strogolsky.profinder.core.entity.Order;
import io.github.strogolsky.profinder.core.entity.OrderStatus;
import io.github.strogolsky.profinder.core.entity.ProfessionalProfile;
import io.github.strogolsky.profinder.core.service.OrderService;
import io.github.strogolsky.profinder.core.service.ProfessionalService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * What goes into the search index: the document of a professional and of an order hold the names of the
 * categories and of the location, not the entities; the way back reads the stored entity by the id.
 */
class SearchMappersTest {

    private static final UUID PROFESSIONAL_ID = UUID.randomUUID();
    private static final UUID ORDER_ID = UUID.randomUUID();

    private ProfessionalService professionalService;
    private OrderService orderService;
    private ProfessionalSearchMapper professionalMapper;
    private OrderSearchMapper orderMapper;

    @BeforeEach
    void setUp() {
        professionalService = mock(ProfessionalService.class);
        orderService = mock(OrderService.class);
        professionalMapper = new ProfessionalSearchMapper(professionalService);
        orderMapper = new OrderSearchMapper(orderService);
    }

    private static Category category(final String name) {
        Category category = new Category();
        category.setName(name);
        return category;
    }

    private static Location location(final String name) {
        Location location = new Location();
        location.setName(name);
        return location;
    }

    private static ProfessionalProfile professional() {
        ProfessionalProfile professional = new ProfessionalProfile();
        professional.setId(PROFESSIONAL_ID);
        professional.setFirstName("Jan");
        professional.setLastName("Novak");
        professional.setDescription("I fix pipes");
        professional.setRating(4.5);
        professional.setCategories(List.of(category("Plumbing"), category("Heating")));
        professional.setLocation(location("Prague"));
        return professional;
    }

    private static Order order() {
        Order order = new Order();
        order.setId(ORDER_ID);
        order.setStatus(OrderStatus.CUSTOMER_PENDING);
        order.setCategories(List.of(category("Plumbing")));
        order.setLocation(location("Brno"));
        order.setTitle("Fix my sink");
        order.setDescription("It leaks");
        return order;
    }

    @Test
    void professionalDocumentHoldsTheNamesNotTheEntities() {
        ProfessionalSearchDto dto = professionalMapper.toDto(professional());

        assertEquals(PROFESSIONAL_ID, dto.getId());
        assertEquals("Jan", dto.getFirstName());
        assertEquals("Novak", dto.getLastName());
        assertEquals("I fix pipes", dto.getDescription());
        assertEquals(4.5, dto.getRating());
        assertEquals(List.of("Plumbing", "Heating"), dto.getCategories());
        assertEquals("Prague", dto.getLocation());
    }

    @Test
    void professionalWithoutCategoriesHasAnEmptyListInTheDocument() {
        ProfessionalProfile professional = professional();
        professional.setCategories(null);

        assertEquals(List.of(), professionalMapper.toDto(professional).getCategories());
    }

    @Test
    void professionalIsReadBackByTheIdOfTheDocument() {
        ProfessionalProfile professional = professional();
        when(professionalService.getById(PROFESSIONAL_ID)).thenReturn(professional);
        ProfessionalSearchDto dto = professionalMapper.toDto(professional);

        assertSame(professional, professionalMapper.toEntity(dto));
        assertEquals(List.of(professional), professionalMapper.toEntityList(List.of(dto)));
    }

    @Test
    void orderDocumentHoldsTheStatusTheNamesAndTheTexts() {
        OrderSearchDto dto = orderMapper.toDto(order());

        assertEquals(ORDER_ID, dto.getId());
        assertEquals("CUSTOMER_PENDING", dto.getStatus());
        assertEquals(List.of("Plumbing"), dto.getCategories());
        assertEquals("Brno", dto.getLocation());
        assertEquals("Fix my sink", dto.getTitle());
        assertEquals("It leaks", dto.getDescription());
    }

    @Test
    void orderWithoutCategoriesAndLocationHasAnEmptyListAndNoLocation() {
        Order order = order();
        order.setCategories(null);
        order.setLocation(null);

        OrderSearchDto dto = orderMapper.toDto(order);

        assertEquals(List.of(), dto.getCategories());
        assertNull(dto.getLocation());
    }

    @Test
    void ordersAreConvertedToDocumentsAsAList() {
        assertEquals(2, orderMapper.toDtoList(List.of(order(), order())).size());
    }

    @Test
    void orderIsReadBackByTheIdOfTheDocument() {
        Order order = order();
        when(orderService.getById(ORDER_ID)).thenReturn(order);
        OrderSearchDto dto = orderMapper.toDto(order);

        assertSame(order, orderMapper.toEntity(dto));
        assertEquals(List.of(order), orderMapper.toEntityList(List.of(dto)));
    }
}
