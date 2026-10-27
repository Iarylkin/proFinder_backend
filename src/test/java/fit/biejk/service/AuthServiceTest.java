package fit.biejk.service;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class AuthServiceTest {

    @Test
    void emailIsTrimmedAndLowerCased() {
        assertEquals("user@example.com", AuthService.normalizeEmail("  User@Example.COM "));
    }
}
