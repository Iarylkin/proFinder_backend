package io.github.strogolsky.profinder.auth.exception;

import io.github.strogolsky.profinder.common.exception.ApiException;
import io.github.strogolsky.profinder.common.exception.ErrorCode;

/**
 * The password reset code is wrong, expired or already used.
 */
public class InvalidResetCodeException extends ApiException {

    private static final long serialVersionUID = 1L;

    /**
     * Creates the exception.
     */
    public InvalidResetCodeException() {
        super(ErrorCode.RESET_TOKEN_INVALID, ErrorCode.RESET_TOKEN_INVALID.getMessage());
    }
}
