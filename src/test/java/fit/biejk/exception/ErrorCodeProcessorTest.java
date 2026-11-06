package fit.biejk.exception;

import io.quarkiverse.resteasy.problem.HttpProblem;
import io.quarkiverse.resteasy.problem.postprocessing.ProblemContext;
import jakarta.ws.rs.core.Response;
import org.junit.jupiter.api.Test;

import java.net.URI;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;

class ErrorCodeProcessorTest {

    private final ErrorCodeProcessor processor = new ErrorCodeProcessor();

    private HttpProblem process(final HttpProblem problem) {
        return processor.apply(problem, ProblemContext.of(problem, "/api/v1/orders"));
    }

    @Test
    void aProblemOfAnApiExceptionKeepsItsCode() {
        HttpProblem problem = new OrderClosedException();

        assertSame(problem, process(problem));
        assertEquals("ORDER_CLOSED", problem.getParameters().get("code"));
        assertEquals(URI.create("urn:profinder:problem:order-closed"), problem.getType());
        assertEquals("Order closed", problem.getTitle());
        assertEquals(409, problem.getStatusCode());
    }

    @Test
    void aBuiltInProblemGetsTheGenericCodeOfItsStatus() {
        HttpProblem result = process(HttpProblem.valueOf(Response.Status.NOT_FOUND, "no such order"));

        assertEquals("NOT_FOUND", result.getParameters().get("code"));
        assertEquals(URI.create("urn:profinder:problem:not-found"), result.getType());
        assertEquals("no such order", result.getDetail());
    }

    @Test
    void aFailedValidationIsAValidationError() {
        HttpProblem result = process(HttpProblem.valueOf(Response.Status.BAD_REQUEST));

        assertEquals("VALIDATION_ERROR", result.getParameters().get("code"));
    }

    @Test
    void aServerErrorIsAnInternalError() {
        HttpProblem result = process(HttpProblem.valueOf(Response.Status.BAD_GATEWAY));

        assertEquals("INTERNAL_ERROR", result.getParameters().get("code"));
    }

    @Test
    void aStatusThatIsNotInTheCatalogIsNotChanged() {
        HttpProblem problem = HttpProblem.valueOf(Response.Status.GONE);

        assertSame(problem, process(problem));
        assertFalse(problem.getParameters().containsKey("code"));
    }

    @Test
    void tooManyAttemptsKeepsRetryAfter() {
        TooManyAttemptsException problem = new TooManyAttemptsException(120);

        assertEquals("RATE_LIMITED", problem.getParameters().get("code"));
        assertEquals(120L, problem.getHeaders().get("Retry-After"));
    }
}
