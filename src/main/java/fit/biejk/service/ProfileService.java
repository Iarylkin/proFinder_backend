package fit.biejk.service;

import fit.biejk.entity.Profile;
import fit.biejk.entity.User;
import fit.biejk.repository.ProfileRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Event;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

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
        profileRepository.persist(profile);
        profileCreated.fire(profile);
        log.debug("Profile created with ID={}", profile.getId());
        return profile;
    }
}
