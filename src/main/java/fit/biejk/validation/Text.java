package fit.biejk.validation;

/**
 * Cleaning of the text fields of a request (docs/13, "Global rules").
 */
public final class Text {

    private Text() {
    }

    /**
     * Removes the white space around the text of a field of a partial update ({@code PATCH}). Unlike {@link
     * #clean(String)} an empty text stays empty: for a field that can be cleared it means "clear", for a required
     * field the validation rejects it.
     *
     * @param value the text from the request, may be {@code null} (the field was not sent)
     * @return the trimmed text, or {@code null} if the field was not sent
     */
    public static String strip(final String value) {
        return value == null ? null : value.strip();
    }

    /**
     * Reads a text of the entity that holds the changes of a partial update: the empty text means "clear the field",
     * so it is stored as {@code null}.
     *
     * @param value the text of a change that is not {@code null}
     * @return {@code null} for the empty text, the text itself otherwise
     */
    public static String emptyToNull(final String value) {
        return value.isEmpty() ? null : value;
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
