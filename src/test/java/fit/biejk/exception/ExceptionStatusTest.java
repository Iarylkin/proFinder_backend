package fit.biejk.exception;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ExceptionStatusTest {

    private static final int RETRY_AFTER_SECONDS = 120;

    @Test
    void exceptionsCarryTheirHttpStatus() {
        assertEquals(409, new OrderClosedException().getResponse().getStatus());
        assertEquals(413, new FileTooLargeException().getResponse().getStatus());
        assertEquals(415, new InvalidFileTypeException().getResponse().getStatus());
        assertEquals(400, new InvalidResetCodeException().getResponse().getStatus());
        assertEquals(409, new StatusConflictException("conflict").getResponse().getStatus());
    }

    @Test
    void tooManyAttemptsHasStatusAndRetryAfter() {
        TooManyAttemptsException exception = new TooManyAttemptsException(RETRY_AFTER_SECONDS);

        assertEquals(429, exception.getResponse().getStatus());
        assertEquals((long) RETRY_AFTER_SECONDS, exception.getResponse().getHeaders().getFirst("Retry-After"));
    }
}
