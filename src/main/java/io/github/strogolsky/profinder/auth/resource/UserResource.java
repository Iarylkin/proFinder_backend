package io.github.strogolsky.profinder.auth.resource;

import io.github.strogolsky.profinder.auth.dto.AuthResponse;
import io.github.strogolsky.profinder.auth.dto.ChangeEmailRequest;
import io.github.strogolsky.profinder.auth.dto.ChangePasswordRequest;
import io.github.strogolsky.profinder.auth.entity.User;
import io.github.strogolsky.profinder.auth.service.TokenService;
import io.github.strogolsky.profinder.auth.service.UserService;
import io.github.strogolsky.profinder.common.security.UserContext;
import io.github.strogolsky.profinder.core.dto.AvatarData;
import io.github.strogolsky.profinder.core.minio.FileService;
import io.github.strogolsky.profinder.core.minio.FileUploadForm;
import io.quarkus.security.Authenticated;
import jakarta.annotation.security.PermitAll;
import jakarta.validation.Valid;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.UUID;


/**
 * REST endpoint for handling user file uploads such as avatar images.
 * <p>
 * Accepts multipart/form-data requests from authenticated users and delegates
 * the upload to {@link FileService}.
 * </p>
 */
@Path("/v1/users")
@Slf4j
@RequiredArgsConstructor
public class UserResource {

    /**
     * Service responsible for handling file uploads and interactions with MinIO.
     */
    private final FileService fileService;

    /**
     * The user who makes the current request.
     */
    private final UserContext userContext;

    /** Service for user entity management. */
    private final UserService userService;

    /** Service for generating JWT tokens. */
    private final TokenService tokenService;
    /**
     * Handles avatar upload for the currently authenticated user.
     *
     * @param form the uploaded file and its metadata
     * @return HTTP 204 if upload was successful, 413 if the file is too large,
     *         415 if it is not a supported image, 500 otherwise
     */
    @PATCH
    @Path("/me/avatar")
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    @Authenticated
    public Response uploadAvatar(final FileUploadForm form) {
        UUID userId = userContext.id();
        log.info("Received avatar upload request from userId={}", userId);

        fileService.uploadAvatar(userId, form.getFile());
        log.debug("Avatar uploaded successfully for userId={}", userId);
        return Response.noContent().build();
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
     * @return a {@link Response} containing the image stream with proper content type;
     *         {@code 404} if the user or the avatar does not exist, {@code 500} if the storage fails
     * @throws Exception if the storage fails (answered with {@code 500})
     */
    @GET
    @Path("/{userId}/avatar")
    @Produces("image/*")
    @PermitAll
    public Response getAvatar(@PathParam("userId") final UUID userId) throws Exception {
        AvatarData data = fileService.getAvatar(userId);

        return Response.ok(data.stream())
                .type(data.contentType())
                .build();
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
        UUID userId = userContext.id();
        User updated = userService.changePassword(
                userId,
                request.oldPassword(),
                request.newPassword(),
                request.confirmPassword()
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
        UUID userId = userContext.id();
        User updated = userService.changeEmail(
                userId,
                request.newEmail(),
                request.password()
        );
        String token = tokenService.generateToken(updated);
        return Response.ok(new AuthResponse(token)).build();
    }
}
