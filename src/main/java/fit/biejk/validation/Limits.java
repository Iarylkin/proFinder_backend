package fit.biejk.validation;

/**
 * The limits of the input fields (see {@code docs/13 - Validation Rules.md}). The DTOs validate against them and
 * the entities size their columns with them, so a value that passes the validation always fits the column.
 */
public final class Limits {

    /**
     * Longest email address (RFC 5321).
     */
    public static final int EMAIL_MAX_LENGTH = 254;

    /**
     * Shortest password that is accepted when a new one is set.
     */
    public static final int PASSWORD_MIN_LENGTH = 8;

    /**
     * Longest password. bcrypt looks only at the first 72 bytes, the limit just keeps the request small.
     */
    public static final int PASSWORD_MAX_LENGTH = 128;

    /**
     * Shortest order title.
     */
    public static final int ORDER_TITLE_MIN_LENGTH = 5;

    /**
     * Longest order title.
     */
    public static final int ORDER_TITLE_MAX_LENGTH = 120;

    /**
     * Shortest order description.
     */
    public static final int ORDER_DESCRIPTION_MIN_LENGTH = 20;

    /**
     * Longest order description.
     */
    public static final int ORDER_DESCRIPTION_MAX_LENGTH = 5000;

    /**
     * Longest description of a response to an order.
     */
    public static final int RESPONSE_DESCRIPTION_MAX_LENGTH = 2000;

    /**
     * Longest description (bio) of a professional.
     */
    public static final int BIO_MAX_LENGTH = 2000;

    /**
     * Shortest review comment.
     */
    public static final int REVIEW_COMMENT_MIN_LENGTH = 5;

    /**
     * Longest review comment.
     */
    public static final int REVIEW_COMMENT_MAX_LENGTH = 1000;

    /**
     * Highest price in an order or a response (a sanity cap).
     */
    public static final int PRICE_MAX = 1_000_000;

    /**
     * Smallest page number; the pages are 1-based.
     */
    public static final int PAGE_MIN = 1;

    /**
     * Page size used when the client sends none.
     */
    public static final int PAGE_SIZE_DEFAULT = 20;

    /**
     * Largest page size; a bigger one is reduced to it.
     */
    public static final int PAGE_SIZE_MAX = 100;

    /**
     * A person's name: letters, marks, digits, spaces and {@code . ' -}.
     */
    public static final String NAME_PATTERN = "^[\\p{L}\\p{M}\\p{N} .'-]*$";

    /**
     * A phone number: digits, a leading plus, spaces, brackets and hyphens.
     */
    public static final String PHONE_PATTERN = "^\\+?[0-9 ()-]*$";

    /**
     * A text that has at least one character other than white space. Unlike {@code @NotBlank} it lets a missing
     * value pass, which is what an optional field of a partial update needs.
     */
    public static final String NOT_BLANK_PATTERN = "(?s).*\\S.*";

    private Limits() {
    }
}
