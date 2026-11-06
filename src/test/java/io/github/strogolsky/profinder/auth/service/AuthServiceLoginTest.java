package io.github.strogolsky.profinder.auth.service;

import io.github.strogolsky.profinder.auth.entity.User;
import io.github.strogolsky.profinder.auth.entity.UserRole;
import io.github.strogolsky.profinder.auth.exception.InvalidCredentialsException;
import io.github.strogolsky.profinder.auth.utilits.CryptoUtils;
import io.github.strogolsky.profinder.core.service.ProfileService;
import io.github.strogolsky.profinder.notification.service.MailService;
import io.quarkus.redis.client.RedisClient;
import io.quarkus.security.identity.SecurityIdentity;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class AuthServiceLoginTest {

    private static final String EMAIL = "user@example.com";

    private static final String PASSWORD = "correct-password";

    private UserService userService;
    private TokenService tokenService;
    private AuthService service;

    @BeforeEach
    void setUp() {
        userService = mock(UserService.class);
        tokenService = mock(TokenService.class);
        service = new AuthService(mock(MailService.class), mock(ProfileService.class), userService,
                mock(SecurityIdentity.class), mock(RedisClient.class), tokenService);
    }

    private User user() {
        User user = new User();
        user.setEmail(EMAIL);
        user.setPassword(CryptoUtils.hash(PASSWORD));
        user.setRole(UserRole.CUSTOMER);
        return user;
    }

    @Test
    void loginReturnsTheTokenForValidCredentials() {
        User user = user();
        when(userService.findByEmail(EMAIL)).thenReturn(Optional.of(user));
        when(tokenService.generateToken(user)).thenReturn("token");

        assertEquals("token", service.login(EMAIL, PASSWORD, UserRole.CUSTOMER));
    }

    @Test
    void unknownEmailIsInvalidCredentials() {
        when(userService.findByEmail(EMAIL)).thenReturn(Optional.empty());

        assertThrows(InvalidCredentialsException.class, () -> service.login(EMAIL, PASSWORD, UserRole.CUSTOMER));
    }

    @Test
    void wrongPasswordIsInvalidCredentials() {
        when(userService.findByEmail(EMAIL)).thenReturn(Optional.of(user()));

        assertThrows(InvalidCredentialsException.class, () -> service.login(EMAIL, "wrong", UserRole.CUSTOMER));
    }

    @Test
    void wrongRoleIsInvalidCredentials() {
        when(userService.findByEmail(EMAIL)).thenReturn(Optional.of(user()));

        assertThrows(InvalidCredentialsException.class, () -> service.login(EMAIL, PASSWORD, UserRole.PROFESSIONAL));
    }
}
