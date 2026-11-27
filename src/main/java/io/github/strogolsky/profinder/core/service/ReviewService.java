package io.github.strogolsky.profinder.core.service;

import io.github.strogolsky.profinder.core.entity.CustomerProfile;
import io.github.strogolsky.profinder.core.entity.ProfessionalProfile;
import io.github.strogolsky.profinder.core.entity.Review;
import io.github.strogolsky.profinder.core.repository.ReviewRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.UUID;

/**
 * Service for managing {@link Review} entities.
 * <p>
 * Provides functionality for creating reviews and retrieving them by customer or professional, and the
 * ratings of the reviews of a professional. The rating of the professional itself is calculated by
 * {@link ProfessionalService}.
 * </p>
 */
@Slf4j
@RequiredArgsConstructor
@ApplicationScoped
public class ReviewService {

    /** Repository for accessing persisted reviews. */
    private final ReviewRepository reviewRepository;

    /** Service for retrieving and updating professionals. */
    private final ProfessionalService professionalService;

    /** Service for retrieving customers. */
    private final CustomerService customerService;

    /**
     * Persists a new review. The rating of the professional is recalculated by the caller.
     *
     * @param review the review to be saved
     * @return the persisted {@link Review}
     */
    @Transactional
    public Review create(final Review review) {
        log.info("Creating review: professionalId={}, customerId={}, rating={}",
                review.getProfessional().getId(),
                review.getCustomer().getId(),
                review.getRating());

        reviewRepository.persist(review);
        log.debug("Review persisted: reviewId={}", review.getId());

        return review;
    }

    /**
     * Reads the ratings of all reviews of a professional, including a review saved a moment ago.
     *
     * @param professionalId ID of the professional
     * @return the ratings, one per review
     */
    public List<Integer> getRatingsByProfessionalId(final UUID professionalId) {
        return reviewRepository.findRatingsByProfessionalId(professionalId);
    }

    /**
     * Retrieves all reviews written for a specific professional.
     *
     * @param professionalId the professional's ID
     * @param page         page number for pagination
     * @param size         number of reviews per page
     * @return list of reviews for the professional
     */
    public List<Review> getByProfessionalId(final UUID professionalId, final int page, final int size) {
        log.info("Fetching reviews for professionalId={}", professionalId);
        ProfessionalProfile professional = professionalService.getById(professionalId);
        List<Review> reviews = reviewRepository.findByProfessional(professional, page, size);
        log.debug("Found {} reviews for professionalId={}", reviews.size(), professionalId);
        return reviews;
    }

    /**
     * Retrieves all reviews written by a specific customer.
     *
     * @param customerId the customer's ID
     * @param page     page number for pagination
     * @param size     number of reviews per page
     * @return list of reviews created by the customer
     */
    public List<Review> getByCustomerId(final UUID customerId, final int page, final int size) {
        log.info("Fetching reviews by customerId={}", customerId);
        CustomerProfile customer = customerService.getById(customerId);
        List<Review> reviews = reviewRepository.findByCustomer(customer, page, size);
        log.debug("Found {} reviews by customerId={}", reviews.size(), customerId);
        return reviews;
    }

}
