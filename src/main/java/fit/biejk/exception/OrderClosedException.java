package fit.biejk.exception;

import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Response;

/**
 * Thrown when a proposal is submitted to an order that no longer accepts proposals (HTTP 409, {@code ORDER_CLOSED}).
 */
public class OrderClosedException extends WebApplicationException {

    /** Serialization version. */
    private static final long serialVersionUID = 1L;

    /**
     * Creates the exception with the {@code ORDER_CLOSED} message and status 409.
     */
    public OrderClosedException() {
        super("This order is closed", Response.Status.CONFLICT);
    }
}
