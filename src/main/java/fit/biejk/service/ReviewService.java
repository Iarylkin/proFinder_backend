package fit.biejk.service;

import fit.biejk.entity.Customer;
import fit.biejk.entity.Review;
import fit.biejk.entity.Specialist;
import fit.biejk.repository.ReviewRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;

import java.util.List;

/**
 * Service for managing {@link Review} entities.
 * <p>
 * Provides functionality for creating reviews and retrieving them by customer, specialist, or order.
 * Also updates the average rating of specialists upon new review creation.
 * </p>
 */
@Slf4j
@ApplicationScoped
public class ReviewService {

    /** Repository for accessing persisted reviews. */
    @Inject
    private ReviewRepository reviewRepository;

    /** Service for retrieving and updating specialists. */
    @Inject
    private SpecialistService specialistService;

    /** Service for retrieving customers. */
    @Inject
    private CustomerService customerService;

    /**
     * Persists a new review and updates the specialist's average rating.
     *
     * @param review the review to be saved
     * @return the persisted {@link Review}
     */
    @Transactional
    public Review create(final Review review) {
        log.info("Creating review: specialistId={}, customerId={}, rating={}",
                review.getSpecialist().getId(),
                review.getCustomer().getId(),
                review.getRating());

        reviewRepository.persist(review);
        log.debug("Review persisted: reviewId={}", review.getId());

        specialistService.computeAverageRating(review.getSpecialist().getId());
        log.debug("Average rating recalculated for specialistId={}", review.getSpecialist().getId());

        return review;
    }

    /**
     * Retrieves all reviews written for a specific specialist.
     *
     * @param specialistId the specialist's ID
     * @param page         page number for pagination
     * @param size         number of reviews per page
     * @return list of reviews for the specialist
     */
    public List<Review> getBySpecialistId(final Long specialistId, final int page, final int size) {
        log.info("Fetching reviews for specialistId={}", specialistId);
        Specialist specialist = specialistService.getById(specialistId);
        List<Review> reviews = reviewRepository.findBySpecialist(specialist, page, size);
        log.debug("Found {} reviews for specialistId={}", reviews.size(), specialistId);
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
    public List<Review> getByCustomerId(final Long customerId, final int page, final int size) {
        log.info("Fetching reviews by customerId={}", customerId);
        Customer customer = customerService.getById(customerId);
        List<Review> reviews = reviewRepository.findByCustomer(customer, page, size);
        log.debug("Found {} reviews by customerId={}", reviews.size(), customerId);
        return reviews;
    }

}
