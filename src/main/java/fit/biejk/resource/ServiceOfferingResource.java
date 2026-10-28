package fit.biejk.resource;

import fit.biejk.entity.ServiceOffering;
import fit.biejk.service.ServiceOfferingService;
import jakarta.annotation.security.PermitAll;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.core.Response;

import java.util.List;

/**
 * REST resource for accessing service offerings.
 * <p>
 * Provides an endpoint for retrieving the list of all available services.
 * </p>
 */
@Path("/v1/categories")
public class ServiceOfferingResource {

    /** Service for handling business logic related to service offerings. */
    @Inject
    private ServiceOfferingService serviceOfferingService;

    /**
     * Retrieves all available service offerings.
     *
     * @return HTTP response containing a list of services
     */
    @GET
    @PermitAll
    public Response getAll() {
        List<ServiceOffering> serviceOfferings = serviceOfferingService.getAll();
        return Response.ok(serviceOfferings).build();
    }
}
