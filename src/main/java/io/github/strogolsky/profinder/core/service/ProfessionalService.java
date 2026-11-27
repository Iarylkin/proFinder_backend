package io.github.strogolsky.profinder.core.service;

import io.github.strogolsky.profinder.auth.service.UserService;
import io.github.strogolsky.profinder.common.security.UserContext;
import io.github.strogolsky.profinder.common.service.VersionCheck;
import io.github.strogolsky.profinder.common.validation.Text;
import io.github.strogolsky.profinder.core.entity.CustomerProfile;
import io.github.strogolsky.profinder.core.entity.ProfessionalProfile;
import io.github.strogolsky.profinder.core.entity.Review;
import io.github.strogolsky.profinder.core.exception.CategoryRequiredException;
import io.github.strogolsky.profinder.core.repository.ProfessionalRepository;
import io.github.strogolsky.profinder.core.search.ProfessionalSearchMapper;
import io.github.strogolsky.profinder.core.search.ProfessionalSearchService;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.NotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.Hibernate;

import java.util.List;
import java.util.UUID;

/**
 * Service class for managing {@link ProfessionalProfile} entities.
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

    /**
     * The user who makes the current request.
     */
    private final UserContext userContext;

    /** Service for customer management. */
    private final CustomerService customerService;

    /** Service for the profiles of the users. */
    private final ProfileService profileService;

    /**
     * Service for the categories: a request names them by id only.
     */
    private final CategoryService categoryService;


    /**
     * Puts a newly created professional profile into the search index.
     * <p>
     * Called for the profiles of the professional role only: customer profiles are not indexed.
     * </p>
     *
     * @param professional the profile that was just created
     */
    void indexCreated(@Observes final ProfessionalProfile professional) {
        log.info("Index new professional profile: ID={}", professional.getId());
        professionalSearchService.save(professionalSearchMapper.toDto(professional));
    }

    /**
     * Retrieves all professionals from the database.
     *
     * @param page page number for pagination
     * @param size number of professionals per page
     * @return list of all professionals
     */
    public List<ProfessionalProfile> getAll(final int page, final int size) {
        log.info("Get all professionals");
        List<ProfessionalProfile> professionals = professionalRepository.findAll().page(page - 1, size).list();
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
    public ProfessionalProfile getById(final UUID id) {
        log.info("Get professional by ID={}", id);
        ProfessionalProfile result = professionalRepository.findById(id);
        if (result == null) {
            log.error("Professional with ID={} not found", id);
            throw new NotFoundException("Professional with id " + id + " not found");
        }
        return result;
    }

    /**
     * Deletes a professional by ID.
     * Delegates deletion logic to {@link UserService}.
     *
     * @param id professional ID
     */
    public void delete(final UUID id) {
        log.info("Delete professional: ID={}", id);
        userService.delete(id);

        professionalSearchService.delete(id);

        log.debug("Professional deleted with ID={}", id);
    }

    /**
     * Creates and saves a review for the specified professional.
     *
     * @param professionalId professional ID
     * @param review review entity
     * @return saved review
     */

    @Transactional
    public Review review(final UUID professionalId, final Review review) {

        UUID customerId = userContext.id();
        CustomerProfile customer = customerService.getById(customerId);
        ProfessionalProfile professional = getById(professionalId);

        review.setCustomer(customer);
        review.setProfessional(professional);

        Review created = reviewService.create(review);
        updateRating(professional);
        return created;
    }

    /**
     * Recalculates the rating of a professional after a review and saves it with the search index.
     *
     * @param professional the professional whose rating changed
     */
    private void updateRating(final ProfessionalProfile professional) {
        professional.setRating(calculateRating(professional.getId()));
        professionalRepository.persist(professional);
        professionalSearchService.save(professionalSearchMapper.toDto(professional));
        log.info("Updated rating={} for professionalId={}",
                professional.getRating(), professional.getId());
    }

    /**
     * The rule of the rating of a professional: the arithmetic mean of the ratings of all their reviews, or
     * {@code 0} when there are none. Change the rating here; the callers do not depend on the formula.
     *
     * @param professionalId ID of the professional
     * @return the rating
     */
    private double calculateRating(final UUID professionalId) {
        return reviewService.getRatingsByProfessionalId(professionalId).stream()
                .mapToInt(Integer::intValue)
                .average()
                .orElse(0.0);
    }

    /**
     * Partially updates the profile of a professional, including the description and the categories:
     * only the non-null fields of {@code changes} are applied. The description is optional (the empty text clears
     * it); the categories can only be replaced, an empty list is rejected.
     *
     * @param id      ID of the professional
     * @param changes a professional holding only the requested changes
     * @return the updated professional
     */
    @Transactional
    public ProfessionalProfile updateProfile(final UUID id, final ProfessionalProfile changes) {
        log.info("Update professional profile: ID={}", id);
        ProfessionalProfile professional = getById(id);
        VersionCheck.require(professional, changes.getVersion());
        profileService.updatePersonalData(professional, changes);
        if (changes.getDescription() != null) {
            professional.setDescription(Text.emptyToNull(changes.getDescription()));
        }
        if (changes.getCategories() != null) {
            if (changes.getCategories().isEmpty()) {
                throw new CategoryRequiredException();
            }
            professional.setCategories(categoryService.resolve(changes.getCategories()));
        }
        professionalRepository.flush();
        // the resource maps the result after the transaction ends, so load the lazy categories now
        Hibernate.initialize(professional.getCategories());

        professionalSearchService.save(professionalSearchMapper.toDto(professional));

        log.debug("Professional profile updated, ID={}", id);
        return professional;
    }
}
