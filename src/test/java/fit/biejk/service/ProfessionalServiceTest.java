package fit.biejk.service;

import fit.biejk.entity.ProfessionalProfile;
import fit.biejk.entity.Category;
import fit.biejk.exception.StaleVersionException;
import fit.biejk.repository.ProfessionalRepository;
import fit.biejk.search.ProfessionalSearchMapper;
import fit.biejk.search.ProfessionalSearchService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ProfessionalServiceTest {

    private static final UUID PROFESSIONAL_ID = UUID.randomUUID();

    private static final long VERSION = 4L;

    private ProfessionalSearchService searchService;
    private UserService userService;
    private ProfessionalService service;
    private ProfileService profileService;
    private ProfessionalProfile professional;

    @BeforeEach
    void setUp() {
        ProfessionalRepository repository = mock(ProfessionalRepository.class);
        profileService = mock(ProfileService.class);
        userService = mock(UserService.class);
        searchService = mock(ProfessionalSearchService.class);
        service = new ProfessionalService(
                repository,
                userService,
                searchService,
                mock(ProfessionalSearchMapper.class),
                mock(ReviewService.class),
                mock(AuthService.class),
                mock(CustomerService.class),
                profileService);

        professional = new ProfessionalProfile();
        professional.setId(PROFESSIONAL_ID);
        professional.setDescription("Old description");
        professional.setCategories(List.of());
        professional.setVersion(VERSION);
        when(repository.findById(PROFESSIONAL_ID)).thenReturn(professional);
    }

    private static ProfessionalProfile changes() {
        ProfessionalProfile changes = new ProfessionalProfile();
        changes.setVersion(VERSION);
        return changes;
    }

    @Test
    void updateProfileWithAStaleVersionIsRejectedAndChangesNothing() {
        ProfessionalProfile changes = changes();
        changes.setVersion(VERSION + 1);
        changes.setDescription("New description");

        assertThrows(StaleVersionException.class, () -> service.updateProfile(PROFESSIONAL_ID, changes));

        assertEquals("Old description", professional.getDescription());
        verify(profileService, never()).updatePersonalData(any(), any());
        verify(searchService, never()).save(any());
    }

    @Test
    void updateProfileChangesOnlyTheSentFields() {
        ProfessionalProfile changes = changes();
        changes.setDescription("New description");

        ProfessionalProfile result = service.updateProfile(PROFESSIONAL_ID, changes);

        assertSame(professional, result);
        assertEquals("New description", result.getDescription());
        assertEquals(List.of(), result.getCategories());
        verify(profileService).updatePersonalData(professional, changes);
        verify(searchService).save(any());
    }

    @Test
    void updateProfileReplacesTheOfferedServices() {
        Category plumbing = new Category();
        plumbing.setId(UUID.randomUUID());
        ProfessionalProfile changes = changes();
        changes.setCategories(List.of(plumbing));

        ProfessionalProfile result = service.updateProfile(PROFESSIONAL_ID, changes);

        assertEquals(List.of(plumbing), result.getCategories());
        assertEquals("Old description", result.getDescription());
    }
}
