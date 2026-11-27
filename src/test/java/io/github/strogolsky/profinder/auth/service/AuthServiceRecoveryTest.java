package io.github.strogolsky.profinder.auth.service;

import io.github.strogolsky.profinder.auth.dto.ResetPasswordRequest;
import io.github.strogolsky.profinder.auth.entity.User;
import io.github.strogolsky.profinder.auth.entity.UserRole;
import io.github.strogolsky.profinder.auth.exception.InvalidResetCodeException;
import io.github.strogolsky.profinder.auth.exception.TooManyAttemptsException;
import io.github.strogolsky.profinder.auth.utilits.CryptoUtils;
import io.github.strogolsky.profinder.core.service.ProfileService;
import io.github.strogolsky.profinder.notification.service.MailService;
import io.quarkus.redis.client.RedisClient;
import io.vertx.redis.client.Response;
import jakarta.ws.rs.BadRequestException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * The password recovery of CLAUDE.md: {@code POST} emails a 6-digit code that is kept hashed for ten minutes,
 * {@code PATCH} resets the password with it. The code is single-use and five wrong codes within 15 minutes lock
 * the reset (429).
 */
class AuthServiceRecoveryTest {

    private static final String EMAIL = "user@example.com";
    private static final String CODE_KEY = "forgot-password:user@example.com";
    private static final String FAILURE_KEY = "reset_fail:user@example.com";
    private static final String NEW_PASSWORD = "NewPassw0rd!x";
    private static final int WINDOW_SECONDS = 900;
    private static final int MAX_ATTEMPTS = 5;
    private static final Pattern SIX_DIGITS = Pattern.compile("\\d{6}");

    private MailService mailService;
    private UserService userService;
    private RedisClient redisClient;
    private TokenService tokenService;
    private AuthService service;

    private User user;

    @BeforeEach
    void setUp() {
        mailService = mock(MailService.class);
        userService = mock(UserService.class);
        redisClient = mock(RedisClient.class);
        tokenService = mock(TokenService.class);
        service = new AuthService(mailService, mock(ProfileService.class), userService, redisClient, tokenService);

        user = new User();
        user.setId(UUID.randomUUID());
        user.setEmail(EMAIL);
        user.setRole(UserRole.CUSTOMER);

        when(userService.findByEmail(EMAIL)).thenReturn(Optional.of(user));
        when(userService.getByEmail(EMAIL)).thenReturn(user);
        when(userService.updatePassword(user.getId(), NEW_PASSWORD)).thenReturn(user);
        when(tokenService.generateToken(user)).thenReturn("token");
    }

    private static Response textResponse(final String text) {
        Response response = mock(Response.class);
        when(response.toString()).thenReturn(text);
        return response;
    }

    private static Response numberResponse(final long number) {
        Response response = mock(Response.class);
        when(response.toInteger()).thenReturn((int) number);
        when(response.toLong()).thenReturn(number);
        return response;
    }

    private static ResetPasswordRequest request(final String email, final String code) {
        return new ResetPasswordRequest(email, NEW_PASSWORD, NEW_PASSWORD, code);
    }

    // ---- POST: the code is issued ----

    @Test
    void unknownAccountGetsNoCodeAndNoMailButNoError() {
        when(userService.findByEmail("nobody@example.com")).thenReturn(Optional.empty());

        service.forgotPassword("nobody@example.com");

        verify(redisClient, never()).setex(anyString(), anyString(), anyString());
        verify(mailService, never()).send(anyString(), anyString(), anyString());
    }

    @Test
    void knownAccountGetsASixDigitCodeByMailAndOnlyItsHashIsStoredForTenMinutes() {
        service.forgotPassword(EMAIL);

        ArgumentCaptor<String> message = ArgumentCaptor.forClass(String.class);
        verify(mailService).send(eq(EMAIL), anyString(), message.capture());
        Matcher code = SIX_DIGITS.matcher(message.getValue());
        assertTrue(code.find(), "the mail holds a 6-digit code");

        ArgumentCaptor<String> stored = ArgumentCaptor.forClass(String.class);
        verify(redisClient).setex(eq(CODE_KEY), eq("600"), stored.capture());
        assertNotEquals(code.group(), stored.getValue(), "the code is not stored in the clear");
        assertTrue(CryptoUtils.verify(code.group(), stored.getValue()), "the stored value is the hash of the code");
    }

    @Test
    void theKeyOfTheCodeIsTheEmailExactlyAsSent() {
        when(userService.findByEmail("User@Example.COM")).thenReturn(Optional.of(user));

        service.forgotPassword("User@Example.COM");

        verify(redisClient).setex(eq("forgot-password:User@Example.COM"), eq("600"), anyString());
    }

    @Test
    void anEmailInAnotherCaseIsAnotherAccountAndGetsNoCode() {
        when(userService.findByEmail("USER@example.com")).thenReturn(Optional.empty());

        service.forgotPassword("USER@example.com");

        verify(userService).findByEmail("USER@example.com");
        verify(redisClient, never()).setex(anyString(), anyString(), anyString());
        verify(mailService, never()).send(anyString(), anyString(), anyString());
    }

    // ---- PATCH: the password is reset ----

    @Test
    void differentPasswordsAreRejectedBeforeAnythingIsRead() {
        ResetPasswordRequest mismatch = new ResetPasswordRequest(EMAIL, NEW_PASSWORD, "Other", "123456");

        assertThrows(BadRequestException.class, () -> service.resetPassword(mismatch));

        verify(redisClient, never()).get(anyString());
        verify(userService, never()).updatePassword(any(UUID.class), anyString());
    }

    @Test
    void validCodeResetsThePasswordReturnsATokenAndIsUsedUp() {
        when(redisClient.get(FAILURE_KEY)).thenReturn(null);
        Response stub1 = textResponse(CryptoUtils.hash("123456"));
        when(redisClient.get(CODE_KEY)).thenReturn(stub1);

        String token = service.resetPassword(request(EMAIL, "123456"));

        assertEquals("token", token);
        verify(userService).updatePassword(user.getId(), NEW_PASSWORD);
        verify(redisClient).del(List.of(CODE_KEY, FAILURE_KEY));
    }

    @Test
    void wrongCodeIsRejectedAndCounted() {
        Response stub2 = textResponse(CryptoUtils.hash("123456"));
        when(redisClient.get(CODE_KEY)).thenReturn(stub2);
        Response stub3 = numberResponse(1);
        when(redisClient.incr(FAILURE_KEY)).thenReturn(stub3);

        assertThrows(InvalidResetCodeException.class, () -> service.resetPassword(request(EMAIL, "000000")));

        verify(userService, never()).updatePassword(any(UUID.class), anyString());
        verify(redisClient).expire(FAILURE_KEY, String.valueOf(WINDOW_SECONDS));
        verify(redisClient, never()).del(anyList());
    }

    @Test
    void anExpiredOrMissingCodeIsRejectedAndCounted() {
        when(redisClient.get(CODE_KEY)).thenReturn(null);
        Response stub4 = numberResponse(1);
        when(redisClient.incr(FAILURE_KEY)).thenReturn(stub4);

        assertThrows(InvalidResetCodeException.class, () -> service.resetPassword(request(EMAIL, "123456")));

        verify(userService, never()).updatePassword(any(UUID.class), anyString());
    }

    @Test
    void theWindowOfTheCounterStartsOnlyWithTheFirstFailure() {
        Response stub5 = textResponse(CryptoUtils.hash("123456"));
        when(redisClient.get(CODE_KEY)).thenReturn(stub5);
        Response stub6 = numberResponse(2);
        when(redisClient.incr(FAILURE_KEY)).thenReturn(stub6);

        assertThrows(InvalidResetCodeException.class, () -> service.resetPassword(request(EMAIL, "000000")));

        verify(redisClient, never()).expire(anyString(), anyString());
    }

    @Test
    void theFifthWrongCodeDestroysTheCode() {
        Response stub7 = textResponse(CryptoUtils.hash("123456"));
        when(redisClient.get(CODE_KEY)).thenReturn(stub7);
        Response stub8 = numberResponse(MAX_ATTEMPTS);
        when(redisClient.incr(FAILURE_KEY)).thenReturn(stub8);

        assertThrows(InvalidResetCodeException.class, () -> service.resetPassword(request(EMAIL, "000000")));

        verify(redisClient).del(List.of(CODE_KEY));
    }

    @Test
    void afterFiveFailuresTheResetIsLockedWithTheRemainingTimeAndTheCodeIsNotEvenChecked() {
        Response stub9 = numberResponse(MAX_ATTEMPTS);
        when(redisClient.get(FAILURE_KEY)).thenReturn(stub9);
        Response stub10 = numberResponse(321);
        when(redisClient.ttl(FAILURE_KEY)).thenReturn(stub10);

        TooManyAttemptsException locked = assertThrows(TooManyAttemptsException.class,
                () -> service.resetPassword(request(EMAIL, "123456")));

        assertEquals(429, locked.getStatus().getStatusCode());
        assertEquals("321", String.valueOf(locked.getHeaders().get("Retry-After")));
        verify(redisClient, never()).get(CODE_KEY);
        verify(userService, never()).updatePassword(any(UUID.class), anyString());
    }

    @Test
    void aLockWithoutAKnownTimeWaitsTheWholeWindow() {
        Response stub11 = numberResponse(MAX_ATTEMPTS);
        when(redisClient.get(FAILURE_KEY)).thenReturn(stub11);
        Response stub12 = numberResponse(-1);
        when(redisClient.ttl(FAILURE_KEY)).thenReturn(stub12);

        TooManyAttemptsException locked = assertThrows(TooManyAttemptsException.class,
                () -> service.resetPassword(request(EMAIL, "123456")));

        assertEquals(String.valueOf(WINDOW_SECONDS), String.valueOf(locked.getHeaders().get("Retry-After")));
    }

    @Test
    void fewerThanFiveFailuresDoNotLock() {
        Response stub13 = numberResponse(MAX_ATTEMPTS - 1);
        when(redisClient.get(FAILURE_KEY)).thenReturn(stub13);
        Response stub14 = textResponse(CryptoUtils.hash("123456"));
        when(redisClient.get(CODE_KEY)).thenReturn(stub14);

        assertEquals("token", service.resetPassword(request(EMAIL, "123456")));
    }

    @Test
    void theCodeIsLookedUpUnderTheEmailExactlyAsSent() {
        Response saved = textResponse(CryptoUtils.hash("123456"));
        when(redisClient.get("forgot-password:User@Example.com")).thenReturn(saved);
        when(userService.getByEmail("User@Example.com")).thenReturn(user);

        service.resetPassword(request("User@Example.com", "123456"));

        verify(redisClient).get("forgot-password:User@Example.com");
        verify(redisClient, never()).get(CODE_KEY);
    }
}
