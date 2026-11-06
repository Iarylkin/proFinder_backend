package fit.biejk.exception;

import io.quarkiverse.resteasy.problem.HttpProblem;

/**
 * An API error that carries its own {@link ErrorCode}. It is a problem of RFC 9457 ({@link HttpProblem}): the
 * {@code type}, {@code title} and status come from the code, the {@code detail} is the message safe to show to the
 * user, and the extension member {@code code} names the code.
 * <p>
 * Used where the client must tell the error apart from the generic one of the same status. Where the status
 * alone is enough, the built-in {@code BadRequestException}, {@code ForbiddenException},
 * {@code NotFoundException} are thrown instead.
 * </p>
 */
public abstract class ApiException extends HttpProblem {

    private static final long serialVersionUID = 1L;

    /**
     * The code of the error.
     */
    private final ErrorCode code;

    /**
     * Creates an error with the status of the code.
     *
     * @param code   the error code
     * @param detail the message safe to show to the user
     */
    protected ApiException(final ErrorCode code, final String detail) {
        this(code, problem(code, detail));
    }

    /**
     * Creates an error from a prepared problem (for example, one that carries extra headers).
     *
     * @param code    the error code
     * @param problem the problem, made by {@link #problem(ErrorCode, String)}
     */
    protected ApiException(final ErrorCode code, final HttpProblem.Builder problem) {
        super(problem);
        this.code = code;
    }

    /**
     * Starts the problem of a code: its type, title and status, the detail, and the member {@code code}.
     *
     * @param code   the error code
     * @param detail the message safe to show to the user
     * @return the builder, to which the caller may add headers
     */
    protected static HttpProblem.Builder problem(final ErrorCode code, final String detail) {
        return HttpProblem.builder()
                .withType(code.getTypeUri())
                .withTitle(code.getTitle())
                .withStatus(code.getStatus())
                .withDetail(detail)
                .with(ErrorCode.MEMBER, code.name());
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
