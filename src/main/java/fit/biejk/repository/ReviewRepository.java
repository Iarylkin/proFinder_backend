package fit.biejk.repository;

import fit.biejk.entity.Client;
import fit.biejk.entity.Review;
import fit.biejk.entity.Specialist;
import io.quarkus.hibernate.orm.panache.PanacheRepository;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.List;

/**
 * Repository for accessing and managing {@link Review} entities.
 */
@ApplicationScoped
public class ReviewRepository implements PanacheRepository<Review> {

    /**
     * Finds all reviews submitted for the given specialist.
     *
     * @param specialist the specialist to filter by
     * @param page       page number for pagination
     * @param size       number of reviews per page
     * @return list of reviews associated with the specialist
     */
    public List<Review> findBySpecialist(final Specialist specialist, final int page, final int size) {
        return find("specialist", specialist)
                .page(page - 1, size)
                .list();
    }

    /**
     * Finds all reviews submitted by the given client.
     *
     * @param client the client to filter by
     * @param page   page number for pagination
     * @param size   number of reviews per page
     * @return list of reviews created by the client
     */
    public List<Review> findByClient(final Client client, final int page, final int size) {
        return find("client", client)
                .page(page - 1, size)
                .list();
    }
}
