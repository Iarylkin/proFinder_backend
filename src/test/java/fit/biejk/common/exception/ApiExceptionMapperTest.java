package fit.biejk.common.exception;

import jakarta.persistence.OptimisticLockException;
import jakarta.ws.rs.core.Response;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ApiExceptionMapperTest {

    @Test
    void optimisticLockFailureIsConflict() {
        Response response = new ApiExceptionMapper().map(new OptimisticLockException());

        assertEquals(409, response.getStatus());
        assertEquals("application/problem+json", response.getMediaType().toString());
    }
}
