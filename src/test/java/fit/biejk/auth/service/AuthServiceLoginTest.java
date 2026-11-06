package fit.biejk.auth.service;

import fit.biejk.auth.entity.User;
import fit.biejk.auth.entity.UserRole;
import fit.biejk.auth.exception.InvalidCredentialsException;
import fit.biejk.auth.utilits.CryptoUtils;
import fit.biejk.core.service.ProfileService;
import fit.biejk.notification.service.MailService;
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
