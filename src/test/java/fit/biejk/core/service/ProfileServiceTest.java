package fit.biejk.core.service;

import fit.biejk.auth.entity.User;
import fit.biejk.auth.entity.UserRole;
import fit.biejk.core.entity.CustomerProfile;
import fit.biejk.core.entity.Location;
import fit.biejk.core.entity.ProfessionalProfile;
import fit.biejk.core.entity.Profile;
import fit.biejk.core.repository.ProfileRepository;
import jakarta.enterprise.event.Event;
import jakarta.ws.rs.NotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ProfileServiceTest {

    private ProfileRepository repository;
    private Event<Profile> profileCreated;
    private LocationService locationService;
    private ProfileService service;

    @SuppressWarnings("unchecked")
    @BeforeEach
    void setUp() {
        repository = mock(ProfileRepository.class);
        profileCreated = mock(Event.class);
        locationService = mock(LocationService.class);
        service = new ProfileService(repository, new ProfileFactory(), locationService, profileCreated);
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

    @Test
    void newProfileGetsTheDefaultLocation() {
        Location prague = new Location();
        when(locationService.getDefault()).thenReturn(prague);

        Profile profile = service.create(userWithRole(UserRole.CUSTOMER));

        assertSame(prague, profile.getLocation());
    }

    @Test
    void personalDataChangesOnlyTheSentFields() {
        Location oldLocation = new Location();
        CustomerProfile profile = new CustomerProfile();
        profile.setFirstName("Anna");
        profile.setLastName("Novak");
        profile.setPhoneNumber("111111111");
        profile.setLocation(oldLocation);
        CustomerProfile changes = new CustomerProfile();
        changes.setPhoneNumber("222222222");

        service.updatePersonalData(profile, changes);

        assertEquals("222222222", profile.getPhoneNumber());
        assertEquals("Anna", profile.getFirstName());
        assertEquals("Novak", profile.getLastName());
        assertSame(oldLocation, profile.getLocation());
    }

    @Test
    void anEmptyTextClearsThePhone() {
        CustomerProfile profile = new CustomerProfile();
        profile.setFirstName("Anna");
        profile.setLastName("Novak");
        profile.setPhoneNumber("111111111");
        CustomerProfile changes = new CustomerProfile();
        changes.setPhoneNumber("");

        service.updatePersonalData(profile, changes);

        assertNull(profile.getPhoneNumber());
        assertEquals("Anna", profile.getFirstName());
        assertEquals("Novak", profile.getLastName());
    }

    @Test
    void personalDataResolvesTheNewLocationById() {
        UUID newLocationId = UUID.randomUUID();
        Location requested = new Location();
        requested.setId(newLocationId);
        Location stored = new Location();
        stored.setId(newLocationId);
        when(locationService.getById(newLocationId)).thenReturn(stored);
        CustomerProfile profile = new CustomerProfile();
        CustomerProfile changes = new CustomerProfile();
        changes.setLocation(requested);

        service.updatePersonalData(profile, changes);

        assertSame(stored, profile.getLocation());
    }

    @Test
    void getOfAMissingProfileIsNotFound() {
        UUID missing = UUID.randomUUID();

        assertThrows(NotFoundException.class, () -> service.get(missing));
    }

    private static User userWithRole(final UserRole role) {
        User user = new User();
        user.setRole(role);
        return user;
    }
}
