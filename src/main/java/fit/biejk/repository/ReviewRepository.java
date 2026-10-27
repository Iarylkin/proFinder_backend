package fit.biejk.repository;

import fit.biejk.entity.Customer;
import fit.biejk.entity.Review;
import fit.biejk.entity.Professional;
import io.quarkus.hibernate.orm.panache.PanacheRepository;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.List;

/**
 * Repository for accessing and managing {@link Review} entities.
 */
@ApplicationScoped
public class ReviewRepository implements PanacheRepository<Review> {

    /**
     * Finds all reviews submitted for the given professional.
     *
     * @param professional the professional to filter by
     * @param page       page number for pagination
     * @param size       number of reviews per page
     * @return list of reviews associated with the professional
     */
    public List<Review> findByProfessional(final Professional professional, final int page, final int size) {
        return find("professional", professional)
                .page(page - 1, size)
                .list();
    }

    /**
     * Finds all reviews submitted by the given customer.
     *
     * @param customer the customer to filter by
     * @param page   page number for pagination
     * @param size   number of reviews per page
     * @return list of reviews created by the customer
     */
    public List<Review> findByCustomer(final Customer customer, final int page, final int size) {
        return find("customer", customer)
                .page(page - 1, size)
                .list();
    }
}
