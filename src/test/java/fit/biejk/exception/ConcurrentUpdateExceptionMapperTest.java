package fit.biejk.exception;

import jakarta.persistence.OptimisticLockException;
import jakarta.persistence.PersistenceException;
import jakarta.transaction.RollbackException;
import jakarta.ws.rs.core.Response;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ConcurrentUpdateExceptionMapperTest {

    private final ConcurrentUpdateExceptionMapper mapper = new ConcurrentUpdateExceptionMapper();

    @Test
    void optimisticLockFailureIsConflict() {
        Response response = mapper.map(new OptimisticLockException("stale"));

        assertEquals(409, response.getStatus());
        assertEquals(ConcurrentUpdateExceptionMapper.MESSAGE, response.getEntity());
    }

    @Test
    void optimisticLockFailureInsideARollbackIsConflict() {
        RollbackException rollback = new RollbackException("rolled back");
        rollback.initCause(new PersistenceException(new OptimisticLockException("stale")));

        Response response = mapper.map(rollback);

        assertEquals(409, response.getStatus());
    }

    @Test
    void otherRollbackIsAServerError() {
        RollbackException rollback = new RollbackException("rolled back");
        rollback.initCause(new IllegalStateException("something else"));

        assertEquals(500, mapper.map(rollback).getStatus());
    }
}
