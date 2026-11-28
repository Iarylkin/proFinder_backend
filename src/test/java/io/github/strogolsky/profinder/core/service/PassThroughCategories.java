package io.github.strogolsky.profinder.core.service;

import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * A {@link CategoryService} for the tests of the services that only pass the categories of a request on:
 * it returns the categories it is given, as if every one of them were stored.
 */
final class PassThroughCategories {

    private PassThroughCategories() {
    }

    static CategoryService service() {
        CategoryService categories = mock(CategoryService.class);
        when(categories.resolve(anyList())).thenAnswer(call -> call.getArgument(0));
        return categories;
    }
}
