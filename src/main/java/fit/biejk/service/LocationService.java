package fit.biejk.service;

import fit.biejk.entity.Location;
import fit.biejk.repository.LocationRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.ws.rs.NotFoundException;
import lombok.RequiredArgsConstructor;

import java.util.List;
import java.util.UUID;

/**
 * Service layer responsible for managing {@link Location} entities.
 * <p>
 * Provides methods for retrieving all locations, finding by ID or name,
 * and obtaining the default location.
 * </p>
 */
@ApplicationScoped
@RequiredArgsConstructor
public class LocationService {

    /**
     * Repository used for accessing location data from the database.
     * <p>
     * Injected by CDI and used internally by the service methods.
     * </p>
     */
    private final LocationRepository locationRepository;

    /**
     * Retrieves all locations from the database.
     *
     * @return list of all {@link Location} entities
     */
    public List<Location> getAll() {
        return locationRepository.listAll();
    }

    /**
     * Finds a {@link Location} by its ID.
     *
     * @param locationId the unique identifier of the location
     * @return the found {@link Location}
     * @throws NotFoundException if no location with the given ID exists
     */
    public Location getById(final UUID locationId) {
        Location result = locationRepository.findById(locationId);
        if (result == null) {
            throw new NotFoundException("Location not found");
        }
        return result;
    }

    /**
     * Finds a {@link Location} by its name.
     *
     * @param name the name of the location (e.g., "Prague")
     * @return the found {@link Location}
     * @throws NotFoundException if no location with the given name exists
     */
    public Location getByName(final String name) {
        Location result = locationRepository.findByName(name);
        if (result == null) {
            throw new NotFoundException("Location not found");
        }
        return result;
    }

    /**
     * Returns the default location used for new users or orders.
     * <p>
     * Currently hardcoded to {@code "Prague"}.
     * </p>
     *
     * @return the default {@link Location}
     * @throws NotFoundException if the default location does not exist
     */
    public Location getDefault() {
        return getByName("Prague");
    }
}
