package io.github.strogolsky.profinder.core.service;

import io.github.strogolsky.profinder.core.entity.Category;
import io.github.strogolsky.profinder.core.repository.CategoryRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.NotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Service class for managing {@link Category} entities.
 * <p>
 * Provides business logic for retrieving available categories.
 * </p>
 */
@ApplicationScoped
@Slf4j
@RequiredArgsConstructor
public class CategoryService {

    /**
     * Repository for accessing category data.
     */
    private final CategoryRepository categoryRepository;

    /**
     * Retrieves all categories from the database.
     *
     * @return list of all available categories
     */
    public List<Category> getAll() {
        log.info("get all categories");
        return categoryRepository.listAll();
    }

    /**
     * Turns the categories of a request into the stored ones. A request names a category only by its id, so the
     * object that comes with it is not a stored entity (it has no version, and the database would refuse it).
     * Every id is looked up, the order of the request is kept and a category named twice is taken once.
     *
     * @param requested the categories of the request, or {@code null} when none were sent
     * @return the stored categories, an empty list when none were sent
     * @throws BadRequestException if a category has no id
     * @throws NotFoundException   if a category does not exist
     */
    public List<Category> resolve(final List<Category> requested) {
        if (requested == null) {
            return List.of();
        }
        Set<UUID> ids = new LinkedHashSet<>();
        for (Category category : requested) {
            if (category == null || category.getId() == null) {
                throw new BadRequestException("Every category needs an id");
            }
            ids.add(category.getId());
        }
        if (ids.isEmpty()) {
            return List.of();
        }
        Map<UUID, Category> stored = new LinkedHashMap<>();
        for (Category category : categoryRepository.list("id in ?1", ids)) {
            stored.put(category.getId(), category);
        }
        for (UUID id : ids) {
            if (!stored.containsKey(id)) {
                log.warn("Category with ID={} not found", id);
                throw new NotFoundException("Category with id " + id + " not found");
            }
        }
        return ids.stream().map(stored::get).toList();
    }
}
