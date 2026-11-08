package io.github.strogolsky.profinder.auth.exception;

import io.github.strogolsky.profinder.common.exception.ApiException;
import io.github.strogolsky.profinder.common.exception.ErrorCode;

/**
 * An account with this email already exists.
 */
public class EmailAlreadyRegisteredException extends ApiException {

    private static final long serialVersionUID = 1L;

    /**
     * Creates the exception.
     */
    public EmailAlreadyRegisteredException() {
        super(ErrorCode.EMAIL_ALREADY_REGISTERED, ErrorCode.EMAIL_ALREADY_REGISTERED.getMessage());
    }
}
