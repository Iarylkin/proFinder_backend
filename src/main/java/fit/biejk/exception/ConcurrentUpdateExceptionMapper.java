package fit.biejk.exception;

import jakarta.persistence.OptimisticLockException;
import jakarta.transaction.RollbackException;
import jakarta.ws.rs.core.Response;
import lombok.extern.slf4j.Slf4j;
import org.jboss.resteasy.reactive.server.ServerExceptionMapper;

/**
 * Turns a failed optimistic lock into HTTP 409.
 * <p>
 * Hibernate raises an {@link OptimisticLockException} when a row was changed by someone else since it was read.
 * A transactional method reports it either directly or as the cause of a {@link RollbackException}, so the
 * cause chain is searched. Any other rollback is logged and answered with 500, as it would be without this mapper.
 * </p>
 */
@Slf4j
public class ConcurrentUpdateExceptionMapper {

    /**
     * Message returned to the client.
     */
    static final String MESSAGE = "The resource was changed by someone else, reload it and try again";

    /**
     * Maps the exception to 409 when an optimistic lock failure caused it, and to 500 otherwise.
     *
     * @param exception the exception thrown by the persistence layer
     * @return 409 for an optimistic lock failure, 500 for any other cause
     */
    @ServerExceptionMapper({OptimisticLockException.class, RollbackException.class})
    public Response map(final Exception exception) {
        for (Throwable cause = exception; cause != null; cause = cause.getCause()) {
            if (cause instanceof OptimisticLockException) {
                return Response.status(Response.Status.CONFLICT).entity(MESSAGE).build();
            }
            if (cause.getCause() == cause) {
                break;
            }
        }
        log.error("Transaction failed", exception);
        return Response.serverError().build();
    }
}
