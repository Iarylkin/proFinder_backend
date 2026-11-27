package io.github.strogolsky.profinder.core.service;

import io.github.strogolsky.profinder.auth.entity.UserRole;
import io.github.strogolsky.profinder.core.entity.CustomerProfile;
import io.github.strogolsky.profinder.core.entity.ProfessionalProfile;
import io.github.strogolsky.profinder.core.entity.Profile;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ProfileFactoryTest {

    private final ProfileFactory factory = new ProfileFactory();

    @Test
    void everyRoleHasAProfile() {
        UUID userId = UUID.randomUUID();
        for (UserRole role : UserRole.values()) {
            Profile profile = factory.create(role, userId);

            assertEquals(userId, profile.getId());
        }
    }

    @Test
    void roleDecidesTheProfileClass() {
        assertInstanceOf(CustomerProfile.class, factory.create(UserRole.CUSTOMER, UUID.randomUUID()));
        assertInstanceOf(ProfessionalProfile.class, factory.create(UserRole.PROFESSIONAL, UUID.randomUUID()));
    }

    @Test
    void missingRoleIsRejected() {
        UUID userId = UUID.randomUUID();

        assertThrows(IllegalArgumentException.class, () -> factory.create(null, userId));
    }
}
