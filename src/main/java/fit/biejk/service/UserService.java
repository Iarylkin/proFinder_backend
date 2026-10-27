package fit.biejk.service;

import fit.biejk.entity.Location;
import fit.biejk.entity.User;
import fit.biejk.repository.UserRepository;
import fit.biejk.utilits.CryptoUtils;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.NotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.Optional;

/**
 * Service class for managing {@link User} entities.
 * <p>
 * Handles shared user operations such as email uniqueness check,
 * updating profile data, and deleting users.
 * </p>
 */
@Slf4j
@RequiredArgsConstructor
@ApplicationScoped
public class UserService {

    /**
     * Repository for accessing user data.
     */
    private final UserRepository userRepository;


    /**
     * Service for managing locations.
     */
    private final LocationService locationService;

    /** Service for sending user notifications via email. */
    private final MailService mailService;

    /**
     * Checks if the given email is unique.
     *
     * @param email email to check
     * @throws IllegalArgumentException if the email is already taken
     */
    public void checkUniqueEmail(final String email) {
        log.info("Check unique email: {}", email);
        if (userRepository.findByEmail(email).isPresent()) {
            log.error("User {} already exists", email);
            throw new IllegalArgumentException("User " + email + " already exists");
        }
    }

    /**
     * Finds a user by their email without failing when the user does not exist.
     *
     * @param email email of the user
     * @return the user, or an empty optional if there is none
     */
    public Optional<User> findByEmail(final String email) {
        return userRepository.findByEmail(email);
    }

    /**
     * Retrieves a user by their email.
     *
     * @param email email of the user
     * @return found user
     * @throws NotFoundException if user is not found
     */
    public User getByEmail(final String email) {
        log.info("Get user by email={}", email);
        Optional<User> user = userRepository.findByEmail(email);
        if (user.isEmpty()) {
            log.error("User {} not found", email);
            throw new NotFoundException("User " + email + " not found");
        }
        log.debug("Found user with ID={}", user.get().getId());
        return user.get();
    }

    /**
     * Retrieves a user by their ID.
     *
     * Logs the operation, throws NotFoundException if the user does not exist.
     *
     * @param id the ID of the user to retrieve
     * @return the found User entity
     * @throws NotFoundException if no user is found with the given ID
     */
    public User getById(final Long id) {
        log.info("Get user by id={}", id);
        User user = userRepository.findById(id);
        if (user == null) {
            log.error("User {} not found", id);
            throw new NotFoundException("User " + id + " not found");
        }
        log.debug("Found user with ID={}", user.getId());
        return user;
    }

    /**
     * Checks if a user exists by their ID.
     *
     * @param id the ID of the user
     * @return true if the user exists, false otherwise
     */
    public boolean existById(final Long id) {
        User user = userRepository.findById(id);
        if (user == null) {
            log.warn("User {} not found", id);
            return false;
        }
        log.info("User {} exists", id);
        return true;
    }

    /**
     * Updates the user data by ID.
     *
     * @param userId   ID of the user
     * @param newUser  updated user data
     * @return updated user
     * @throws NotFoundException if user is not found
     */
    @Transactional
    public User update(final Long userId, final User newUser) {
        log.info("Update user: userId={}, newEmail={}", userId, newUser.getEmail());
        User existingUser = userRepository.findById(userId);
        if (existingUser == null) {
            log.error("User with ID={} not found", userId);
            throw new NotFoundException("User with id " + userId + " not found");
        }
        existingUser.setPhoneNumber(newUser.getPhoneNumber());
        if (newUser.getLocation() != null) {
            Location location = locationService.getById(newUser.getLocation().getId());
            existingUser.setLocation(location);
        }
        existingUser.setFirstName(newUser.getFirstName());
        existingUser.setLastName(newUser.getLastName());
        log.debug("User updated with ID={}", existingUser.getId());
        return existingUser;
    }

    /**
     * Updates user password.
     *
     * @param userId      user ID
     * @param newPassword new plain text password
     * @return updated user
     */
    @Transactional
    public User updatePassword(final Long userId, final String newPassword) {
        User user = getById(userId);
        user.setPassword(CryptoUtils.hash(newPassword));
        return user;
    }

    /**
     * Updates user email.
     *
     * @param userId   user ID
     * @param newEmail new email
     * @return updated user
     */
    @Transactional
    public User updateEmail(final Long userId, final String newEmail) {
        log.info("Update user: userId={}, newEmail={}", userId, newEmail);
        User existingUser = getById(userId);
        existingUser.setEmail(newEmail);
        log.debug("User updated with ID={}", existingUser.getId());
        return existingUser;
    }



    /**
     * Deletes the user by ID.
     *
     * @param userId ID of the user
     * @throws NotFoundException if user is not found
     */
    @Transactional
    public void delete(final Long userId) {
        log.info("Delete user: userId={}", userId);
        User existingUser = userRepository.findById(userId);
        if (existingUser == null) {
            log.error("User with ID={} not found", userId);
            throw new NotFoundException("User with id " + userId + " not found");
        }
        userRepository.delete(existingUser);
        log.debug("User deleted with ID={}", userId);
    }

    /**
     * Changes password after verifying current one.
     *
     * @param userId          user ID
     * @param currentPassword current password
     * @param newPassword     new password
     * @param confirmPassword confirmation of new password
     * @return updated user
     */
    @Transactional
    public User changePassword(final Long userId,
                               final String currentPassword,
                               final String newPassword,
                               final String confirmPassword) {
        User user = getById(userId);
        if (!CryptoUtils.verify(currentPassword, user.getPassword())) {
            throw new IllegalArgumentException("Invalid old password");
        }
        if (!newPassword.equals(confirmPassword)) {
            throw new IllegalArgumentException("Passwords do not match");
        }
        return updatePassword(userId, newPassword);
    }

    /**
     * Changes email after verifying password.
     *
     * @param userId         user ID
     * @param newEmail       new email
     * @param currentPassword current password
     * @return updated user
     */
    @Transactional
    public User changeEmail(final Long userId,
                            final String newEmail,
                            final String currentPassword) {
        User user = getById(userId);
        if (user.getEmail().equalsIgnoreCase(newEmail)) {
            throw new IllegalArgumentException("New email is same as old email");
        }
        if (!CryptoUtils.verify(currentPassword, user.getPassword())) {
            throw new IllegalArgumentException("Invalid password");
        }
        checkUniqueEmail(newEmail);
        String oldEmail = user.getEmail();
        user = updateEmail(userId, newEmail);
        mailService.send(oldEmail, "Email change notification",
                "Your email has been changed to: " + newEmail
                    + "\nIf this wasn’t you, please contact support.");
        return user;
    }
}
