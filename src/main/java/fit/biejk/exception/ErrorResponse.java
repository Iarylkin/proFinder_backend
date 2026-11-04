package fit.biejk.exception;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Map;

/**
 * Body of every error response: {@code {"error": {"code", "message", "details"}}}.
 */
@Getter
@AllArgsConstructor
public final class ErrorResponse {

    /**
     * The error itself.
     */
    private final Body error;

    /**
     * Builds the body of an error response.
     *
     * @param code    the error code
     * @param message the message safe to show to the user
     * @param details field to reason, for a validation error; {@code null} otherwise
     * @return the response body
     */
    public static ErrorResponse of(final ErrorCode code, final String message, final Map<String, String> details) {
        return new ErrorResponse(new Body(code.name(), message, details));
    }

    /**
     * The error: a stable code, a message and the optional details.
     */
    @Getter
    @AllArgsConstructor
    public static final class Body {

        /**
         * Stable, machine-matchable code from the {@link ErrorCode} catalog.
         */
        private final String code;

        /**
         * Human-readable message, safe to show as it is.
         */
        private final String message;

        /**
         * Field to reason for a validation error; {@code null} for the other errors.
         */
        private final Map<String, String> details;
    }
}
