package fit.biejk.resource;

import fit.biejk.dto.ProfileUpdateRequest;
import fit.biejk.entity.Customer;
import fit.biejk.entity.Professional;
import fit.biejk.entity.User;
import fit.biejk.mapper.CustomerMapper;
import fit.biejk.mapper.ProfessionalMapper;
import fit.biejk.service.AuthService;
import fit.biejk.service.ProfileService;
import io.quarkus.security.Authenticated;
import jakarta.validation.Valid;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.PATCH;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.core.Response;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * REST resource for the profile of the authenticated user.
 * <p>
 * The same paths serve customers and professionals; the response depends on the role of the caller.
 * </p>
 */
@Slf4j
@Path("/v1/users/me/profile")
@RequiredArgsConstructor
public class ProfileResource {

    /**
     * Service for reading and updating profiles.
     */
    private final ProfileService profileService;

    /**
     * Service for resolving the current user.
     */
    private final AuthService authService;

    /**
     * Mapper for customer profiles.
     */
    private final CustomerMapper customerMapper;

    /**
     * Mapper for professional profiles.
     */
    private final ProfessionalMapper professionalMapper;

    /**
     * Returns the profile of the authenticated user.
     *
     * @return a customer or professional DTO, depending on the role
     */
    @GET
    @Authenticated
    public Response getProfile() {
        Long userId = authService.getCurrentUserId();
        log.info("Get profile: userId={}", userId);
        return Response.ok(toDto(profileService.get(userId))).build();
    }

    /**
     * Partially updates the profile of the authenticated user. Only the sent fields change.
     *
     * @param request fields to change
     * @return the updated profile
     */
    @PATCH
    @Authenticated
    public Response updateProfile(@Valid final ProfileUpdateRequest request) {
        Long userId = authService.getCurrentUserId();
        log.info("Update profile: userId={}, request={}", userId, request);
        return Response.ok(toDto(profileService.update(userId, request))).build();
    }

    private Object toDto(final User profile) {
        if (profile instanceof Professional professional) {
            return professionalMapper.toDto(professional);
        }
        return customerMapper.toDto((Customer) profile);
    }
}
