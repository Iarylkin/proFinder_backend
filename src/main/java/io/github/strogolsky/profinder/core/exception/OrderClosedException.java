package io.github.strogolsky.profinder.core.exception;

import io.github.strogolsky.profinder.common.exception.ApiException;
import io.github.strogolsky.profinder.common.exception.ErrorCode;

/**
 * The order does not accept bids any more.
 */
public class OrderClosedException extends ApiException {

    private static final long serialVersionUID = 1L;

    /**
     * Creates the exception.
     */
    public OrderClosedException() {
        super(ErrorCode.ORDER_CLOSED, ErrorCode.ORDER_CLOSED.getMessage());
    }
}
