package io.github.strogolsky.profinder.core.exception;

import io.github.strogolsky.profinder.common.exception.ApiException;
import io.github.strogolsky.profinder.common.exception.ErrorCode;

/**
 * The uploaded file is over the size limit.
 */
public class FileTooLargeException extends ApiException {

    private static final long serialVersionUID = 1L;

    /**
     * Creates the exception.
     */
    public FileTooLargeException() {
        super(ErrorCode.FILE_TOO_LARGE, ErrorCode.FILE_TOO_LARGE.getMessage());
    }
}
