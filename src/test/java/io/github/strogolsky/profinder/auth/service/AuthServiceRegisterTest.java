package io.github.strogolsky.profinder.auth.service;

import io.github.strogolsky.profinder.auth.entity.User;
import io.github.strogolsky.profinder.auth.entity.UserRole;
import io.github.strogolsky.profinder.core.service.ProfileService;
import io.github.strogolsky.profinder.notification.service.MailService;
import io.quarkus.redis.client.RedisClient;
import jakarta.ws.rs.BadRequestException;

import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AuthServiceRegisterTest {

    private static final UUID USER_ID = UUID.randomUUID();

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
        when(userService.create(any(User.class))).thenAnswer(call -> {
            User created = call.getArgument(0);
            created.setId(USER_ID);
            return created;
        });
        when(tokenService.generateToken(any(User.class))).thenReturn("token");
    }

    @Test
    void registerCreatesTheUserAndItsProfile() {
        String token = service.register("pro@example.com", "secret", UserRole.PROFESSIONAL);

        assertEquals("token", token);
        verify(profileService).create(USER_ID, UserRole.PROFESSIONAL);
    }

    @Test
    void registerWithoutARoleIsRejected() {
        assertThrows(BadRequestException.class, () -> service.register("x@example.com", "secret", null));

        verify(userService, never()).create(any(User.class));
    }

    @Test
    void failedProfileDeletesTheNewUser() {
        when(profileService.create(USER_ID, UserRole.CUSTOMER)).thenThrow(new IllegalStateException("core is down"));

        assertThrows(IllegalStateException.class,
                () -> service.register("c@example.com", "secret", UserRole.CUSTOMER));

        verify(userService).delete(USER_ID);
    }

    @Test
    void failedUndoDoesNotHideTheOriginalError() {
        when(profileService.create(USER_ID, UserRole.CUSTOMER)).thenThrow(new IllegalStateException("core is down"));
        doThrow(new IllegalStateException("auth is down")).when(userService).delete(USER_ID);

        IllegalStateException thrown = assertThrows(IllegalStateException.class,
                () -> service.register("c@example.com", "secret", UserRole.CUSTOMER));

        assertEquals("core is down", thrown.getMessage());
    }
}
