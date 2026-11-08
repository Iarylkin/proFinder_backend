package io.github.strogolsky.profinder.core.exception;

import io.github.strogolsky.profinder.common.exception.ApiException;
import io.github.strogolsky.profinder.common.exception.ErrorCode;

/**
 * The professional tried to remove all their categories; at least one must stay.
 */
public class CategoryRequiredException extends ApiException {

    private static final long serialVersionUID = 1L;

    /**
     * Creates the exception.
     */
    public CategoryRequiredException() {
        super(ErrorCode.CATEGORY_REQUIRED, ErrorCode.CATEGORY_REQUIRED.getMessage());
    }
}
