package fit.biejk.utilits;

import at.favre.lib.crypto.bcrypt.BCrypt;
import org.apache.commons.text.RandomStringGenerator;

import java.security.SecureRandom;

/**
 * Utility class for password hashing and code generation.
 */
public final class CryptoUtils {

    /**
     * BCrypt cost factor.
     */
    private static final int BCRYPT_COST = 12;

    /**
     * Cryptographically strong random source used for generated codes.
     */
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    /**
     * Private constructor to prevent instantiation.
     */
    private CryptoUtils() {
        throw new UnsupportedOperationException("Utility class");
    }

    /**
     * Generates a random numeric code using a cryptographically strong random source.
     *
     * @param length number of digits
     * @return generated code
     */
    public static String generateNumericCode(final int length) {
        return new RandomStringGenerator.Builder()
                .withinRange('0', '9')
                .usingRandom(SECURE_RANDOM::nextInt)
                .build()
                .generate(length);
    }

    /**
     * Hashes plain text using BCrypt.
     *
     * @param plain plain text (e.g. password)
     * @return hashed value
     */
    public static String hash(final String plain) {
        return BCrypt.withDefaults().hashToString(BCRYPT_COST, plain.toCharArray());
    }

    /**
     * Verifies plain text against a BCrypt hash.
     *
     * @param plain  plain text
     * @param hashed hashed value
     * @return true if match, false otherwise
     */
    public static boolean verify(final String plain, final String hashed) {
        return BCrypt.verifyer().verify(plain.toCharArray(), hashed).verified;
    }
}
