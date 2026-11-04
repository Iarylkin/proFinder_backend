package fit.biejk.exception;

import jakarta.json.JsonException;
import jakarta.json.bind.JsonbException;
import jakarta.persistence.OptimisticLockException;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Path;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import lombok.extern.slf4j.Slf4j;
import org.jboss.resteasy.reactive.server.ServerExceptionMapper;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Turns every error into the one response format {@link ErrorResponse} with a code from {@link ErrorCode}.
 * <ul>
 *   <li>{@link ApiException} keeps its own code;</li>
 *   <li>the other {@link WebApplicationException}s (the built-in {@code BadRequestException},
 *   {@code NotFoundException}, ...) get the generic code of their status and keep their headers;
 *   a status that is not in the catalog is passed through unchanged;</li>
 *   <li>failed bean validation becomes {@code VALIDATION_ERROR} with a field to reason map;</li>
 *   <li>a failed optimistic lock becomes {@code CONFLICT};</li>
 *   <li>a body that is not valid JSON, or has a value of the wrong type or format (for example a date that is not
 *   ISO 8601), becomes {@code VALIDATION_ERROR};</li>
 *   <li>anything else is logged and answered with {@code INTERNAL_ERROR}; its message never reaches the client.</li>
 * </ul>
 */
@Slf4j
public class ApiExceptionMapper {

    /**
     * Prefix of the default message that {@link WebApplicationException} makes up from the status.
     */
    private static final String DEFAULT_MESSAGE_PREFIX = "HTTP ";

    /**
     * Message for a body that cannot be read. The text of the parser is not shown: it names internal classes.
     */
    private static final String MALFORMED_BODY_MESSAGE = "The request body is malformed";

    /**
     * Maps an exception that carries its own HTTP status.
     *
     * @param exception the exception thrown by a resource or a service
     * @return the response in the common error format
     */
    @ServerExceptionMapper
    public Response map(final WebApplicationException exception) {
        Response original = exception.getResponse();
        ErrorCode code = exception instanceof ApiException api
                ? api.getCode()
                : ErrorCode.forStatus(original.getStatus()).orElse(null);
        if (code == null) {
            return original;
        }
        if (original.getStatus() >= ErrorCode.INTERNAL_ERROR.getStatus()) {
            log.error("Server error", exception);
            return build(Response.fromResponse(original), code, code.getMessage(), null);
        }
        return build(Response.fromResponse(original), code, messageOf(exception, code), null);
    }

    /**
     * Maps a failed bean validation to {@code VALIDATION_ERROR} with the invalid fields in {@code details}.
     *
     * @param exception the violations
     * @return the response in the common error format
     */
    @ServerExceptionMapper
    public Response map(final ConstraintViolationException exception) {
        Map<String, String> details = new LinkedHashMap<>();
        for (ConstraintViolation<?> violation : exception.getConstraintViolations()) {
            details.merge(fieldOf(violation.getPropertyPath()), violation.getMessage(),
                    (first, second) -> first + "; " + second);
        }
        return build(Response.status(ErrorCode.VALIDATION_ERROR.getStatus()), ErrorCode.VALIDATION_ERROR,
                ErrorCode.VALIDATION_ERROR.getMessage(), details);
    }

    /**
     * Maps any other exception: {@code CONFLICT} when an optimistic lock failed, {@code INTERNAL_ERROR} otherwise.
     * <p>
     * Hibernate raises an {@link OptimisticLockException} when a row was changed by someone else since it was read.
     * A transactional method reports it either directly or as the cause of a {@code RollbackException}, so the
     * cause chain is searched.
     * </p>
     *
     * @param exception the exception thrown by the persistence layer or by the code
     * @return {@code 409} for an optimistic lock failure, {@code 500} for anything else
     */
    @ServerExceptionMapper
    public Response map(final Throwable exception) {
        for (Throwable cause = exception; cause != null; cause = cause.getCause()) {
            if (cause instanceof OptimisticLockException) {
                return build(Response.status(ErrorCode.CONFLICT.getStatus()), ErrorCode.CONFLICT,
                        ErrorCode.CONFLICT.getMessage(), null);
            }
            if (cause.getCause() == cause) {
                break;
            }
        }
        if (isMalformedBody(exception)) {
            return build(Response.status(ErrorCode.VALIDATION_ERROR.getStatus()), ErrorCode.VALIDATION_ERROR,
                    MALFORMED_BODY_MESSAGE, null);
        }
        log.error("Unhandled error", exception);
        return build(Response.status(ErrorCode.INTERNAL_ERROR.getStatus()), ErrorCode.INTERNAL_ERROR,
                ErrorCode.INTERNAL_ERROR.getMessage(), null);
    }

    /**
     * Tells whether the request body could not be read: it is not valid JSON, or a value has the wrong type or
     * format (for example a date that is not ISO 8601). That is the mistake of the client.
     *
     * @param exception the exception, whose causes are searched too
     * @return {@code true} if the body is malformed
     */
    private static boolean isMalformedBody(final Throwable exception) {
        for (Throwable cause = exception; cause != null; cause = cause.getCause()) {
            if (cause instanceof JsonbException || cause instanceof JsonException) {
                return true;
            }
            if (cause.getCause() == cause) {
                break;
            }
        }
        return false;
    }

    private static Response build(final Response.ResponseBuilder builder, final ErrorCode code,
                                  final String message, final Map<String, String> details) {
        return builder
                .entity(ErrorResponse.of(code, message, details))
                .type(MediaType.APPLICATION_JSON)
                .build();
    }

    private static String messageOf(final WebApplicationException exception, final ErrorCode code) {
        String message = exception.getMessage();
        if (message == null || message.isBlank() || message.startsWith(DEFAULT_MESSAGE_PREFIX)) {
            return code.getMessage();
        }
        return message;
    }

    /**
     * Finds the name of the invalid field: the last node of the property path (the earlier nodes are the method
     * and the parameter of the resource).
     *
     * @param path the property path of the violation
     * @return the name of the field
     */
    private static String fieldOf(final Path path) {
        String field = "";
        for (Path.Node node : path) {
            field = node.getName();
        }
        return field;
    }
}
