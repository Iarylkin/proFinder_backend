package fit.biejk.service;

import fit.biejk.dto.ProfileUpdateRequest;
import fit.biejk.entity.Professional;
import fit.biejk.entity.ServiceOffering;
import fit.biejk.repository.ProfessionalRepository;
import fit.biejk.search.ProfessionalSearchMapper;
import fit.biejk.search.ProfessionalSearchService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ProfessionalServiceTest {

    private static final long PROFESSIONAL_ID = 2L;

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
        professional.setServiceOfferings(List.of());
        when(repository.findById(PROFESSIONAL_ID)).thenReturn(professional);
    }

    @Test
    void updateProfileChangesOnlyTheSentFields() {
        ProfileUpdateRequest request = new ProfileUpdateRequest();
        request.setDescription("New description");

        Professional result = service.updateProfile(PROFESSIONAL_ID, request);

        assertSame(professional, result);
        assertEquals("New description", result.getDescription());
        assertEquals(List.of(), result.getServiceOfferings());
        verify(userService).updateProfile(PROFESSIONAL_ID, request);
        verify(searchService).save(any());
    }

    @Test
    void updateProfileReplacesTheOfferedServices() {
        ServiceOffering plumbing = new ServiceOffering();
        plumbing.setId(1L);
        ProfileUpdateRequest request = new ProfileUpdateRequest();
        request.setServiceOfferings(List.of(plumbing));

        Professional result = service.updateProfile(PROFESSIONAL_ID, request);

        assertEquals(List.of(plumbing), result.getServiceOfferings());
        assertEquals("Old description", result.getDescription());
    }
}
