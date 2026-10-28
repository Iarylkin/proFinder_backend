package fit.biejk.service;

import fit.biejk.dto.ProfileUpdateRequest;
import fit.biejk.entity.Customer;
import fit.biejk.repository.CustomerRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.NotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.List;

/**
 * Service class for managing {@link Customer} entities.
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
     * Creates a new customer and persists it to the database.
     *
     * @param customer customer entity to create
     * @return the created customer
     */
    @Transactional
    public Customer create(final Customer customer) {
        log.info("Create customer: {}", customer.getEmail());
        userService.checkUniqueEmail(customer.getEmail());
        customerRepository.persist(customer);
        log.debug("Customer created with ID={}", customer.getId());
        return customer;
    }

    /**
     * Retrieves a list of all customers.
     *
     * @param page page number for pagination
     * @param size number of customers per page
     * @return list of customers
     */
    public List<Customer> getAll(final int page, final int size) {
        log.info("Get all customers");
        List<Customer> result = customerRepository.findAll().page(page - 1, size).list();
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
    public Customer getById(final Long id) {
        log.info("Get customer by ID={}", id);
        Customer result = customerRepository.findById(id);
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
    public Customer update(final Long id, final Customer customer) {
        log.info("Update customer: ID={}, email={}", id, customer.getEmail());
        Customer old = getById(id);
        userService.update(id, customer);
        log.debug("Customer updated with ID={}", id);
        return old;
    }

    /**
     * Deletes a customer by ID.
     *
     * @param id ID of the customer to delete
     */
    @Transactional
    public void delete(final Long id) {
        log.info("Delete customer: ID={}", id);
        userService.delete(id);
        log.debug("Customer deleted with ID={}", id);
    }

    /**
     * Partially updates the profile of a customer.
     *
     * @param id      ID of the customer
     * @param request fields to change
     * @return the updated customer
     * @throws BadRequestException if the request contains fields that only professionals have
     */
    @Transactional
    public Customer updateProfile(final Long id, final ProfileUpdateRequest request) {
        if (request.getDescription() != null || request.getCategories() != null) {
            throw new BadRequestException("Description and categories are available for professionals only");
        }
        Customer customer = getById(id);
        userService.updateProfile(id, request);
        return customer;
    }
}
