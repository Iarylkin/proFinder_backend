package fit.biejk.service;

import fit.biejk.entity.*;
import fit.biejk.repository.ProfessionalRepository;
import fit.biejk.search.ProfessionalSearchMapper;
import fit.biejk.search.ProfessionalSearchService;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.NotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.Hibernate;

import java.util.List;

/**
 * Service class for managing {@link Professional} entities.
 * <p>
 * Handles creation, retrieval, updating, and deletion of professionals.
 * Delegates common user-related logic to {@link UserService}.
 * </p>
 */
@Slf4j
@ApplicationScoped
@RequiredArgsConstructor
public class ProfessionalService {

    /**
     * Repository for accessing professional data.
     */
    private final ProfessionalRepository professionalRepository;

    /**
     * Service for handling common user operations (validation, update, delete).
     */
    private final UserService userService;

    /**
     * Service used to persist professional data to the Elasticsearch index.
     */
    private final ProfessionalSearchService professionalSearchService;

    /**
     * Mapper for converting Professional entities into ProfessionalSearchDto.
     */
    private final ProfessionalSearchMapper professionalSearchMapper;

    /** Service for managing reviews. */
    private final ReviewService reviewService;

    /** Service for authentication and user context. */
    private final AuthService authService;

    /** Service for customer management. */
    private final CustomerService customerService;


    /**
     * Creates and persists a new professional.
     * Performs email uniqueness check before creation.
     *
     * @param professional professional to be created
     * @return created professional
     */
    @Transactional
    public Professional create(final Professional professional) {
        log.info("Create professional: {}", professional.getEmail());
        userService.checkUniqueEmail(professional.getEmail());
        professionalRepository.persist(professional);

        professionalSearchService.save(professionalSearchMapper.toDto(professional));

        log.debug("Professional created with ID={}", professional.getId());
        return professional;
    }

    /**
     * Retrieves all professionals from the database.
     *
     * @param page page number for pagination
     * @param size number of professionals per page
     * @return list of all professionals
     */
    public List<Professional> getAll(final int page, final int size) {
        log.info("Get all professionals");
        List<Professional> professionals = professionalRepository.findAll().page(page - 1, size).list();
        log.debug("Found {} professionals", professionals.size());
        return professionals;
    }

    /**
     * Retrieves a professional by their ID.
     *
     * @param id professional ID
     * @return found professional
     * @throws NotFoundException if professional is not found
     */
    public Professional getById(final Long id) {
        log.info("Get professional by ID={}", id);
        Professional result = professionalRepository.findById(id);
        if (result == null) {
            log.error("Professional with ID={} not found", id);
            throw new NotFoundException("Professional with id " + id + " not found");
        }
        return result;
    }

    /**
     * Updates an existing professional.
     * Delegates base update logic to {@link UserService}, then applies additional updates.
     *
     * @param id         professional ID
     * @param professional updated professional data
     * @return updated professional
     */
    @Transactional
    public Professional update(final Long id, final Professional professional) {
        log.info("Update professional: ID={}, email={}", id, professional.getEmail());
        Professional old = getById(id);
        userService.update(id, professional);
        old.setDescription(professional.getDescription());
        professionalRepository.flush();

        old.getCategories().size();

        professionalSearchService.save(professionalSearchMapper.toDto(old));

        log.debug("Professional updated with ID={}", id);
        return old;
    }

    /**
     * Deletes a professional by ID.
     * Delegates deletion logic to {@link UserService}.
     *
     * @param id professional ID
     */
    @Transactional
    public void delete(final Long id) {
        log.info("Delete professional: ID={}", id);
        userService.delete(id);

        professionalSearchService.delete(id);

        log.debug("Professional deleted with ID={}", id);
    }

    /**
     * Recalculates and updates the average rating for a professional.
     * <p>
     * Computes the arithmetic mean from all reviews linked to the professional.
     * If no reviews are found, the rating is set to {@code null}.
     * </p>
     *
     * @param professionalId the ID of the professional whose rating should be updated
     */
    @Transactional
    public void computeAverageRating(final Long professionalId) {
        Professional professional = getById(professionalId);
        List<Review> reviews = professional.getReviews();

        if (reviews == null || reviews.isEmpty()) {
            log.warn("No reviews found for professionalId={}. Setting averageRating to null.", professionalId);
            professional.setAverageRating(null);
        } else {
            double sum = 0;
            for (Review review : reviews) {
                sum += review.getRating();
            }
            double average = sum / reviews.size();
            professional.setAverageRating(average);
            log.debug("Computed new averageRating={} for professionalId={}", average, professionalId);
        }

        professionalRepository.persist(professional);

        professionalSearchService.save(professionalSearchMapper.toDto(professional));

        log.info("Updated averageRating for professionalId={}", professionalId);
    }

    /**
     * Creates and saves a review for the specified professional.
     *
     * @param professionalId professional ID
     * @param review review entity
     * @return saved review
     */

    @Transactional
    public Review review(final Long professionalId, final Review review) {

        Long customerId = authService.getCurrentUserId();
        Customer customer = customerService.getById(customerId);
        Professional professional = getById(professionalId);

        review.setCustomer(customer);
        review.setProfessional(professional);

        return reviewService.create(review);
    }

    /**
     * Partially updates the profile of a professional, including the description and the categories:
     * only the non-null fields of {@code changes} are applied.
     *
     * @param id      ID of the professional
     * @param changes a professional holding only the requested changes
     * @return the updated professional
     */
    @Transactional
    public Professional updateProfile(final Long id, final Professional changes) {
        log.info("Update professional profile: ID={}", id);
        Professional professional = getById(id);
        userService.updateProfile(id, changes);
        if (changes.getDescription() != null) {
            professional.setDescription(changes.getDescription());
        }
        if (changes.getCategories() != null) {
            professional.setCategories(changes.getCategories());
        }
        professionalRepository.flush();
        // the resource maps the result after the transaction ends, so load the lazy categories now
        Hibernate.initialize(professional.getCategories());

        professionalSearchService.save(professionalSearchMapper.toDto(professional));

        log.debug("Professional profile updated, ID={}", id);
        return professional;
    }
}
