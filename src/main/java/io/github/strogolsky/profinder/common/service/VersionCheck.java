package io.github.strogolsky.profinder.common.service;

import io.github.strogolsky.profinder.common.entity.VersionedEntity;
import io.github.strogolsky.profinder.common.exception.StaleVersionException;

import java.util.Objects;

/**
 * Compares the version that the client has seen with the current version of an entity.
 * <p>
 * The check in {@code @Version} of Hibernate protects only against two transactions that overlap on the server.
 * The version sent by the client also protects against a change made after the client read the resource.
 * </p>
 */
public final class VersionCheck {

    private VersionCheck() {
    }

    /**
     * Fails if the version sent by the client is not the current version of the entity. Nothing must be changed
     * before this check.
     *
     * @param entity  the entity loaded for the update
     * @param version the version the client sent
     * @throws StaleVersionException if the entity has another version
     */
    public static void require(final VersionedEntity entity, final Long version) {
        if (!Objects.equals(entity.getVersion(), version)) {
            throw new StaleVersionException();
        }
    }
}
