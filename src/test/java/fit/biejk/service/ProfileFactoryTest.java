package fit.biejk.service;

import fit.biejk.entity.CustomerProfile;
import fit.biejk.entity.ProfessionalProfile;
import fit.biejk.entity.Profile;
import fit.biejk.entity.User;
import fit.biejk.entity.UserRole;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ProfileFactoryTest {

    private final ProfileFactory factory = new ProfileFactory();

    @Test
    void everyRoleHasAProfile() {
        User user = new User();
        for (UserRole role : UserRole.values()) {
            Profile profile = factory.create(role, user);

            assertSame(user, profile.getUser());
        }
    }

    @Test
    void roleDecidesTheProfileClass() {
        assertInstanceOf(CustomerProfile.class, factory.create(UserRole.CUSTOMER, new User()));
        assertInstanceOf(ProfessionalProfile.class, factory.create(UserRole.PROFESSIONAL, new User()));
    }

    @Test
    void missingRoleIsRejected() {
        User user = new User();

        assertThrows(IllegalArgumentException.class, () -> factory.create(null, user));
    }
}
