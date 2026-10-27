package fit.biejk.minio;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Optional;
import java.util.Set;

/**
 * Image formats accepted for avatars, recognised by their magic bytes
 * and never by the file extension or the content type sent by the client.
 */
public enum ImageType {

    /** JPEG image. */
    JPEG("image/jpeg"),

    /** PNG image. */
    PNG("image/png"),

    /** GIF image. */
    GIF("image/gif"),

    /** WebP image. */
    WEBP("image/webp"),

    /** HEIC image. */
    HEIC("image/heic"),

    /** HEIF image. */
    HEIF("image/heif");

    /**
     * Number of leading bytes needed to recognise every supported format.
     */
    public static final int HEADER_LENGTH = 12;

    /** First bytes of a JPEG file. */
    private static final byte[] JPEG_SIGNATURE = {(byte) 0xFF, (byte) 0xD8, (byte) 0xFF};

    /** First bytes of a PNG file. */
    private static final byte[] PNG_SIGNATURE = {(byte) 0x89, 'P', 'N', 'G', 0x0D, 0x0A, 0x1A, 0x0A};

    /** Offset of the brand in an ISO base media file (HEIC, HEIF). */
    private static final int BRAND_OFFSET = 8;

    /** Offset of the {@code ftyp} marker in an ISO base media file. */
    private static final int FTYP_OFFSET = 4;

    /** Offset of the {@code WEBP} marker in a RIFF container. */
    private static final int WEBP_OFFSET = 8;

    /** Brands that identify HEIC images. */
    private static final Set<String> HEIC_BRANDS = Set.of("heic", "heix", "hevc", "hevx", "heim", "heis");

    /** Brands that identify HEIF images. */
    private static final Set<String> HEIF_BRANDS = Set.of("mif1", "msf1");

    /**
     * MIME type that is stored with the object.
     */
    private final String mimeType;

    ImageType(final String mimeType) {
        this.mimeType = mimeType;
    }

    /**
     * Returns the MIME type of this format.
     *
     * @return MIME type, for example {@code image/png}
     */
    public String getMimeType() {
        return mimeType;
    }

    /**
     * Detects the image format from the first bytes of a file.
     *
     * @param header leading bytes of the file, at least {@value #HEADER_LENGTH} when available
     * @return the detected format, or an empty optional if it is not a supported image
     */
    public static Optional<ImageType> detect(final byte[] header) {
        if (startsWith(header, 0, JPEG_SIGNATURE)) {
            return Optional.of(JPEG);
        }
        if (startsWith(header, 0, PNG_SIGNATURE)) {
            return Optional.of(PNG);
        }
        if (ascii(header, 0, "GIF87a") || ascii(header, 0, "GIF89a")) {
            return Optional.of(GIF);
        }
        if (ascii(header, 0, "RIFF") && ascii(header, WEBP_OFFSET, "WEBP")) {
            return Optional.of(WEBP);
        }
        if (ascii(header, FTYP_OFFSET, "ftyp") && header.length >= HEADER_LENGTH) {
            String brand = new String(header, BRAND_OFFSET, 4, StandardCharsets.US_ASCII);
            if (HEIC_BRANDS.contains(brand)) {
                return Optional.of(HEIC);
            }
            if (HEIF_BRANDS.contains(brand)) {
                return Optional.of(HEIF);
            }
        }
        return Optional.empty();
    }

    private static boolean startsWith(final byte[] data, final int offset, final byte[] prefix) {
        return data.length >= offset + prefix.length
                && Arrays.equals(data, offset, offset + prefix.length, prefix, 0, prefix.length);
    }

    private static boolean ascii(final byte[] data, final int offset, final String text) {
        return startsWith(data, offset, text.getBytes(StandardCharsets.US_ASCII));
    }
}
