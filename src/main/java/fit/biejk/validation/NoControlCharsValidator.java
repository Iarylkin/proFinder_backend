package fit.biejk.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

/**
 * Validator of {@link NoControlChars}.
 */
public final class NoControlCharsValidator implements ConstraintValidator<NoControlChars, CharSequence> {

    /**
     * The first character that is not a control character.
     */
    private static final char FIRST_PRINTABLE = ' ';

    /**
     * The delete character, the last control character.
     */
    private static final char DELETE = '\u007F';

    /**
     * Whether line feeds and tabs are allowed.
     */
    private boolean multiline;

    @Override
    public void initialize(final NoControlChars annotation) {
        this.multiline = annotation.multiline();
    }

    @Override
    public boolean isValid(final CharSequence value, final ConstraintValidatorContext context) {
        if (value == null) {
            return true;
        }
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            boolean control = c < FIRST_PRINTABLE || c == DELETE;
            boolean allowed = multiline && (c == '\n' || c == '\t');
            if (control && !allowed) {
                return false;
            }
        }
        return true;
    }
}
