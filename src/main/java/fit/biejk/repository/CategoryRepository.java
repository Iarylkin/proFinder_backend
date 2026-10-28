package fit.biejk.repository;

import fit.biejk.entity.Category;
import io.quarkus.hibernate.orm.panache.PanacheRepository;
import jakarta.enterprise.context.ApplicationScoped;

/**
 * Repository for managing {@link Category} entities.
 * <p>
 * Provides CRUD operations using Panache.
 * </p>
 */
@ApplicationScoped
public class CategoryRepository implements PanacheRepository<Category> {
}
