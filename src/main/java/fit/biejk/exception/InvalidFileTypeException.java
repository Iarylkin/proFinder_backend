package fit.biejk.exception;

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
