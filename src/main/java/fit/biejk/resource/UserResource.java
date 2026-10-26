package fit.biejk.resource;

import fit.biejk.dto.AuthResponse;
import fit.biejk.dto.AvatarData;
import fit.biejk.dto.ChangeEmailRequest;
import fit.biejk.dto.ChangePasswordRequest;
import fit.biejk.entity.User;
import fit.biejk.minio.FileUploadForm;
import fit.biejk.minio.FileService;
import fit.biejk.service.AuthService;
import fit.biejk.service.TokenService;
import fit.biejk.service.UserService;
import io.quarkus.security.Authenticated;
import jakarta.annotation.security.PermitAll;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import lombok.extern.slf4j.Slf4j;


/**
 * REST endpoint for handling user file uploads such as avatar images.
 * <p>
 * Accepts multipart/form-data requests from authenticated users and delegates
 * the upload to {@link FileService}.
 * </p>
 */
@Path("/v1/users")
@Slf4j
public class UserResource {

    /**
     * Service responsible for handling file uploads and interactions with MinIO.
     */
    @Inject
    private FileService fileService;

    /**
     * Service for retrieving the currently authenticated user ID and auth logic.
     */
    @Inject
    private AuthService authService;

    /** Service for user entity management. */
    @Inject
    private UserService userService;

    /** Service for generating JWT tokens. */
    @Inject
    private TokenService tokenService;
    /**
     * Handles avatar upload for the currently authenticated user.
     *
     * @param form the uploaded file and its metadata
     * @return HTTP 200 if upload was successful, 500 otherwise
     */
    @PATCH
    @Path("/me/avatar")
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    @Authenticated
    public Response uploadAvatar(final FileUploadForm form) {
        Long userId = authService.getCurrentUserId();
        log.info("Received avatar upload request from userId={}", userId);

        try {
            fileService.uploadAvatar(userId, form.getFile(), form.getContentType());
            log.debug("Avatar uploaded successfully for userId={}", userId);
            return Response.ok().build();
        } catch (Exception e) {
            log.error("Failed to upload avatar for userId={}: {}", userId, e.getMessage(), e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(e.getMessage()).build();
        }
    }

    /**
     * Retrieves the avatar image of the specified user.
     * <p>
     * This endpoint is publicly accessible and returns the user's avatar image stream.
     * The response will have the appropriate content type (e.g., image/png, image/jpeg)
     * depending on the stored image format.
     * </p>
     *
     * @param userId the ID of the user whose avatar is to be retrieved
     * @return a {@link Response} containing the image stream with proper content type,
     *         or an error response with status {@code 500 Internal Server Error} if retrieval fails
     */
    @GET
    @Path("/{userId}/avatar")
    @Produces("image/*")
    @PermitAll
    public Response getAvatar(@PathParam("userId") final Long userId) {
        try {
            AvatarData data = fileService.getAvatar(userId);

            return Response.ok(data.stream())
                    .type(data.contentType())
                    .build();
        } catch (Exception e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(e.getMessage()).build();
        }

    }

    /**
     * Changes the current user's password.
     *
     * @param request password change request
     * @return new JWT token wrapped in {@link AuthResponse}
     */
    @PATCH
    @Path("/me/password")
    @Authenticated
    public Response changePassword(@Valid final ChangePasswordRequest request) {
        Long userId = authService.getCurrentUserId();
        User updated = userService.changePassword(
                userId,
                request.getOldPassword(),
                request.getNewPassword(),
                request.getConfirmPassword()
        );
        String token = tokenService.generateToken(updated);
        return Response.ok(new AuthResponse(token)).build();
    }

    /**
     * Changes the current user's email.
     *
     * @param request email change request
     * @return new JWT token wrapped in {@link AuthResponse}
     */
    @PATCH
    @Path("/me/email")
    @Authenticated
    public Response changeEmail(@Valid final ChangeEmailRequest request) {
        Long userId = authService.getCurrentUserId();
        User updated = userService.changeEmail(
                userId,
                request.getNewEmail(),
                request.getPassword()
        );
        String token = tokenService.generateToken(updated);
        return Response.ok(new AuthResponse(token)).build();
    }
}
