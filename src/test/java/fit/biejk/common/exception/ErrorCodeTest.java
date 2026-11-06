package fit.biejk.common.exception;

import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ErrorCodeTest {

    private static final int METHOD_NOT_ALLOWED = 405;

    private static final int SERVICE_UNAVAILABLE = 503;

    @Test
    void statusesOfTheCatalog() {
        assertEquals(400, ErrorCode.VALIDATION_ERROR.getStatus());
        assertEquals(401, ErrorCode.INVALID_CREDENTIALS.getStatus());
        assertEquals(409, ErrorCode.EMAIL_ALREADY_REGISTERED.getStatus());
        assertEquals(413, ErrorCode.FILE_TOO_LARGE.getStatus());
        assertEquals(415, ErrorCode.INVALID_FILE_TYPE.getStatus());
        assertEquals(429, ErrorCode.RATE_LIMITED.getStatus());
        assertEquals(500, ErrorCode.INTERNAL_ERROR.getStatus());
    }

    @Test
    void genericCodeOfAStatus() {
        assertEquals(Optional.of(ErrorCode.VALIDATION_ERROR), ErrorCode.forStatus(400));
        assertEquals(Optional.of(ErrorCode.UNAUTHENTICATED), ErrorCode.forStatus(401));
        assertEquals(Optional.of(ErrorCode.FORBIDDEN), ErrorCode.forStatus(403));
        assertEquals(Optional.of(ErrorCode.NOT_FOUND), ErrorCode.forStatus(404));
        assertEquals(Optional.of(ErrorCode.CONFLICT), ErrorCode.forStatus(409));
        assertEquals(Optional.of(ErrorCode.RATE_LIMITED), ErrorCode.forStatus(429));
    }

    @Test
    void everyServerErrorIsInternalError() {
        assertEquals(Optional.of(ErrorCode.INTERNAL_ERROR), ErrorCode.forStatus(SERVICE_UNAVAILABLE));
    }

    @Test
    void statusThatIsNotInTheCatalogHasNoCode() {
        assertTrue(ErrorCode.forStatus(METHOD_NOT_ALLOWED).isEmpty());
    }
}
