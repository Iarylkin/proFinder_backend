package fit.biejk.core.service;

import fit.biejk.auth.entity.User;
import fit.biejk.auth.entity.UserRole;
import fit.biejk.core.entity.CustomerProfile;
import fit.biejk.core.entity.ProfessionalProfile;
import fit.biejk.core.entity.Profile;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.EnumMap;
import java.util.Map;
import java.util.function.Supplier;

/**
 * Creates the profile that matches a role.
 * <p>
 * A new role is one more line in {@link #CONSTRUCTORS}; nothing else needs to know which profile class a role has.
 * </p>
 */
@ApplicationScoped
public class ProfileFactory {

    /**
     * Constructor of the profile class of each role.
     */
    private static final Map<UserRole, Supplier<Profile>> CONSTRUCTORS = new EnumMap<>(Map.of(
            UserRole.CUSTOMER, CustomerProfile::new,
            UserRole.PROFESSIONAL, ProfessionalProfile::new));

    /**
     * Creates an empty profile of the type of the role and links it to the user.
     *
     * @param role the role of the user
     * @param user the user the profile belongs to
     * @return a new profile
     * @throws IllegalArgumentException if the role has no profile class
     */
    public Profile create(final UserRole role, final User user) {
        Supplier<Profile> constructor = CONSTRUCTORS.get(role);
        if (constructor == null) {
            throw new IllegalArgumentException("No profile for role " + role);
        }
        Profile profile = constructor.get();
        profile.setUser(user);
        return profile;
    }
}
