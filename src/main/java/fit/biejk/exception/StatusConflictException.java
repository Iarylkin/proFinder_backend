package fit.biejk.exception;

import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Response;

/**
 * Thrown when a requested status change is not allowed for the current state of a resource (HTTP 409).
 */
public class StatusConflictException extends WebApplicationException {

    private static final long serialVersionUID = 1L;

    /**
     * Creates the exception.
     *
     * @param message explanation of why the status change is rejected
     */
    public StatusConflictException(final String message) {
        super(message, Response.Status.CONFLICT);
    }
}
