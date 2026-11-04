package fit.biejk.exception;

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
