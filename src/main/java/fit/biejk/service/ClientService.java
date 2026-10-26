package fit.biejk.service;

import fit.biejk.entity.Client;
import fit.biejk.repository.ClientRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.NotFoundException;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.List;

/**
 * Service class for managing {@link Client} entities.
 * <p>
 * Handles creation, retrieval, updating, and deletion of clients,
 * and delegates shared logic to {@link UserService}.
 * </p>
 */
@Slf4j
@NoArgsConstructor
@ApplicationScoped
public class ClientService {

    /**
     * Repository for accessing client data.
     */
    @Inject
    private ClientRepository clientRepository;

    /**
     * Service for shared user operations (validation, deletion, etc.).
     */
    @Inject
    private UserService userService;

    /**
     * Creates a new client and persists it to the database.
     *
     * @param client client entity to create
     * @return the created client
     */
    @Transactional
    public Client create(final Client client) {
        log.info("Create client: {}", client.getEmail());
        userService.checkUniqueEmail(client.getEmail());
        clientRepository.persist(client);
        log.debug("Client created with ID={}", client.getId());
        return client;
    }

    /**
     * Retrieves a list of all clients.
     *
     * @param page page number for pagination
     * @param size number of clients per page
     * @return list of clients
     */
    public List<Client> getAll(final int page, final int size) {
        log.info("Get all clients");
        List<Client> result = clientRepository.findAll().page(page - 1, size).list();
        log.debug("Found {} clients", result.size());
        return result;
    }

    /**
     * Retrieves a client by their ID.
     *
     * @param id ID of the client
     * @return found client
     * @throws NotFoundException if the client is not found
     */
    public Client getById(final Long id) {
        log.info("Get client by ID={}", id);
        Client result = clientRepository.findById(id);
        if (result == null) {
            log.error("Client with ID={} not found", id);
            throw new NotFoundException("Client with id " + id + " not found");
        }
        return result;
    }

    /**
     * Updates client data by ID.
     *
     * @param id     ID of the client to update
     * @param client updated client data
     * @return the original client before update
     */
    @Transactional
    public Client update(final Long id, final Client client) {
        log.info("Update client: ID={}, email={}", id, client.getEmail());
        Client old = getById(id);
        userService.update(id, client);
        log.debug("Client updated with ID={}", id);
        return old;
    }

    /**
     * Deletes a client by ID.
     *
     * @param id ID of the client to delete
     */
    @Transactional
    public void delete(final Long id) {
        log.info("Delete client: ID={}", id);
        userService.delete(id);
        log.debug("Client deleted with ID={}", id);
    }
}
