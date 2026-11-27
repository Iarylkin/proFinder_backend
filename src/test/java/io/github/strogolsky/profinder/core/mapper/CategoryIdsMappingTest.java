package io.github.strogolsky.profinder.core.mapper;

import io.github.strogolsky.profinder.core.dto.OrderCreateRequest;
import io.github.strogolsky.profinder.core.dto.OrderUpdateRequest;
import io.github.strogolsky.profinder.core.dto.ProfessionalUpdateRequest;
import io.github.strogolsky.profinder.core.entity.Category;
import io.github.strogolsky.profinder.core.entity.Order;
import io.github.strogolsky.profinder.core.entity.ProfessionalProfile;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * A request names the categories by id ({@code categoryIds}); the mappers turn the ids into references to the
 * categories, which the service later replaces with the stored ones.
 */
class CategoryIdsMappingTest {

    private static final UUID FIRST = UUID.randomUUID();
    private static final UUID SECOND = UUID.randomUUID();
    private static final long VERSION = 2L;

    private static ValidatorFactory factory;
    private static Validator validator;

    private final OrderMapper orderMapper = new OrderMapperImpl();
    private final ProfessionalMapper professionalMapper = new ProfessionalMapperImpl();

    @BeforeAll
    static void createValidator() {
        factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }

    @AfterAll
    static void closeValidator() {
        factory.close();
    }

    private static List<UUID> idsOf(final List<Category> categories) {
        return categories.stream().map(Category::getId).toList();
    }

    @Test
    void newOrderGetsAReferenceForEveryId() {
        OrderCreateRequest request = new OrderCreateRequest(List.of(FIRST, SECOND), "Fix my sink",
                "The kitchen sink is leaking badly", 100, null);

        Order order = orderMapper.toEntity(request);

        assertEquals(List.of(FIRST, SECOND), idsOf(order.getCategories()));
    }

    @Test
    void newOrderWithoutIdsHasNoCategories() {
        OrderCreateRequest request = new OrderCreateRequest(null, "Fix my sink",
                "The kitchen sink is leaking badly", 100, null);

        assertNull(orderMapper.toEntity(request).getCategories());
    }

    @Test
    void orderChangesGetAReferenceForEveryId() {
        OrderUpdateRequest request = new OrderUpdateRequest(List.of(SECOND), null, null, null, null, null, null,
                VERSION);

        Order changes = orderMapper.toEntity(request);

        assertEquals(List.of(SECOND), idsOf(changes.getCategories()));
    }

    @Test
    void orderChangesWithoutIdsKeepTheCategories() {
        OrderUpdateRequest request = new OrderUpdateRequest(null, null, null, null, null, null, null, VERSION);

        assertNull(orderMapper.toEntity(request).getCategories());
    }

    @Test
    void professionalChangesGetAReferenceForEveryId() {
        ProfessionalUpdateRequest request = new ProfessionalUpdateRequest(null, null, null, null, null,
                List.of(FIRST, SECOND), VERSION);

        ProfessionalProfile changes = professionalMapper.toEntity(request);

        assertEquals(List.of(FIRST, SECOND), idsOf(changes.getCategories()));
    }

    @Test
    void professionalChangesWithoutIdsKeepTheCategories() {
        ProfessionalUpdateRequest request = new ProfessionalUpdateRequest(null, null, null, null, null, null,
                VERSION);

        assertNull(professionalMapper.toEntity(request).getCategories());
    }

    @Test
    void anIdThatIsNullIsRejectedByTheValidation() {
        List<UUID> withNull = new ArrayList<>();
        withNull.add(null);
        ProfessionalUpdateRequest request = new ProfessionalUpdateRequest(null, null, null, null, null, withNull,
                VERSION);

        Set<ConstraintViolation<ProfessionalUpdateRequest>> violations = validator.validate(request);

        assertFalse(violations.isEmpty());
        assertTrue(violations.stream().anyMatch(v -> v.getPropertyPath().toString().startsWith("categoryIds")));
    }

    @Test
    void validIdsPassTheValidation() {
        ProfessionalUpdateRequest request = new ProfessionalUpdateRequest(null, null, null, null, null,
                List.of(FIRST), VERSION);

        assertTrue(validator.validate(request).isEmpty());
    }
}
