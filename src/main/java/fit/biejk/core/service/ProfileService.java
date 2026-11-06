package fit.biejk.core.service;

import fit.biejk.auth.entity.User;
import fit.biejk.common.validation.Text;
import fit.biejk.core.entity.Profile;
import fit.biejk.core.repository.ProfileRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Event;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.NotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.UUID;

/**
 * Service for the profiles of the users.
 */
@Slf4j
@ApplicationScoped
@RequiredArgsConstructor
public class ProfileService {

    /**
     * Repository for the profiles.
     */
    private final ProfileRepository profileRepository;

    /**
     * Factory that picks the profile class of a role.
     */
    private final ProfileFactory profileFactory;

    /**
     * Service for the locations.
     */
    private final LocationService locationService;

    /**
     * Announces a new profile, so that the parts interested in its type (for example the search index)
     * can react to it.
     */
    private final Event<Profile> profileCreated;

    /**
     * Creates the profile of a user. The type of the profile is the one of the role of the user.
     *
     * @param user the user the profile belongs to
     * @return the created profile
     */
    @Transactional
    public Profile create(final User user) {
        log.info("Create profile: userId={}, role={}", user.getId(), user.getRole());
        Profile profile = profileFactory.create(user.getRole(), user);
        profile.setLocation(locationService.getDefault());
        profileRepository.persist(profile);
        profileCreated.fire(profile);
        log.debug("Profile created with ID={}", profile.getId());
        return profile;
    }

    /**
     * Gets the profile of a user. The type of the profile is the one of the role of the user.
     *
     * @param userId ID of the user
     * @return the profile
     * @throws NotFoundException if the user has no profile
     */
    public Profile get(final UUID userId) {
        log.info("Get profile: userId={}", userId);
        Profile profile = profileRepository.findById(userId);
        if (profile == null) {
            log.error("Profile of user {} not found", userId);
            throw new NotFoundException("Profile of user " + userId + " not found");
        }
        return profile;
    }

    /**
     * Applies the personal data of {@code changes} (name, phone, location) to a profile; only the fields that
     * are not {@code null} change. Meant to be called inside the transaction of the caller.
     *
     * @param profile the profile to change
     * @param changes a profile holding only the requested changes; a phone that is the empty text clears the
     *                field (the names are required, the request validation rejects an empty one)
     */
    public void updatePersonalData(final Profile profile, final Profile changes) {
        if (changes.getFirstName() != null) {
            profile.setFirstName(changes.getFirstName());
        }
        if (changes.getLastName() != null) {
            profile.setLastName(changes.getLastName());
        }
        if (changes.getPhoneNumber() != null) {
            profile.setPhoneNumber(Text.emptyToNull(changes.getPhoneNumber()));
        }
        if (changes.getLocation() != null) {
            profile.setLocation(locationService.getById(changes.getLocation().getId()));
        }
    }
}
