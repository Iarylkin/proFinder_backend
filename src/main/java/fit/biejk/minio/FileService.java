package fit.biejk.minio;

import fit.biejk.dto.AvatarData;
import fit.biejk.entity.User;
import fit.biejk.exception.FileTooLargeException;
import fit.biejk.exception.InvalidFileTypeException;
import fit.biejk.service.UserService;
import io.minio.*;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.InternalServerErrorException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;

import java.util.UUID;

/**
 * Service for handling user file operations with MinIO,
 * including avatar uploads and downloads.
 */
@ApplicationScoped
@Slf4j
@RequiredArgsConstructor
public class FileService {

    /**
     * Name of the MinIO bucket used to store user avatar images.
     * <p>
     * This constant defines a logical separation for storing all avatars
     * in a single bucket named <strong>"user-avatars"</strong>. The bucket is created
     * automatically if it does not exist before the first upload.
     * </p>
     */
    private static final String BUCKET = "user-avatars";

    /**
     * Maximum avatar size in bytes (5 MB).
     */
    private static final int AVATAR_MAX_BYTES = 5 * 1024 * 1024;


    /**
     * MinIO client for interacting with the S3-compatible storage.
     */
    private final MinioClient minioClient;

    /**
     * Service for accessing and updating {@link User} entities.
     */
    private final UserService userService;

    /**
     * Uploads the avatar to MinIO and saves the avatar key to the user.
     *
     * @param userId          the ID of the user
     * @param fileInputStream file data to upload
     * @throws FileTooLargeException       if the file exceeds {@value #AVATAR_MAX_BYTES} bytes
     * @throws InvalidFileTypeException    if its content is not a supported image
     * @throws InternalServerErrorException if reading or storing the file fails
     */
    @Transactional
    public void uploadAvatar(final UUID userId, final InputStream fileInputStream) {
        log.info("Uploading avatar for userId={}", userId);

        User user = userService.getById(userId);
        byte[] fileBytes = readAvatar(fileInputStream);
        if (fileBytes.length > AVATAR_MAX_BYTES) {
            log.warn("Avatar rejected, too large: userId={}", userId);
            throw new FileTooLargeException();
        }
        ImageType type = ImageType.detect(fileBytes).orElseThrow(() -> {
            log.warn("Avatar rejected, unsupported type: userId={}", userId);
            return new InvalidFileTypeException();
        });
        String avatarKey = "avatars/" + userId;

        try {
            ensureBucketExists();

            minioClient.putObject(
                    PutObjectArgs.builder()
                            .bucket(BUCKET)
                            .object(avatarKey)
                            .stream(new ByteArrayInputStream(fileBytes), fileBytes.length, -1)
                            .contentType(type.getMimeType())
                            .build()
            );
        } catch (Exception e) {
            log.error("Failed to store avatar for userId={}", userId, e);
            throw new InternalServerErrorException("Failed to upload avatar");
        }

        user.setAvatarKey(avatarKey);

        log.debug("Avatar uploaded and key set for userId={}", userId);
    }

    private byte[] readAvatar(final InputStream fileInputStream) {
        try {
            return fileInputStream.readNBytes(AVATAR_MAX_BYTES + 1);
        } catch (IOException e) {
            log.error("Failed to read the uploaded avatar", e);
            throw new InternalServerErrorException("Failed to upload avatar");
        }
    }

    /**
     * Generates a presigned URL for downloading the user's avatar.
     *
     * @param userId ID of the user
     * @return presigned URL for accessing the avatar
     * @throws Exception if URL generation fails
     */
    public AvatarData getAvatar(final UUID userId) throws Exception {
        log.info("Getting avatar for userId={}", userId);

        User user = userService.getById(userId);
        if (user.getAvatarKey() == null) {
            log.warn("User with id={} has no avatar", userId);
            throw new IllegalStateException("User does not have an avatar");
        }

        StatObjectResponse stat = minioClient.statObject(
                StatObjectArgs.builder()
                        .bucket(BUCKET)
                        .object(user.getAvatarKey())
                        .build());

        String contentType = stat.contentType() != null
                ? stat.contentType()
                : "application/octet-stream";


        InputStream stream = minioClient.getObject(
                GetObjectArgs.builder()
                        .bucket(BUCKET)
                        .object(user.getAvatarKey())
                        .build());

        log.info("Successfully retrieved avatar stream for userId={}", userId);
        return new AvatarData(stream, contentType);
    }

    /**
     * Ensures that the bucket used for storing user avatars exists.
     * Creates it if it does not.
     *
     * @throws Exception if checking or creation fails
     */
    private void ensureBucketExists() throws Exception {
        boolean found = minioClient.bucketExists(
                BucketExistsArgs.builder().bucket(BUCKET).build()
        );

        if (!found) {
            log.info("Bucket '{}' not found, creating...", BUCKET);
            minioClient.makeBucket(
                    MakeBucketArgs.builder().bucket(BUCKET).build()
            );
            log.debug("Bucket '{}' created successfully", BUCKET);
        }
    }
}
