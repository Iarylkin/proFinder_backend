package io.github.strogolsky.profinder.auth.exception;

import io.github.strogolsky.profinder.common.exception.ApiException;
import io.github.strogolsky.profinder.common.exception.ErrorCode;

/**
 * Thrown when a client exceeds an attempt limit (HTTP 429, {@code RATE_LIMITED}) and must wait before retrying.
 */
public class TooManyAttemptsException extends ApiException {

    /** Serialization version. */
    private static final long serialVersionUID = 1L;

    /**
     * Creates the exception with status 429 and a {@code Retry-After} header.
     *
     * @param retryAfterSeconds number of seconds after which the client may try again
     */
    public TooManyAttemptsException(final long retryAfterSeconds) {
        super(ErrorCode.RATE_LIMITED, problem(ErrorCode.RATE_LIMITED, "Too many attempts, try again later")
                .withHeader("Retry-After", retryAfterSeconds));
    }
}
