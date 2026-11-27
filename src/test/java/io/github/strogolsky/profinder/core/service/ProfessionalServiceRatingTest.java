package io.github.strogolsky.profinder.core.service;

import io.github.strogolsky.profinder.auth.service.UserService;
import io.github.strogolsky.profinder.common.security.UserContext;
import io.github.strogolsky.profinder.core.entity.CustomerProfile;
import io.github.strogolsky.profinder.core.entity.ProfessionalProfile;
import io.github.strogolsky.profinder.core.entity.Review;
import io.github.strogolsky.profinder.core.repository.ProfessionalRepository;
import io.github.strogolsky.profinder.core.search.ProfessionalSearchDto;
import io.github.strogolsky.profinder.core.search.ProfessionalSearchMapper;
import io.github.strogolsky.profinder.core.search.ProfessionalSearchService;
import jakarta.ws.rs.NotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InOrder;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Posting a review through the professional, and the average rating that follows from the reviews:
 * the rating of a professional is the mean of the ratings of all their reviews.
 */
class ProfessionalServiceRatingTest {

    private static final UUID PROFESSIONAL_ID = UUID.randomUUID();
    private static final UUID CUSTOMER_ID = UUID.randomUUID();
    private static final double DELTA = 0.0001;

    private ProfessionalRepository professionalRepository;
    private ProfessionalSearchService searchService;
    private ProfessionalSearchMapper searchMapper;
    private ReviewService reviewService;
    private UserContext userContext;
    private CustomerService customerService;
    private ProfessionalService service;

    private ProfessionalProfile professional;
    private CustomerProfile customer;

    @BeforeEach
    void setUp() {
        professionalRepository = mock(ProfessionalRepository.class);
        searchService = mock(ProfessionalSearchService.class);
        searchMapper = mock(ProfessionalSearchMapper.class);
        reviewService = mock(ReviewService.class);
        userContext = mock(UserContext.class);
        customerService = mock(CustomerService.class);
        service = new ProfessionalService(professionalRepository, mock(UserService.class), searchService,
                searchMapper, reviewService, userContext, customerService, mock(ProfileService.class));

        professional = new ProfessionalProfile();
        professional.setId(PROFESSIONAL_ID);
        professional.setReviews(new ArrayList<>());
        customer = new CustomerProfile();
        customer.setId(CUSTOMER_ID);

        when(professionalRepository.findById(PROFESSIONAL_ID)).thenReturn(professional);
        when(userContext.id()).thenReturn(CUSTOMER_ID);
        when(customerService.getById(CUSTOMER_ID)).thenReturn(customer);
    }

    private static Review reviewWithRating(final int rating) {
        Review review = new Review();
        review.setRating(rating);
        return review;
    }

    @Test
    void reviewIsWrittenByTheCurrentCustomerForTheProfessionalFromThePath() {
        Review posted = reviewWithRating(5);
        when(reviewService.create(posted)).thenReturn(posted);

        Review result = service.review(PROFESSIONAL_ID, posted);

        assertSame(posted, result);
        assertSame(customer, posted.getCustomer());
        assertSame(professional, posted.getProfessional());
        verify(reviewService).create(posted);
    }

    @Test
    void reviewOfAMissingProfessionalIsNotFoundAndNothingIsSaved() {
        assertThrows(NotFoundException.class, () -> service.review(UUID.randomUUID(), reviewWithRating(5)));

        verify(reviewService, never()).create(any(Review.class));
    }

    private Review postReview() {
        Review posted = reviewWithRating(5);
        when(reviewService.create(posted)).thenReturn(posted);
        return service.review(PROFESSIONAL_ID, posted);
    }

    @Test
    void ratingIsTheMeanOfAllRatingsAndIsReadAfterTheReviewIsSaved() {
        when(reviewService.getRatingsByProfessionalId(PROFESSIONAL_ID)).thenReturn(List.of(5, 4, 3));

        Review posted = postReview();

        assertEquals(4.0, professional.getRating(), DELTA);
        InOrder order = inOrder(reviewService);
        order.verify(reviewService).create(posted);
        order.verify(reviewService).getRatingsByProfessionalId(PROFESSIONAL_ID);
    }

    @Test
    void ratingOfASingleReviewIsItsRating() {
        when(reviewService.getRatingsByProfessionalId(PROFESSIONAL_ID)).thenReturn(List.of(2));

        postReview();

        assertEquals(2.0, professional.getRating(), DELTA);
    }

    @Test
    void ratingWithoutAnyRatingIsZero() {
        when(reviewService.getRatingsByProfessionalId(PROFESSIONAL_ID)).thenReturn(List.of());

        postReview();

        assertEquals(0.0, professional.getRating(), DELTA);
    }

    @Test
    void newRatingIsSavedAndSentToTheSearchIndex() {
        ProfessionalSearchDto document = new ProfessionalSearchDto();
        when(searchMapper.toDto(professional)).thenReturn(document);
        when(reviewService.getRatingsByProfessionalId(PROFESSIONAL_ID)).thenReturn(List.of(5, 4));

        postReview();

        assertEquals(4.5, professional.getRating(), DELTA);
        verify(professionalRepository).persist(professional);
        verify(searchService).save(document);
    }

    @Test
    void reviewOfAMissingProfessionalChangesNoRating() {
        assertThrows(NotFoundException.class, () -> service.review(UUID.randomUUID(), reviewWithRating(5)));

        verify(reviewService, never()).getRatingsByProfessionalId(any(UUID.class));
        verify(searchService, never()).save(any());
    }
}
