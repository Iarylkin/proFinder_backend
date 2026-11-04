package fit.biejk.exception;

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
