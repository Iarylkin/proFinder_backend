package fit.biejk.core.exception;

import fit.biejk.common.exception.ApiException;
import fit.biejk.common.exception.ErrorCode;

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
