package io.github.strogolsky.profinder.common.exception;

/**
 * The client changes a resource on the basis of a version that is no longer the current one: someone else changed
 * the resource after the client read it (HTTP 409, {@code CONFLICT}). The client must read the resource again.
 */
public class StaleVersionException extends ApiException {

    private static final long serialVersionUID = 1L;

    /**
     * Creates the exception.
     */
    public StaleVersionException() {
        super(ErrorCode.CONFLICT, ErrorCode.CONFLICT.getMessage());
    }
}
