package fit.biejk.exception;

import io.quarkiverse.resteasy.problem.HttpProblem;
import io.quarkiverse.resteasy.problem.postprocessing.ProblemContext;
import io.quarkiverse.resteasy.problem.postprocessing.ProblemPostProcessor;
import jakarta.inject.Singleton;

/**
 * Gives every problem a {@code code}: the problems of the library for the built-in exceptions ({@code 404}, a failed
 * validation, ...) get the generic code of their status and its {@code type}. A problem that already has a code
 * (an {@link ApiException}) is not changed.
 */
@Singleton
public final class ErrorCodeProcessor implements ProblemPostProcessor {

    @Override
    public HttpProblem apply(final HttpProblem problem, final ProblemContext context) {
        if (problem.getParameters().containsKey(ErrorCode.MEMBER)) {
            return problem;
        }
        return ErrorCode.forStatus(problem.getStatusCode())
                .map(code -> HttpProblem.builder(problem)
                        .withType(code.getTypeUri())
                        .with(ErrorCode.MEMBER, code.name())
                        .build())
                .orElse(problem);
    }
}
