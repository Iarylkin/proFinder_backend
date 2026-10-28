package fit.biejk.service;

import fit.biejk.entity.CustomerProfile;
import fit.biejk.entity.ProfessionalProfile;
import fit.biejk.entity.Profile;
import fit.biejk.entity.User;
import fit.biejk.entity.UserRole;
import fit.biejk.repository.ProfileRepository;
import jakarta.enterprise.event.Event;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class ProfileServiceTest {

    private ProfileRepository repository;
    private Event<Profile> profileCreated;
    private ProfileService service;

    @SuppressWarnings("unchecked")
    @BeforeEach
    void setUp() {
        repository = mock(ProfileRepository.class);
        profileCreated = mock(Event.class);
        service = new ProfileService(repository, new ProfileFactory(), profileCreated);
    }

    @Test
    void customerGetsACustomerProfile() {
        User user = userWithRole(UserRole.CUSTOMER);

        Profile profile = service.create(user);

        assertInstanceOf(CustomerProfile.class, profile);
        assertSame(user, profile.getUser());
        verify(repository).persist(profile);
        verify(profileCreated).fire(profile);
    }

    @Test
    void professionalGetsAProfessionalProfile() {
        User user = userWithRole(UserRole.PROFESSIONAL);

        Profile profile = service.create(user);

        assertInstanceOf(ProfessionalProfile.class, profile);
        assertSame(user, profile.getUser());
    }

    private static User userWithRole(final UserRole role) {
        User user = new User();
        user.setRole(role);
        return user;
    }
}
