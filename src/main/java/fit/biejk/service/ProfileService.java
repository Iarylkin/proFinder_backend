package fit.biejk.service;

import fit.biejk.dto.ProfileUpdateRequest;
import fit.biejk.entity.User;
import fit.biejk.entity.UserRole;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.ws.rs.ForbiddenException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Service for reading and updating the profile of the current user.
 * <p>
 * A profile is a {@link fit.biejk.entity.Customer} or a {@link fit.biejk.entity.Professional},
 * depending on the role of the user.
 * </p>
 */
@Slf4j
@ApplicationScoped
@RequiredArgsConstructor
public class ProfileService {

    /**
     * Service used to find the role of the user.
     */
    private final UserService userService;

    /**
     * Service for customer profiles.
     */
    private final CustomerService customerService;

    /**
     * Service for professional profiles.
     */
    private final ProfessionalService professionalService;

    /**
     * Returns the profile of a user.
     *
     * @param userId ID of the user
     * @return the customer or professional entity of that user
     * @throws ForbiddenException if the user has no customer or professional profile
     */
    public User get(final Long userId) {
        return switch (roleOf(userId)) {
            case CUSTOMER -> customerService.getById(userId);
            case PROFESSIONAL -> professionalService.getById(userId);
            default -> throw noProfile(userId);
        };
    }

    /**
     * Partially updates the profile of a user.
     *
     * @param userId  ID of the user
     * @param request fields to change
     * @return the updated customer or professional entity
     * @throws ForbiddenException  if the user has no customer or professional profile
     * @throws jakarta.ws.rs.BadRequestException if a customer sends professional-only fields
     */
    public User update(final Long userId, final ProfileUpdateRequest request) {
        return switch (roleOf(userId)) {
            case CUSTOMER -> customerService.updateProfile(userId, request);
            case PROFESSIONAL -> professionalService.updateProfile(userId, request);
            default -> throw noProfile(userId);
        };
    }

    private UserRole roleOf(final Long userId) {
        UserRole role = userService.getById(userId).getRole();
        if (role == null) {
            throw noProfile(userId);
        }
        return role;
    }

    private ForbiddenException noProfile(final Long userId) {
        log.warn("User {} has no customer or professional profile", userId);
        return new ForbiddenException("This account has no profile");
    }
}
