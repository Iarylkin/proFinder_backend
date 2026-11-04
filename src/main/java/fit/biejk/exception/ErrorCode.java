package fit.biejk.exception;

import jakarta.ws.rs.core.Response;
import lombok.Getter;

import java.util.Arrays;
import java.util.Optional;

/**
 * Error Catalog: the stable, machine-matchable codes the API returns, with their HTTP status and default message.
 * <p>
 * Only the codes that the code can return today are listed; the full target catalog is in
 * {@code docs/8 - API Specification.md}. A code is added together with the feature that needs it.
 * </p>
 * <p>
 * A <em>generic</em> code stands for a whole status: an exception that carries only a status (the built-in
 * {@code BadRequestException}, {@code NotFoundException}, ...) is answered with the generic code of its status.
 * The other codes are for cases the client must tell apart from the generic one.
 * </p>
 */
@Getter
public enum ErrorCode {

    /**
     * The request is malformed or a field is invalid.
     */
    VALIDATION_ERROR(Response.Status.BAD_REQUEST.getStatusCode(), "Some fields are invalid", true),

    /**
     * The password reset code is wrong, expired or already used.
     */
    RESET_TOKEN_INVALID(Response.Status.BAD_REQUEST.getStatusCode(),
            "The password reset code is invalid or expired", false),

    /**
     * Missing, invalid or expired JWT.
     */
    UNAUTHENTICATED(Response.Status.UNAUTHORIZED.getStatusCode(), "Please log in", true),

    /**
     * Wrong email, password or role at sign-in. One code for all of them, so the answer does not
     * reveal whether the email is registered.
     */
    INVALID_CREDENTIALS(Response.Status.UNAUTHORIZED.getStatusCode(), "Incorrect email or password", false),

    /**
     * A valid token, but not the right role or owner for the action.
     */
    FORBIDDEN(Response.Status.FORBIDDEN.getStatusCode(), "You don't have permission to do this", true),

    /**
     * The resource does not exist.
     */
    NOT_FOUND(Response.Status.NOT_FOUND.getStatusCode(), "Not found", true),

    /**
     * The state of the resource does not allow the action, or it was changed by someone else.
     */
    CONFLICT(Response.Status.CONFLICT.getStatusCode(), "This item was modified, please refresh", true),

    /**
     * An account with this email already exists.
     */
    EMAIL_ALREADY_REGISTERED(Response.Status.CONFLICT.getStatusCode(),
            "An account with this email already exists", false),

    /**
     * The order does not accept bids any more.
     */
    ORDER_CLOSED(Response.Status.CONFLICT.getStatusCode(), "This order is closed", false),

    /**
     * The uploaded file is over the size limit.
     */
    FILE_TOO_LARGE(Response.Status.REQUEST_ENTITY_TOO_LARGE.getStatusCode(), "File exceeds the size limit", false),

    /**
     * The uploaded file has a type that is not allowed.
     */
    INVALID_FILE_TYPE(Response.Status.UNSUPPORTED_MEDIA_TYPE.getStatusCode(),
            "This file type isn't allowed here", false),

    /**
     * A request limit was reached; the response carries {@code Retry-After}.
     * {@link Response.Status} has no constant for 429.
     */
    RATE_LIMITED(Http.TOO_MANY_REQUESTS, "Too many requests, try again later", true),

    /**
     * An unhandled server error.
     */
    INTERNAL_ERROR(Response.Status.INTERNAL_SERVER_ERROR.getStatusCode(),
            "Something went wrong, please try again", true);

    /**
     * HTTP statuses that {@link Response.Status} does not define.
     */
    private static final class Http {

        /**
         * 429 Too Many Requests.
         */
        private static final int TOO_MANY_REQUESTS = 429;

        private Http() {
        }
    }

    /**
     * HTTP status of the response.
     */
    private final int status;

    /**
     * Message shown when the exception has no message of its own.
     */
    private final String message;

    /**
     * Whether the code stands for the whole status (see the class description).
     */
    private final boolean generic;

    ErrorCode(final int status, final String message, final boolean generic) {
        this.status = status;
        this.message = message;
        this.generic = generic;
    }

    /**
     * Finds the generic code of a status.
     *
     * @param status HTTP status of the exception
     * @return the code; any 5xx status gives {@link #INTERNAL_ERROR}; empty for a status that is not in the catalog
     */
    public static Optional<ErrorCode> forStatus(final int status) {
        if (status >= INTERNAL_ERROR.status) {
            return Optional.of(INTERNAL_ERROR);
        }
        return Arrays.stream(values())
                .filter(code -> code.generic && code.status == status)
                .findFirst();
    }
}
