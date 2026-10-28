package fit.biejk.resource;

import fit.biejk.entity.Location;
import fit.biejk.service.LocationService;
import jakarta.annotation.security.PermitAll;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import lombok.RequiredArgsConstructor;

import java.util.List;

/**
 * REST endpoint for accessing location-related data.
 * <p>
 * Provides public access to the list of all available {@link Location} entities.
 * </p>
 */
@Path("/v1/locations")
@RequiredArgsConstructor
public class LocationResource {

    /**
     * Service used for retrieving location data from the database.
     * <p>
     * Injected via CDI and used to delegate business logic.
     * </p>
     */
    private final LocationService locationService;

    /**
     * Retrieves a list of all available locations.
     * <p>
     * This endpoint is accessible by all users without authentication.
     * </p>
     *
     * @return list of {@link Location} entities
     */
    @GET
    @PermitAll
    public List<Location> getAll() {
        return locationService.getAll();
    }
}
