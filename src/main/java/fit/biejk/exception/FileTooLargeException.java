package fit.biejk.exception;

import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Response;

/**
 * Thrown when an uploaded file exceeds the allowed size (HTTP 413, {@code FILE_TOO_LARGE}).
 */
public class FileTooLargeException extends WebApplicationException {

    /** Serialization version. */
    private static final long serialVersionUID = 1L;

    /**
     * Creates the exception with the {@code FILE_TOO_LARGE} message and status 413.
     */
    public FileTooLargeException() {
        super("File exceeds the size limit", Response.Status.REQUEST_ENTITY_TOO_LARGE);
    }
}
