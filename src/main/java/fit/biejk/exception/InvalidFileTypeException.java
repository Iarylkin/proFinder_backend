package fit.biejk.exception;

import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Response;

/**
 * Thrown when the content of an uploaded file is not an allowed type (HTTP 415, {@code INVALID_FILE_TYPE}).
 */
public class InvalidFileTypeException extends WebApplicationException {

    /** Serialization version. */
    private static final long serialVersionUID = 1L;

    /**
     * Creates the exception with the {@code INVALID_FILE_TYPE} message and status 415.
     */
    public InvalidFileTypeException() {
        super("This file type isn't allowed here", Response.Status.UNSUPPORTED_MEDIA_TYPE);
    }
}
