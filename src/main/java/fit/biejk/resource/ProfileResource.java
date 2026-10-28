package fit.biejk.resource;

import fit.biejk.dto.ProfileUpdateRequest;
import fit.biejk.entity.UserRole;
import fit.biejk.mapper.CustomerMapper;
import fit.biejk.mapper.ProfessionalMapper;
import fit.biejk.service.AuthService;
import fit.biejk.service.CustomerService;
import fit.biejk.service.ProfessionalService;
import io.quarkus.security.Authenticated;
import jakarta.validation.Valid;
import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.ForbiddenException;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.PATCH;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.core.Response;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.UUID;

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
     * Service for resolving the current user and their role.
     */
    private final AuthService authService;

    /**
     * Service for customer profiles.
     */
    private final CustomerService customerService;

    /**
     * Service for professional profiles.
     */
    private final ProfessionalService professionalService;

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
     * @throws ForbiddenException if the account has no role yet
     */
    @GET
    @Authenticated
    public Response getProfile() {
        UUID userId = authService.getCurrentUserId();
        log.info("Get profile: userId={}", userId);
        return switch (roleOfCurrentUser()) {
            case CUSTOMER -> Response.ok(customerMapper.toDto(customerService.getById(userId))).build();
            case PROFESSIONAL -> Response.ok(professionalMapper.toDto(professionalService.getById(userId))).build();
        };
    }

    /**
     * Partially updates the profile of the authenticated user. Only the sent fields change.
     *
     * @param request fields to change
     * @return the updated profile
     * @throws ForbiddenException  if the account has no role yet
     * @throws BadRequestException if a customer sends fields that only professionals have
     */
    @PATCH
    @Authenticated
    public Response updateProfile(@Valid final ProfileUpdateRequest request) {
        UUID userId = authService.getCurrentUserId();
        log.info("Update profile: userId={}, request={}", userId, request);
        return switch (roleOfCurrentUser()) {
            case CUSTOMER -> {
                if (request.getDescription() != null || request.getCategories() != null) {
                    throw new BadRequestException("Description and categories are available for professionals only");
                }
                var customer = customerService.updateProfile(userId, customerMapper.toEntity(request));
                yield Response.ok(customerMapper.toDto(customer)).build();
            }
            case PROFESSIONAL -> {
                var professional = professionalService.updateProfile(userId, professionalMapper.toEntity(request));
                yield Response.ok(professionalMapper.toDto(professional)).build();
            }
        };
    }

    private UserRole roleOfCurrentUser() {
        return authService.getCurrentUserRole()
                .orElseThrow(() -> new ForbiddenException("This account has no role yet"));
    }
}
