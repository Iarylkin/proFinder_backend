package fit.biejk.resource;

import fit.biejk.dto.*;
import fit.biejk.service.AuthService;
import jakarta.annotation.security.PermitAll;
import jakarta.validation.Valid;
import jakarta.ws.rs.PATCH;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.core.Response;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * REST resource for handling authentication-related operations such as registration, login,
 * password change, and password recovery.
 * <p>
 * Routes are grouped under the "/auth" path.
 * </p>
 */
@Path("/v1/auth")
@Slf4j
@RequiredArgsConstructor
public class AuthResource {

    /**
     * Service for handling authentication logic such as user registration, login,
     * password reset, and password change.
     */
    private final AuthService authService;

    /**
     * Registers a new user account.
     *
     * @param request the registration request containing email, password, and role
     * @return HTTP 200 response with a generated authentication token
     */
    @POST
    @Path("/register")
    @PermitAll
    public Response register(
            @Valid final AuthRequest request
    ) {
        log.info("Register request: {}", request);
        String result = authService.register(request.getEmail(), request.getPassword(), request.getRole());
        AuthResponse response = new AuthResponse(result);
        log.info("Register response: {}", response);


        return Response.status(Response.Status.CREATED).entity(response).build();
    }

    /**
     * Authenticates a user and returns a JWT token if credentials are valid.
     *
     * @param request the login request containing email, password, and role
     * @return HTTP 200 response with a generated authentication token
     */
    @POST
    @Path("/login")
    @PermitAll
    public Response login(@Valid final AuthRequest request) {
        log.info("Login request: {}", request);
        String result = authService.login(request.getEmail(), request.getPassword(), request.getRole());
        AuthResponse response = new AuthResponse(result);
        log.info("Login response: {}", response);
        return Response.ok(response).build();
    }

    /**
     * Initiates password reset by generating and emailing a verification code to the user.
     *
     * @param request contains the email address of the user
     * @return HTTP 202 response, whether or not a user with this email exists
     */
    @POST
    @Path("/password-recovery")
    @PermitAll
    public Response createRecoveryRequest(@Valid final ForgotPasswordRequest request) {
        authService.forgotPassword(request.getEmail());
        return Response.accepted().build();
    }

    /**
     * Resets the user's password using a verification code previously sent by email.
     *
     * @param request contains email, verification code, and new password with confirmation
     * @return HTTP 200 response with a new authentication token
     */
    @PATCH
    @Path("/password-recovery")
    @PermitAll
    public Response completeRecovery(@Valid final ResetPasswordRequest request) {
        AuthResponse response = new AuthResponse(authService.resetPassword(request));
        return Response.ok(response).build();
    }

}
