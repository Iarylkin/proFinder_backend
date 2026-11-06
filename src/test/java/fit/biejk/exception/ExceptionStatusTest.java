package fit.biejk.exception;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ExceptionStatusTest {

    private static final int RETRY_AFTER_SECONDS = 120;

    @Test
    void exceptionsCarryTheirHttpStatus() {
        assertEquals(409, new OrderClosedException().getStatusCode());
        assertEquals(413, new FileTooLargeException().getStatusCode());
        assertEquals(415, new InvalidFileTypeException().getStatusCode());
        assertEquals(400, new InvalidResetCodeException().getStatusCode());
        assertEquals(409, new StatusConflictException("conflict").getResponse().getStatus());
        assertEquals(401, new InvalidCredentialsException().getStatusCode());
        assertEquals(409, new EmailAlreadyRegisteredException().getStatusCode());
    }

    @Test
    void exceptionsCarryTheirErrorCode() {
        assertEquals(ErrorCode.ORDER_CLOSED, new OrderClosedException().getCode());
        assertEquals(ErrorCode.FILE_TOO_LARGE, new FileTooLargeException().getCode());
        assertEquals(ErrorCode.INVALID_FILE_TYPE, new InvalidFileTypeException().getCode());
        assertEquals(ErrorCode.RESET_TOKEN_INVALID, new InvalidResetCodeException().getCode());
        assertEquals(ErrorCode.INVALID_CREDENTIALS, new InvalidCredentialsException().getCode());
        assertEquals(ErrorCode.EMAIL_ALREADY_REGISTERED, new EmailAlreadyRegisteredException().getCode());
        assertEquals(ErrorCode.RATE_LIMITED, new TooManyAttemptsException(RETRY_AFTER_SECONDS).getCode());
    }

    @Test
    void tooManyAttemptsHasStatusAndRetryAfter() {
        TooManyAttemptsException exception = new TooManyAttemptsException(RETRY_AFTER_SECONDS);

        assertEquals(429, exception.getStatusCode());
        assertEquals((long) RETRY_AFTER_SECONDS, exception.getHeaders().get("Retry-After"));
    }
}
