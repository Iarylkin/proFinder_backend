package fit.biejk.service;

import fit.biejk.entity.Category;
import fit.biejk.repository.CategoryRepository;
import jakarta.enterprise.context.ApplicationScoped;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.List;

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
}
