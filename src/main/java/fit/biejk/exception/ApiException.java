package fit.biejk.exception;

import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Response;

/**
 * An API error that carries its own {@link ErrorCode}. The HTTP status comes from the code.
 * <p>
 * Used where the client must tell the error apart from the generic one of the same status. Where the status
 * alone is enough, the built-in {@code BadRequestException}, {@code ForbiddenException},
 * {@code NotFoundException} are thrown instead.
 * </p>
 */
public abstract class ApiException extends WebApplicationException {

    private static final long serialVersionUID = 1L;

    /**
     * The code of the error.
     */
    private final ErrorCode code;

    /**
     * Creates an error with the status of the code.
     *
     * @param code    the error code
     * @param message the message safe to show to the user
     */
    protected ApiException(final ErrorCode code, final String message) {
        super(message, code.getStatus());
        this.code = code;
    }

    /**
     * Creates an error with a prepared response (for example, one that carries extra headers).
     *
     * @param code     the error code
     * @param message  the message safe to show to the user
     * @param response the response; its status must be the status of the code
     */
    protected ApiException(final ErrorCode code, final String message, final Response response) {
        super(message, response);
        this.code = code;
    }

    /**
     * Returns the code of the error.
     *
     * @return the error code
     */
    public ErrorCode getCode() {
        return code;
    }
}
