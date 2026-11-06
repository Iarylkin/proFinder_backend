package io.github.strogolsky.profinder.core.repository;

import io.github.strogolsky.profinder.core.entity.Category;
import io.quarkus.hibernate.orm.panache.PanacheRepositoryBase;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.UUID;

/**
 * Repository for managing {@link Category} entities.
 * <p>
 * Provides CRUD operations using Panache.
 * </p>
 */
@ApplicationScoped
public class CategoryRepository implements PanacheRepositoryBase<Category, UUID> {
}
