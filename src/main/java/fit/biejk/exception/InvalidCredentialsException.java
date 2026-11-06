package fit.biejk.exception;

/**
 * Wrong email, password or role at sign-in. The same answer for all of them, so it does not reveal whether
 * the email is registered.
 */
public class InvalidCredentialsException extends ApiException {

    private static final long serialVersionUID = 1L;

    /**
     * Creates the exception.
     */
    public InvalidCredentialsException() {
        super(ErrorCode.INVALID_CREDENTIALS, ErrorCode.INVALID_CREDENTIALS.getMessage());
    }
}
