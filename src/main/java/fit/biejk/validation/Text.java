package fit.biejk.validation;

/**
 * Cleaning of the text fields of a request (docs/13, "Global rules").
 */
public final class Text {

    private Text() {
    }

    /**
     * Removes the white space around a text; a text that is empty after that is the same as no text.
     * Passwords, codes and tokens must not be cleaned: they are taken exactly as they are sent.
     *
     * @param value the text from the request, may be {@code null}
     * @return the trimmed text, or {@code null} if there is nothing left
     */
    public static String clean(final String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.strip();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
