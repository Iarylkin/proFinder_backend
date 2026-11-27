package io.github.strogolsky.profinder.core.service;

import io.github.strogolsky.profinder.auth.service.UserService;
import io.github.strogolsky.profinder.common.security.UserContext;
import io.github.strogolsky.profinder.core.entity.Category;
import io.github.strogolsky.profinder.core.entity.ProfessionalProfile;
import io.github.strogolsky.profinder.core.repository.ProfessionalRepository;
import io.github.strogolsky.profinder.core.search.ProfessionalSearchDto;
import io.github.strogolsky.profinder.core.search.ProfessionalSearchMapper;
import io.github.strogolsky.profinder.core.search.ProfessionalSearchService;
import io.quarkus.hibernate.orm.panache.PanacheQuery;
import jakarta.ws.rs.NotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InOrder;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Reading, indexing, deleting and updating a professional. The partial update by version is in
 * {@link ProfessionalServiceTest}, the reviews and the rating in {@link ProfessionalServiceRatingTest}.
 */
class ProfessionalServiceCrudTest {

    private static final UUID PROFESSIONAL_ID = UUID.randomUUID();
    private static final long VERSION = 4L;
    private static final int PAGE = 2;
    private static final int SIZE = 10;

    private ProfessionalRepository repository;
    private UserService userService;
    private ProfessionalSearchService searchService;
    private ProfessionalSearchMapper searchMapper;
    private ProfileService profileService;
    private ProfessionalService service;

    private ProfessionalProfile professional;
    private ProfessionalSearchDto document;

    @BeforeEach
    void setUp() {
        repository = mock(ProfessionalRepository.class);
        userService = mock(UserService.class);
        searchService = mock(ProfessionalSearchService.class);
        searchMapper = mock(ProfessionalSearchMapper.class);
        profileService = mock(ProfileService.class);
        service = new ProfessionalService(repository, userService, searchService, searchMapper,
                mock(ReviewService.class), mock(UserContext.class), mock(CustomerService.class), profileService,
                PassThroughCategories.service());

        professional = new ProfessionalProfile();
        professional.setId(PROFESSIONAL_ID);
        professional.setVersion(VERSION);
        professional.setCategories(List.of());
        document = new ProfessionalSearchDto();

        when(repository.findById(PROFESSIONAL_ID)).thenReturn(professional);
        when(searchMapper.toDto(professional)).thenReturn(document);
    }

    @Test
    void newProfessionalProfileIsIndexedForSearch() {
        service.indexCreated(professional);

        verify(searchService).save(document);
    }

    @Test
    void professionalIsFoundById() {
        assertSame(professional, service.getById(PROFESSIONAL_ID));
    }

    @Test
    void missingProfessionalIsNotFound() {
        assertThrows(NotFoundException.class, () -> service.getById(UUID.randomUUID()));
    }

    @SuppressWarnings("unchecked")
    @Test
    void allProfessionalsAreReadByPageWhichStartsAtOne() {
        PanacheQuery<ProfessionalProfile> query = mock(PanacheQuery.class);
        when(repository.findAll()).thenReturn(query);
        when(query.page(PAGE - 1, SIZE)).thenReturn(query);
        when(query.list()).thenReturn(List.of(professional));

        assertEquals(List.of(professional), service.getAll(PAGE, SIZE));
    }

    @Test
    void deletedProfessionalLeavesTheSearchIndexAfterTheAccountIsDeleted() {
        service.delete(PROFESSIONAL_ID);

        InOrder order = inOrder(userService, searchService);
        order.verify(userService).delete(PROFESSIONAL_ID);
        order.verify(searchService).delete(PROFESSIONAL_ID);
    }

    @Test
    void professionalOfAMissingAccountStaysInTheIndexUntouched() {
        doThrow(new NotFoundException("no user")).when(userService).delete(PROFESSIONAL_ID);

        assertThrows(NotFoundException.class, () -> service.delete(PROFESSIONAL_ID));

        verify(searchService, never()).delete(any(UUID.class));
    }

    @Test
    void updatedProfileIsSentToTheSearchIndex() {
        ProfessionalProfile changes = new ProfessionalProfile();
        changes.setVersion(VERSION);
        changes.setDescription("Plumber");

        ProfessionalProfile result = service.updateProfile(PROFESSIONAL_ID, changes);

        assertSame(professional, result);
        assertEquals("Plumber", professional.getDescription());
        verify(profileService).updatePersonalData(professional, changes);
        verify(searchService).save(document);
    }

    @Test
    void updateKeepsTheCategoriesWhenNoneAreSent() {
        Category plumbing = new Category();
        professional.setCategories(List.of(plumbing));
        ProfessionalProfile changes = new ProfessionalProfile();
        changes.setVersion(VERSION);

        service.updateProfile(PROFESSIONAL_ID, changes);

        assertEquals(List.of(plumbing), professional.getCategories());
    }

    @Test
    void updateOfAMissingProfessionalIsNotFoundAndIndexesNothing() {
        ProfessionalProfile changes = new ProfessionalProfile();
        changes.setVersion(VERSION);

        assertThrows(NotFoundException.class, () -> service.updateProfile(UUID.randomUUID(), changes));

        verify(searchService, never()).save(any(ProfessionalSearchDto.class));
    }
}
