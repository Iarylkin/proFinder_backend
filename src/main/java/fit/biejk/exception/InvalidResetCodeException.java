package fit.biejk.exception;

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
