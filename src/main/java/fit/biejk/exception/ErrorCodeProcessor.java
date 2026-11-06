package fit.biejk.exception;

import com.fasterxml.jackson.core.JsonProcessingException;
import io.quarkiverse.resteasy.problem.HttpProblem;
import io.quarkiverse.resteasy.problem.postprocessing.ProblemContext;
import io.quarkiverse.resteasy.problem.postprocessing.ProblemPostProcessor;
import jakarta.enterprise.context.ApplicationScoped;

/**
 * Gives every problem of the library (the built-in exceptions: {@code 404}, a failed validation, an unreadable body,
 * a server error) the {@code code}, {@code type} and {@code title} of the generic code of its status, and replaces
 * the text a framework made up. A problem that already has a code (an {@link ApiException}) is not changed.
 */
@ApplicationScoped
public class ErrorCodeProcessor implements ProblemPostProcessor {

    /**
     * Prefix of the default message that the framework makes up from the status.
     */
    private static final String DEFAULT_DETAIL_PREFIX = "HTTP ";

    /**
     * Detail of a body that cannot be read. The text of the parser is not shown: it names internal types.
     */
    private static final String MALFORMED_BODY = "The request body is malformed";

    /**
     * Adds the code, type, title and detail of the generic code of the status to a problem that has no code.
     *
     * @param problem the problem the library made
     * @param context the exception and the request the problem comes from
     * @return the problem with the code; the same problem if it already has one or its status has no generic code
     */
    @Override
    public HttpProblem apply(final HttpProblem problem, final ProblemContext context) {
        if (problem.getParameters().containsKey(ErrorCode.MEMBER)) {
            return problem;
        }
        return ErrorCode.forStatus(problem.getStatusCode())
                .map(code -> HttpProblem.builder(problem)
                        .withType(code.getTypeUri())
                        .withTitle(code.getTitle())
                        .withDetail(detailOf(problem, context, code))
                        .with(ErrorCode.MEMBER, code.name())
                        .build())
                .orElse(problem);
    }

    private static String detailOf(final HttpProblem problem, final ProblemContext context, final ErrorCode code) {
        if (context.cause instanceof JsonProcessingException) {
            return MALFORMED_BODY;
        }
        String detail = problem.getDetail();
        if (detail == null || detail.isBlank() || detail.startsWith(DEFAULT_DETAIL_PREFIX)) {
            return code.getMessage();
        }
        return detail;
    }
}
