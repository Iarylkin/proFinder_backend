package fit.biejk.validation;

import java.util.Optional;

/**
 * Cleaning of the text fields of a request (docs/13, "Global rules").
 */
public final class Text {

    private Text() {
    }

    /**
     * Cleans the text of a field that a partial update can clear. The field has three states: not sent
     * ({@code null}, keep the value), sent as {@code null} or as an empty text ({@link Optional#empty()}, clear the
     * value) and sent with a text (that text, trimmed).
     *
     * @param value the field of the request
     * @return the field with the white space around its text removed; an empty text is {@link Optional#empty()}
     */
    public static Optional<String> cleanOrClear(final Optional<String> value) {
        if (value == null) {
            return null;
        }
        return value.map(String::strip).filter(text -> !text.isEmpty());
    }

    /**
     * Writes a field of a partial update as the text of the entity that holds the changes: the empty text means
     * "clear the field", {@code null} means "keep it".
     *
     * @param value the field of the request
     * @return {@code null} if the field was not sent, the empty text to clear it, or the text
     */
    public static String toChange(final Optional<String> value) {
        return value == null ? null : value.orElse("");
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
