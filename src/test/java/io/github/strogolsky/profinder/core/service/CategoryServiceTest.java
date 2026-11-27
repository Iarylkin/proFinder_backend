package io.github.strogolsky.profinder.core.service;

import io.github.strogolsky.profinder.core.entity.Category;
import io.github.strogolsky.profinder.core.repository.CategoryRepository;
import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.NotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * A request names a category only by its id; the service turns those stubs into the stored categories, because
 * the stub has no version and the database refuses it.
 */
class CategoryServiceTest {

    private CategoryRepository repository;
    private CategoryService service;

    private Category plumbing;
    private Category cleaning;

    @BeforeEach
    void setUp() {
        repository = mock(CategoryRepository.class);
        service = new CategoryService(repository);
        plumbing = stored();
        cleaning = stored();
    }

    private static Category stored() {
        Category category = new Category();
        category.setId(UUID.randomUUID());
        category.setVersion(0L);
        return category;
    }

    private static Category stub(final UUID id) {
        Category category = new Category();
        category.setId(id);
        return category;
    }

    private void storeBoth() {
        when(repository.list(org.mockito.ArgumentMatchers.eq("id in ?1"), org.mockito.ArgumentMatchers.<Object>any()))
                .thenReturn(List.of(cleaning, plumbing));
    }

    @Test
    void allCategoriesAreReadFromTheRepository() {
        when(repository.listAll()).thenReturn(List.of(plumbing, cleaning));

        assertEquals(List.of(plumbing, cleaning), service.getAll());
    }

    @Test
    void noCategoriesInTheRequestStayNone() {
        assertNull(service.resolve(null));

        verify(repository, never()).list(any(String.class), any(Object[].class));
    }

    @Test
    void emptyListStaysEmptyWithoutAQuery() {
        assertEquals(List.of(), service.resolve(new ArrayList<>()));

        verify(repository, never()).list(any(String.class), any(Object[].class));
    }

    @Test
    void stubsAreReplacedByTheStoredCategoriesInTheOrderOfTheRequest() {
        storeBoth();

        List<Category> result = service.resolve(List.of(stub(plumbing.getId()), stub(cleaning.getId())));

        assertEquals(2, result.size());
        assertSame(plumbing, result.get(0));
        assertSame(cleaning, result.get(1));
    }

    @Test
    void aCategoryNamedTwiceIsTakenOnce() {
        storeBoth();

        List<Category> result = service.resolve(
                List.of(stub(plumbing.getId()), stub(plumbing.getId()), stub(cleaning.getId())));

        assertEquals(List.of(plumbing, cleaning), result);
    }

    @Test
    void anUnknownCategoryIsNotFound() {
        storeBoth();

        assertThrows(NotFoundException.class,
                () -> service.resolve(List.of(stub(plumbing.getId()), stub(UUID.randomUUID()))));
    }

    @Test
    void aCategoryWithoutAnIdIsABadRequest() {
        assertThrows(BadRequestException.class, () -> service.resolve(List.of(new Category())));
    }

    @Test
    void aMissingCategoryEntryIsABadRequest() {
        List<Category> withNull = new ArrayList<>();
        withNull.add(null);

        assertThrows(BadRequestException.class, () -> service.resolve(withNull));
    }
}
