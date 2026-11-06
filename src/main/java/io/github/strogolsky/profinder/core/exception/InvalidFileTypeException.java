package io.github.strogolsky.profinder.core.exception;

import io.github.strogolsky.profinder.common.exception.ApiException;
import io.github.strogolsky.profinder.common.exception.ErrorCode;

/**
 * The uploaded file has a type that is not allowed.
 */
public class InvalidFileTypeException extends ApiException {

    private static final long serialVersionUID = 1L;

    /**
     * Creates the exception.
     */
    public InvalidFileTypeException() {
        super(ErrorCode.INVALID_FILE_TYPE, ErrorCode.INVALID_FILE_TYPE.getMessage());
    }
}
