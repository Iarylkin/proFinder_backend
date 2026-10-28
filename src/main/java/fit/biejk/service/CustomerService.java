package fit.biejk.service;

import fit.biejk.entity.CustomerProfile;
import fit.biejk.repository.CustomerRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.NotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.UUID;

/**
 * Service class for managing {@link CustomerProfile} entities.
 * <p>
 * Handles creation, retrieval, updating, and deletion of customers,
 * and delegates shared logic to {@link UserService}.
 * </p>
 */
@Slf4j
@RequiredArgsConstructor
@ApplicationScoped
public class CustomerService {

    /**
     * Repository for accessing customer data.
     */
    private final CustomerRepository customerRepository;

    /**
     * Service for shared user operations (validation, deletion, etc.).
     */
    private final UserService userService;

    /**
     * Retrieves a list of all customers.
     *
     * @param page page number for pagination
     * @param size number of customers per page
     * @return list of customers
     */
    public List<CustomerProfile> getAll(final int page, final int size) {
        log.info("Get all customers");
        List<CustomerProfile> result = customerRepository.findAll().page(page - 1, size).list();
        log.debug("Found {} customers", result.size());
        return result;
    }

    /**
     * Retrieves a customer by their ID.
     *
     * @param id ID of the customer
     * @return found customer
     * @throws NotFoundException if the customer is not found
     */
    public CustomerProfile getById(final UUID id) {
        log.info("Get customer by ID={}", id);
        CustomerProfile result = customerRepository.findById(id);
        if (result == null) {
            log.error("Customer with ID={} not found", id);
            throw new NotFoundException("Customer with id " + id + " not found");
        }
        return result;
    }

    /**
     * Updates customer data by ID.
     *
     * @param id     ID of the customer to update
     * @param customer updated customer data
     * @return the original customer before update
     */
    @Transactional
    public CustomerProfile update(final UUID id, final CustomerProfile customer) {
        log.info("Update customer: ID={}, email={}", id, customer.getUser().getEmail());
        CustomerProfile old = getById(id);
        userService.update(id, customer.getUser());
        log.debug("Customer updated with ID={}", id);
        return old;
    }

    /**
     * Deletes a customer by ID.
     *
     * @param id ID of the customer to delete
     */
    @Transactional
    public void delete(final UUID id) {
        log.info("Delete customer: ID={}", id);
        userService.delete(id);
        log.debug("Customer deleted with ID={}", id);
    }

    /**
     * Partially updates the profile of a customer: only the non-null fields of {@code changes} are applied.
     *
     * @param id      ID of the customer
     * @param changes a customer holding only the requested changes
     * @return the updated customer
     */
    @Transactional
    public CustomerProfile updateProfile(final UUID id, final CustomerProfile changes) {
        CustomerProfile customer = getById(id);
        userService.updateProfile(id, changes.getUser());
        return customer;
    }
}
