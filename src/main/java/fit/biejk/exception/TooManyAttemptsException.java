package fit.biejk.exception;

import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Response;

/**
 * Thrown when a client exceeds an attempt limit (HTTP 429, {@code RATE_LIMITED}) and must wait before retrying.
 */
public class TooManyAttemptsException extends WebApplicationException {

    /** Serialization version. */
    private static final long serialVersionUID = 1L;

    /** HTTP status code 429, which {@link Response.Status} does not define. */
    private static final int TOO_MANY_REQUESTS = 429;

    /**
     * Creates the exception with status 429 and a {@code Retry-After} header.
     *
     * @param retryAfterSeconds number of seconds after which the client may try again
     */
    public TooManyAttemptsException(final long retryAfterSeconds) {
        super("Too many attempts, try again later",
                Response.status(TOO_MANY_REQUESTS).header("Retry-After", retryAfterSeconds).build());
    }
}
