package io.github.strogolsky.profinder.core.dto;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * A search by filters runs in Elasticsearch; with no filter at all the plain list of the database is returned.
 * A blank text is not a filter.
 */
class FilterRequestsTest {

    @Test
    void emptyOrderRequestHasNoFilters() {
        assertFalse(new OrderFilterRequest().hasFilters());
    }

    @Test
    void orderRequestWithCategoriesIsFiltered() {
        OrderFilterRequest request = new OrderFilterRequest();
        request.setCategories(List.of("Plumbing"));

        assertTrue(request.hasFilters());
    }

    @Test
    void orderRequestWithAnEmptyListOfCategoriesIsNotFiltered() {
        OrderFilterRequest request = new OrderFilterRequest();
        request.setCategories(List.of());

        assertFalse(request.hasFilters());
    }

    @Test
    void orderRequestWithALocationOrAQueryIsFiltered() {
        OrderFilterRequest byLocation = new OrderFilterRequest();
        byLocation.setLocation("Prague");
        OrderFilterRequest byQuery = new OrderFilterRequest();
        byQuery.setQuery("sink");

        assertTrue(byLocation.hasFilters());
        assertTrue(byQuery.hasFilters());
    }

    @Test
    void orderRequestWithBlankTextsIsNotFiltered() {
        OrderFilterRequest request = new OrderFilterRequest();
        request.setLocation("  ");
        request.setQuery("");

        assertFalse(request.hasFilters());
    }

    @Test
    void emptyProfessionalRequestHasNoFilters() {
        assertFalse(new ProfessionalFilterRequest().hasFilters());
    }

    @Test
    void professionalRequestWithAQueryOrALocationIsFiltered() {
        ProfessionalFilterRequest byQuery = new ProfessionalFilterRequest();
        byQuery.setQuery("pipes");
        ProfessionalFilterRequest byLocation = new ProfessionalFilterRequest();
        byLocation.setLocation("Brno");

        assertTrue(byQuery.hasFilters());
        assertTrue(byLocation.hasFilters());
    }

    @Test
    void professionalRequestWithBlankTextsIsNotFiltered() {
        ProfessionalFilterRequest request = new ProfessionalFilterRequest();
        request.setQuery(" ");
        request.setLocation("");

        assertFalse(request.hasFilters());
    }
}
