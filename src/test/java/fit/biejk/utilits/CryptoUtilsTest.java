package fit.biejk.utilits;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CryptoUtilsTest {

    private static final int CODE_LENGTH = 6;

    @Test
    void generatedCodeHasRequestedLengthAndOnlyDigits() {
        String code = CryptoUtils.generateNumericCode(CODE_LENGTH);

        assertEquals(CODE_LENGTH, code.length());
        assertTrue(code.chars().allMatch(Character::isDigit));
    }
}
