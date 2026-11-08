package io.github.strogolsky.profinder.common.exception;

import jakarta.persistence.OptimisticLockException;
import jakarta.ws.rs.core.Response;
import org.jboss.resteasy.reactive.server.ServerExceptionMapper;

/**
 * The only mapping the problem library does not know: a failed optimistic lock becomes {@code CONFLICT}. Every other
 * error is turned into {@code application/problem+json} (RFC 9457) by the library, see {@link ErrorCodeProcessor}.
 * <p>
 * Hibernate raises an {@link OptimisticLockException} when a row was changed by someone else since it was read.
 * </p>
 */
public final class ApiExceptionMapper {

    /**
     * Maps a failed optimistic lock.
     *
     * @param exception the exception of the persistence layer
     * @return {@code 409 CONFLICT}
     */
    @ServerExceptionMapper
    public Response map(final OptimisticLockException exception) {
        return new StaleVersionException().toResponse();
    }
}
