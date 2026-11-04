package fit.biejk.exception;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.exc.MismatchedInputException;
import jakarta.persistence.OptimisticLockException;
import jakarta.persistence.PersistenceException;
import jakarta.transaction.RollbackException;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Path;
import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.ForbiddenException;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Response;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ApiExceptionMapperTest {

    private static final int METHOD_NOT_ALLOWED = 405;

    private static final int BAD_GATEWAY = 502;

    private static final int RETRY_AFTER_SECONDS = 30;

    private final ApiExceptionMapper mapper = new ApiExceptionMapper();

    private static ErrorResponse.Body bodyOf(final Response response) {
        return ((ErrorResponse) response.getEntity()).getError();
    }

    @Test
    void apiExceptionKeepsItsOwnCode() {
        Response response = mapper.map(new OrderClosedException());

        assertEquals(409, response.getStatus());
        assertEquals("ORDER_CLOSED", bodyOf(response).getCode());
        assertEquals("This order is closed", bodyOf(response).getMessage());
        assertNull(bodyOf(response).getDetails());
    }

    @Test
    void builtInExceptionsGetTheGenericCodeOfTheirStatus() {
        assertEquals("NOT_FOUND", bodyOf(mapper.map(new NotFoundException("Order not found"))).getCode());
        assertEquals("FORBIDDEN", bodyOf(mapper.map(new ForbiddenException("No access"))).getCode());
        assertEquals("VALIDATION_ERROR", bodyOf(mapper.map(new BadRequestException("Bad"))).getCode());
        assertEquals("CONFLICT", bodyOf(mapper.map(new StatusConflictException("Cannot change"))).getCode());
    }

    @Test
    void messageOfTheExceptionIsKept() {
        Response response = mapper.map(new NotFoundException("Order not found"));

        assertEquals(404, response.getStatus());
        assertEquals("Order not found", bodyOf(response).getMessage());
    }

    @Test
    void defaultMessageOfTheFrameworkIsReplacedByTheMessageOfTheCode() {
        assertEquals("Not found", bodyOf(mapper.map(new NotFoundException())).getMessage());
    }

    @Test
    void headersOfTheExceptionAreKept() {
        Response response = mapper.map(new TooManyAttemptsException(RETRY_AFTER_SECONDS));

        assertEquals(429, response.getStatus());
        assertEquals("RATE_LIMITED", bodyOf(response).getCode());
        assertEquals((long) RETRY_AFTER_SECONDS, response.getHeaders().getFirst("Retry-After"));
    }

    @Test
    void statusThatIsNotInTheCatalogIsPassedThrough() {
        WebApplicationException exception = new WebApplicationException(METHOD_NOT_ALLOWED);

        assertSame(exception.getResponse(), mapper.map(exception));
    }

    @Test
    void messageOfAServerErrorIsNotShown() {
        Response response = mapper.map(new WebApplicationException("database password is wrong", BAD_GATEWAY));

        assertEquals(BAD_GATEWAY, response.getStatus());
        assertEquals("INTERNAL_ERROR", bodyOf(response).getCode());
        assertEquals("Something went wrong, please try again", bodyOf(response).getMessage());
    }

    @Test
    void validationErrorListsTheInvalidFields() {
        ConstraintViolationException exception = new ConstraintViolationException(Set.of(
                violation("create", "title", "must not be blank")));

        Response response = mapper.map(exception);

        assertEquals(400, response.getStatus());
        assertEquals("VALIDATION_ERROR", bodyOf(response).getCode());
        assertEquals(Map.of("title", "must not be blank"), bodyOf(response).getDetails());
    }

    @Test
    void optimisticLockFailureIsConflict() {
        Response response = mapper.map(new OptimisticLockException("stale"));

        assertEquals(409, response.getStatus());
        assertEquals("CONFLICT", bodyOf(response).getCode());
    }

    @Test
    void optimisticLockFailureInsideARollbackIsConflict() {
        RollbackException rollback = new RollbackException("rolled back");
        rollback.initCause(new PersistenceException(new OptimisticLockException("stale")));

        assertEquals(409, mapper.map(rollback).getStatus());
    }

    @Test
    void unreadableBodyIsAValidationErrorWithoutTheParserText() {
        Response response = mapper.map(
                new JsonMappingException(null, "Cannot parse 2026-13-45 as java.time.LocalDateTime"));

        assertEquals(400, response.getStatus());
        assertEquals("VALIDATION_ERROR", bodyOf(response).getCode());
        assertEquals("The request body is malformed", bodyOf(response).getMessage());
    }

    @Test
    void unreadableBodyInsideAnotherExceptionIsAValidationError() {
        assertEquals(400, mapper.map(
                new RuntimeException(new JsonMappingException(null, "Unexpected character"))).getStatus());
    }

    @Test
    void aValueOfTheWrongFormatHasTheCommonBodyInsteadOfTheOneOfQuarkus() {
        Response response = mapper.map(MismatchedInputException.from((JsonParser) null, String.class, "bad date"));

        assertEquals(400, response.getStatus());
        assertEquals("VALIDATION_ERROR", bodyOf(response).getCode());
        assertEquals("The request body is malformed", bodyOf(response).getMessage());
    }

    @Test
    void anyOtherErrorIsAServerErrorWithoutItsMessage() {
        RollbackException rollback = new RollbackException("secret table name");
        rollback.initCause(new IllegalStateException("something else"));

        Response response = mapper.map(rollback);

        assertEquals(500, response.getStatus());
        assertEquals("INTERNAL_ERROR", bodyOf(response).getCode());
        assertEquals("Something went wrong, please try again", bodyOf(response).getMessage());
    }

    private static ConstraintViolation<Object> violation(final String method, final String field,
                                                         final String message) {
        Path.Node methodNode = node(method);
        Path.Node fieldNode = node(field);
        Path path = mock(Path.class);
        when(path.iterator()).thenReturn(List.of(methodNode, fieldNode).iterator());
        @SuppressWarnings("unchecked")
        ConstraintViolation<Object> violation = mock(ConstraintViolation.class);
        when(violation.getPropertyPath()).thenReturn(path);
        when(violation.getMessage()).thenReturn(message);
        return violation;
    }

    private static Path.Node node(final String name) {
        Path.Node node = mock(Path.Node.class);
        when(node.getName()).thenReturn(name);
        return node;
    }
}
