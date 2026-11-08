package io.github.strogolsky.profinder.core.service;

import io.github.strogolsky.profinder.auth.entity.User;
import io.github.strogolsky.profinder.auth.entity.UserRole;
import io.github.strogolsky.profinder.core.entity.CustomerProfile;
import io.github.strogolsky.profinder.core.entity.ProfessionalProfile;
import io.github.strogolsky.profinder.core.entity.Profile;
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
