package fit.biejk.resource;

import fit.biejk.entity.Category;
import fit.biejk.service.CategoryService;
import jakarta.annotation.security.PermitAll;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.core.Response;
import lombok.RequiredArgsConstructor;

import java.util.List;

/**
 * REST resource for accessing categories.
 * <p>
 * Provides an endpoint for retrieving the list of all available categories.
 * </p>
 */
@Path("/v1/categories")
@RequiredArgsConstructor
public class CategoryResource {

    /** Service for handling business logic related to categories. */
    private final CategoryService categoryService;

    /**
     * Retrieves all available categories.
     *
     * @return HTTP response containing a list of categories
     */
    @GET
    @PermitAll
    public Response getAll() {
        List<Category> categories = categoryService.getAll();
        return Response.ok(categories).build();
    }
}
