package io.github.strogolsky.profinder.core.service;

import io.github.strogolsky.profinder.core.entity.CustomerProfile;
import io.github.strogolsky.profinder.core.entity.ProfessionalProfile;
import io.github.strogolsky.profinder.core.entity.Review;
import io.github.strogolsky.profinder.core.repository.ReviewRepository;
import jakarta.ws.rs.NotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Reviews of professionals: a saved review changes the rating of the professional, and the reviews are read
 * by page for a professional or for the customer who wrote them.
 */
class ReviewServiceTest {

    private static final UUID PROFESSIONAL_ID = UUID.randomUUID();
    private static final UUID CUSTOMER_ID = UUID.randomUUID();
    private static final int PAGE = 2;
    private static final int SIZE = 10;
    private static final int RATING = 4;

    private ReviewRepository reviewRepository;
    private ProfessionalService professionalService;
    private CustomerService customerService;
    private ReviewService service;

    private ProfessionalProfile professional;
    private CustomerProfile customer;
    private Review review;

    @BeforeEach
    void setUp() {
        reviewRepository = mock(ReviewRepository.class);
        professionalService = mock(ProfessionalService.class);
        customerService = mock(CustomerService.class);
        service = new ReviewService(reviewRepository, professionalService, customerService);

        professional = new ProfessionalProfile();
        professional.setId(PROFESSIONAL_ID);
        customer = new CustomerProfile();
        customer.setId(CUSTOMER_ID);
        review = new Review();
        review.setProfessional(professional);
        review.setCustomer(customer);
        review.setRating(RATING);
        review.setComment("Good work");

        when(professionalService.getById(PROFESSIONAL_ID)).thenReturn(professional);
        when(customerService.getById(CUSTOMER_ID)).thenReturn(customer);
    }

    @Test
    void reviewIsSavedAndReturned() {
        Review result = service.create(review);

        assertSame(review, result);
        verify(reviewRepository).persist(review);
    }

    @Test
    void ratingsOfAProfessionalAreReadFromTheRepository() {
        when(reviewRepository.findRatingsByProfessionalId(PROFESSIONAL_ID)).thenReturn(List.of(5, 3));

        assertEquals(List.of(5, 3), service.getRatingsByProfessionalId(PROFESSIONAL_ID));
    }

    @Test
    void reviewsOfAProfessionalAreReadByPage() {
        when(reviewRepository.findByProfessional(professional, PAGE, SIZE)).thenReturn(List.of(review));

        assertEquals(List.of(review), service.getByProfessionalId(PROFESSIONAL_ID, PAGE, SIZE));
    }

    @Test
    void reviewsOfAMissingProfessionalAreNotFound() {
        UUID missing = UUID.randomUUID();
        when(professionalService.getById(missing)).thenThrow(new NotFoundException("no professional"));

        assertThrows(NotFoundException.class, () -> service.getByProfessionalId(missing, PAGE, SIZE));

        verify(reviewRepository, never())
                .findByProfessional(any(ProfessionalProfile.class), anyInt(), anyInt());
    }

    @Test
    void reviewsWrittenByACustomerAreReadByPage() {
        when(reviewRepository.findByCustomer(customer, PAGE, SIZE)).thenReturn(List.of(review));

        assertEquals(List.of(review), service.getByCustomerId(CUSTOMER_ID, PAGE, SIZE));
    }

    @Test
    void reviewsOfAMissingCustomerAreNotFound() {
        UUID missing = UUID.randomUUID();
        when(customerService.getById(missing)).thenThrow(new NotFoundException("no customer"));

        assertThrows(NotFoundException.class, () -> service.getByCustomerId(missing, PAGE, SIZE));

        verify(reviewRepository, never())
                .findByCustomer(any(CustomerProfile.class), anyInt(), anyInt());
    }
}
