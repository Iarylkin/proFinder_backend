package io.github.strogolsky.profinder.core.mapper;

import io.github.strogolsky.profinder.core.entity.Category;

import java.util.UUID;

/**
 * Turns the id of a category in a request into a reference to the category: a {@link Category} that holds only the
 * id. It is not a stored entity (it has no version): the service replaces it with the stored category through
 * {@code CategoryService.resolve} before anything is saved. The mappers use it for the ids of a request.
 */
public final class CategoryReferences {

    private CategoryReferences() {
    }

    /**
     * Creates the reference to a category.
     *
     * @param id the id of the category
     * @return a category that holds only the id
     */
    public static Category toReference(final UUID id) {
        Category category = new Category();
        category.setId(id);
        return category;
    }
}
