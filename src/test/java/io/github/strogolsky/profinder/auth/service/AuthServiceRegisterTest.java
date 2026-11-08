package io.github.strogolsky.profinder.auth.service;

import io.github.strogolsky.profinder.auth.entity.User;
import io.github.strogolsky.profinder.auth.entity.UserRole;
import io.github.strogolsky.profinder.core.service.ProfileService;
import io.github.strogolsky.profinder.notification.service.MailService;
import io.quarkus.redis.client.RedisClient;
import jakarta.ws.rs.BadRequestException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AuthServiceRegisterTest {

    private UserService userService;
    private ProfileService profileService;
    private AuthService service;

    @BeforeEach
    void setUp() {
        userService = mock(UserService.class);
        profileService = mock(ProfileService.class);
        TokenService tokenService = mock(TokenService.class);
        service = new AuthService(
                mock(MailService.class),
                profileService,
                userService,
                mock(RedisClient.class),
                tokenService);
        when(userService.create(any(User.class))).thenAnswer(call -> call.getArgument(0));
        when(tokenService.generateToken(any(User.class))).thenReturn("token");
    }

    @Test
    void registerCreatesTheUserAndItsProfile() {
        String token = service.register("pro@example.com", "secret", UserRole.PROFESSIONAL);

        assertEquals("token", token);
        verify(profileService).create(any(User.class));
    }

    @Test
    void registerWithoutARoleIsRejected() {
        assertThrows(BadRequestException.class, () -> service.register("x@example.com", "secret", null));

        verify(userService, never()).create(any(User.class));
    }
}
