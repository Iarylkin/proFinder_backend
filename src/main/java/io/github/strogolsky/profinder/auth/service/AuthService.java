package io.github.strogolsky.profinder.auth.service;

import io.github.strogolsky.profinder.auth.dto.ResetPasswordRequest;
import io.github.strogolsky.profinder.auth.entity.User;
import io.github.strogolsky.profinder.auth.entity.UserRole;
import io.github.strogolsky.profinder.auth.exception.InvalidCredentialsException;
import io.github.strogolsky.profinder.auth.exception.InvalidResetCodeException;
import io.github.strogolsky.profinder.auth.exception.TooManyAttemptsException;
import io.github.strogolsky.profinder.auth.utilits.CryptoUtils;
import io.github.strogolsky.profinder.core.service.ProfileService;
import io.github.strogolsky.profinder.notification.service.MailService;
import io.quarkus.redis.client.RedisClient;
import io.vertx.redis.client.Response;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.BadRequestException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.Locale;
import java.util.Optional;

/**
 * Service responsible for authentication logic.
 * <p>
 * Handles registration, login, JWT token generation, and user context resolution.
 * </p>
 */
@Slf4j
@ApplicationScoped
@RequiredArgsConstructor
public class AuthService {

    /**
     * Length of the verification code used for password reset (in digits).
     */
    private static final int PASSWORD_LENGTH = 6;

    /**
     * Redis key prefix for password reset codes.
     */
    private static final String RESET_CODE_PREFIX = "forgot-password:";

    /**
     * Redis key prefix for the counter of wrong password reset codes.
     */
    private static final String RESET_FAILURE_PREFIX = "reset_fail:";

    /**
     * Number of wrong reset codes allowed within the attempt window.
     */
    private static final int MAX_RESET_ATTEMPTS = 5;

    /**
     * Length of the attempt window for wrong reset codes, in seconds (15 minutes).
     */
    private static final int RESET_ATTEMPT_WINDOW_SECONDS = 900;

    /**
     * Service for sending emails.
     */
    private final MailService mailService;

    /**
     * Service for managing profiles.
     */
    private final ProfileService profileService;

    /**
     * Service for managing users.
     */
    private final UserService userService;

    /**
     * Redis client for temporary storage (e.g., verification codes).
     */
    private final RedisClient redisClient;

    /** Service for generating signed JWT tokens. */
    private final TokenService tokenService;

    /**
     * Registers a new user (customer or professional) and returns a JWT token.
     *
     * @param email    user email
     * @param password plain text password
     * @param role     user role
     * @return JWT token for the newly created user
     */
    @Transactional
    public String register(final String email, final String password, final UserRole role) {
        log.info("Register: email={}, role={}", email, role);
        if (role == null) {
            log.warn("Register without a role");
            throw new BadRequestException("Unsupported role");
        }
        User created = userService.create(newUser(email, password, role));
        profileService.create(created);
        log.debug("Created user with ID={}, role={}", created.getId(), role);
        return tokenService.generateToken(created);
    }

    private User newUser(final String email, final String password, final UserRole role) {
        User user = new User();
        user.setEmail(email);
        user.setPassword(CryptoUtils.hash(password));
        user.setRole(role);
        return user;
    }

    /**
     * Authenticates a user by verifying email and password and returns a JWT token.
     *
     * @param email    user email
     * @param password plain text password
     * @param role     expected user role
     * @return signed JWT token
     */
    public String login(final String email, final String password, final UserRole role) {
        log.info("Login: email={}, role={}", email, role);
        User user = userService.findByEmail(email).orElseThrow(() -> {
            log.warn("Login failed, unknown email");
            return new InvalidCredentialsException();
        });
        if (!CryptoUtils.verify(password, user.getPassword())) {
            log.warn("Login failed, invalid password");
            throw new InvalidCredentialsException();
        }

        if (user.getRole() != role) {
            log.warn("Login failed, invalid role: expected={}, actual={}", role, user.getRole());
            throw new InvalidCredentialsException();
        }
        return tokenService.generateToken(user);
    }

    /**
     * Initiates the forgot password process by generating and sending a verification code.
     *
     * @param email user's email address
     */
    @Transactional
    public void forgotPassword(final String email) {
        Optional<User> user = userService.findByEmail(email);
        if (user.isEmpty()) {
            log.warn("Password reset requested for an unknown account");
            return;
        }

        String code = CryptoUtils.generateNumericCode(PASSWORD_LENGTH);
        String hashedCode = CryptoUtils.hash(code);

        redisClient.setex(RESET_CODE_PREFIX + normalizeEmail(email), "600", hashedCode);

        String message = "Your password reset code is: " + code + "\nIt is valid for 10 minutes.";

        mailService.send(email, "Reset password", message);
        log.debug("Reset code issued for userId={}", user.get().getId());
    }

    /**
     * Normalizes an email for use in cache keys: trimmed and lower-cased.
     *
     * @param email raw email
     * @return normalized email
     */
    static String normalizeEmail(final String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }

    /**
     * Resets the password using the provided verification code.
     * <p>
     * The code is single-use. After {@value #MAX_RESET_ATTEMPTS} wrong codes within
     * {@value #RESET_ATTEMPT_WINDOW_SECONDS} seconds the code is discarded and further attempts
     * are rejected with 429 until the window expires.
     * </p>
     *
     * @param request reset password request containing email, code, and new password
     * @return new JWT token after successful password reset
     * @throws BadRequestException       if the passwords differ
     * @throws InvalidResetCodeException if the code is invalid or expired
     * @throws TooManyAttemptsException  if there were too many wrong codes
     */
    @Transactional
    public String resetPassword(final ResetPasswordRequest request) {
        if (!request.newPassword().equals(request.confirmPassword())) {
            log.warn("New password and confirmation do not match");
            throw new BadRequestException("Passwords do not match");
        }

        String email = normalizeEmail(request.email());
        String codeKey = RESET_CODE_PREFIX + email;
        String failureKey = RESET_FAILURE_PREFIX + email;
        ensureNotLocked(failureKey);

        Response saved = redisClient.get(codeKey);
        if (saved == null || !CryptoUtils.verify(request.verificationCode(), saved.toString())) {
            registerFailure(codeKey, failureKey);
            log.warn("Invalid or expired password reset code");
            throw new InvalidResetCodeException();
        }
        redisClient.del(List.of(codeKey, failureKey));

        User user = userService.getByEmail(request.email());
        User updatedUser = userService.updatePassword(user.getId(), request.newPassword());

        log.debug("Updated user with ID={}", updatedUser.getId());

        return tokenService.generateToken(updatedUser);
    }

    private void ensureNotLocked(final String failureKey) {
        Response failures = redisClient.get(failureKey);
        if (failures == null || failures.toInteger() < MAX_RESET_ATTEMPTS) {
            return;
        }
        Response ttl = redisClient.ttl(failureKey);
        long retryAfter = ttl != null && ttl.toLong() > 0 ? ttl.toLong() : RESET_ATTEMPT_WINDOW_SECONDS;
        throw new TooManyAttemptsException(retryAfter);
    }

    private void registerFailure(final String codeKey, final String failureKey) {
        long failures = redisClient.incr(failureKey).toLong();
        if (failures == 1) {
            redisClient.expire(failureKey, String.valueOf(RESET_ATTEMPT_WINDOW_SECONDS));
        }
        if (failures >= MAX_RESET_ATTEMPTS) {
            redisClient.del(List.of(codeKey));
        }
    }
}
