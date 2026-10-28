package fit.biejk.service;

import fit.biejk.entity.Professional;
import fit.biejk.entity.Category;
import fit.biejk.repository.ProfessionalRepository;
import fit.biejk.search.ProfessionalSearchMapper;
import fit.biejk.search.ProfessionalSearchService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ProfessionalServiceTest {

    private static final UUID PROFESSIONAL_ID = UUID.randomUUID();

    private ProfessionalSearchService searchService;
    private UserService userService;
    private ProfessionalService service;
    private Professional professional;

    @BeforeEach
    void setUp() {
        ProfessionalRepository repository = mock(ProfessionalRepository.class);
        userService = mock(UserService.class);
        searchService = mock(ProfessionalSearchService.class);
        service = new ProfessionalService(
                repository,
                userService,
                searchService,
                mock(ProfessionalSearchMapper.class),
                mock(ReviewService.class),
                mock(AuthService.class),
                mock(CustomerService.class));

        professional = new Professional();
        professional.setId(PROFESSIONAL_ID);
        professional.setDescription("Old description");
        professional.setCategories(List.of());
        when(repository.findById(PROFESSIONAL_ID)).thenReturn(professional);
    }

    @Test
    void updateProfileChangesOnlyTheSentFields() {
        Professional changes = new Professional();
        changes.setDescription("New description");

        Professional result = service.updateProfile(PROFESSIONAL_ID, changes);

        assertSame(professional, result);
        assertEquals("New description", result.getDescription());
        assertEquals(List.of(), result.getCategories());
        verify(userService).updateProfile(PROFESSIONAL_ID, changes);
        verify(searchService).save(any());
    }

    @Test
    void updateProfileReplacesTheOfferedServices() {
        Category plumbing = new Category();
        plumbing.setId(UUID.randomUUID());
        Professional changes = new Professional();
        changes.setCategories(List.of(plumbing));

        Professional result = service.updateProfile(PROFESSIONAL_ID, changes);

        assertEquals(List.of(plumbing), result.getCategories());
        assertEquals("Old description", result.getDescription());
    }
}
