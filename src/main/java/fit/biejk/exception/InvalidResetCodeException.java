package fit.biejk.exception;

import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Response;

/**
 * Thrown when a password reset code is wrong, missing or expired (HTTP 400, {@code RESET_TOKEN_INVALID}).
 */
public class InvalidResetCodeException extends WebApplicationException {

    /** Serialization version. */
    private static final long serialVersionUID = 1L;

    /**
     * Creates the exception with the {@code RESET_TOKEN_INVALID} message and status 400.
     */
    public InvalidResetCodeException() {
        super("The password reset code is invalid or expired", Response.Status.BAD_REQUEST);
    }
}
