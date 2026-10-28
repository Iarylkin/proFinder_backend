package fit.biejk.service;

import fit.biejk.entity.Location;
import fit.biejk.entity.User;
import fit.biejk.repository.UserRepository;
import jakarta.ws.rs.NotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class UserServiceTest {

    private static final long USER_ID = 1L;
    private static final UUID OLD_LOCATION_ID = UUID.randomUUID();
    private static final UUID NEW_LOCATION_ID = UUID.randomUUID();

    private UserRepository userRepository;
    private LocationService locationService;
    private UserService service;
    private User user;
    private Location oldLocation;

    @BeforeEach
    void setUp() {
        userRepository = mock(UserRepository.class);
        locationService = mock(LocationService.class);
        service = new UserService(userRepository, locationService, mock(MailService.class));

        oldLocation = new Location();
        oldLocation.setId(OLD_LOCATION_ID);

        user = new User();
        user.setId(USER_ID);
        user.setFirstName("Anna");
        user.setLastName("Novak");
        user.setPhoneNumber("111111111");
        user.setLocation(oldLocation);

        when(userRepository.findById(USER_ID)).thenReturn(user);
    }

    @Test
    void updateProfileChangesOnlyTheSentFields() {
        User changes = new User();
        changes.setPhoneNumber("222222222");

        User result = service.updateProfile(USER_ID, changes);

        assertSame(user, result);
        assertEquals("222222222", result.getPhoneNumber());
        assertEquals("Anna", result.getFirstName());
        assertEquals("Novak", result.getLastName());
        assertSame(oldLocation, result.getLocation());
    }

    @Test
    void updateProfileResolvesTheNewLocationById() {
        Location requested = new Location();
        requested.setId(NEW_LOCATION_ID);
        Location stored = new Location();
        stored.setId(NEW_LOCATION_ID);
        stored.setName("Brno");
        when(locationService.getById(NEW_LOCATION_ID)).thenReturn(stored);
        User changes = new User();
        changes.setLocation(requested);

        User result = service.updateProfile(USER_ID, changes);

        assertSame(stored, result.getLocation());
    }

    @Test
    void updateProfileOfAMissingUserIsNotFound() {
        User changes = new User();

        assertThrows(NotFoundException.class, () -> service.updateProfile(999L, changes));
    }
}
