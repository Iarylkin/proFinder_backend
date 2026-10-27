package fit.biejk.service;

import fit.biejk.dto.ResetPasswordRequest;
import fit.biejk.entity.Client;
import fit.biejk.entity.Specialist;
import fit.biejk.entity.User;
import fit.biejk.entity.UserRole;
import fit.biejk.utilits.CryptoUtils;
import io.quarkus.redis.client.RedisClient;
import io.quarkus.security.identity.SecurityIdentity;
import io.vertx.redis.client.Response;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.BadRequestException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.Locale;
import java.util.Optional;

/**
 * Service responsible for authentication logic.
 * <p>
 * Handles sign-up, sign-in, JWT token generation, and user context resolution.
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
     * Service for sending emails.
     */
    private final MailService mailService;

    /**
     * Service for managing locations.
     */
    private final LocationService locationService;

    /**
     * Service for managing specialists.
     */
    private final SpecialistService specialistService;

    /**
     * Service for managing clients.
     */
    private final ClientService clientService;

    /**
     * Service for managing users.
     */
    private final UserService userService;

    /**
     * Quarkus security context used to get the current authenticated identity.
     */
    private final SecurityIdentity securityIdentity;

    /**
     * Redis client for temporary storage (e.g., verification codes).
     */
    private final RedisClient redisClient;

    /** Service for generating signed JWT tokens. */
    private final TokenService tokenService;

    /**
     * Registers a new user (client or specialist) and returns a JWT token.
     *
     * @param email    user email
     * @param password plain text password
     * @param role     user role
     * @return JWT token for the newly created user
     */
    public String signUp(final String email, final String password, final UserRole role) {
        log.info("Sign up: email={}, role={}", email, role);
        User created = switch (role) {
            case SPECIALIST -> specialistService.create(newUser(new Specialist(), email, password, role));
            case CLIENT -> clientService.create(newUser(new Client(), email, password, role));
            default -> {
                log.warn("Sign up with unsupported role={}", role);
                throw new BadRequestException("Unsupported role");
            }
        };
        log.debug("Created user with ID={}, role={}", created.getId(), role);
        return tokenService.generateToken(created);
    }

    private <T extends User> T newUser(final T user, final String email, final String password,
                                       final UserRole role) {
        user.setEmail(email);
        user.setPassword(CryptoUtils.hash(password));
        user.setRole(role);
        user.setLocation(locationService.getDefault());
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
    public String signIn(final String email, final String password, final UserRole role) {
        log.info("Sign in: email={}, role={}", email, role);
        User user = userService.getByEmail(email);
        if (!CryptoUtils.verify(password, user.getPassword())) {
            log.error("Invalid password");
            throw new IllegalArgumentException("Invalid password");
        }

        if (user.getRole() != role) {
            log.error("Invalid role: expected={}, actual={}", role, user.getRole());
            throw new IllegalArgumentException("Invalid role");
        }
        return tokenService.generateToken(user);
    }

    /**
     * Returns the ID of the currently authenticated user.
     *
     * @return user ID as Long
     */
    public Long getCurrentUserId() {
        log.debug("Current principal name={}", securityIdentity.getPrincipal().getName());
        return Long.parseLong(securityIdentity.getPrincipal().getName());
    }

    /**
     * Checks if the specified user ID matches the currently authenticated user.
     *
     * @param userId user ID to check
     * @return true if matches, false otherwise
     */
    public boolean isCurrentUser(final Long userId) {
        Long currentUserId = getCurrentUserId();
        log.debug("Comparing user IDs: current={}, requested={}", currentUserId, userId);
        return currentUserId.equals(userId);
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
     *
     * @param request reset password request containing email, code, and new password
     * @return new JWT token after successful password reset
     * @throws IllegalArgumentException if validation fails
     */
    @Transactional
    public String resetPassword(final ResetPasswordRequest request) {
        if (!request.getNewPassword().equals(request.getConfirmPassword())) {
            log.error("New password and confirmation do not match");
            throw new IllegalArgumentException("Invalid new password");
        }

        String key = RESET_CODE_PREFIX + normalizeEmail(request.getEmail());
        Response response = redisClient.get(key);
        String savedHashedCode = response != null ? response.toString() : null;

        if (!CryptoUtils.verify(request.getVerificationCode(), savedHashedCode)) {
            log.error("Invalid verification code");
            throw new IllegalArgumentException("Invalid verification code");
        }

        User user = userService.getByEmail(request.getEmail());
        User updatedUser = userService.updatePassword(user.getId(), request.getNewPassword());

        log.debug("Updated user with ID={}", updatedUser.getId());

        return tokenService.generateToken(updatedUser);
    }
}
