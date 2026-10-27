package fit.biejk.minio;

import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ImageTypeTest {

    private static byte[] bytes(final String text) {
        return text.getBytes(StandardCharsets.ISO_8859_1);
    }

    @Test
    void detectsJpeg() {
        byte[] header = {(byte) 0xFF, (byte) 0xD8, (byte) 0xFF, (byte) 0xE0, 0, 0, 0, 0, 0, 0, 0, 0};
        assertEquals(Optional.of(ImageType.JPEG), ImageType.detect(header));
    }

    @Test
    void detectsPng() {
        byte[] header = {(byte) 0x89, 'P', 'N', 'G', 0x0D, 0x0A, 0x1A, 0x0A, 0, 0, 0, 0};
        assertEquals(Optional.of(ImageType.PNG), ImageType.detect(header));
    }

    @Test
    void detectsGif() {
        assertEquals(Optional.of(ImageType.GIF), ImageType.detect(bytes("GIF89a......")));
        assertEquals(Optional.of(ImageType.GIF), ImageType.detect(bytes("GIF87a......")));
    }

    @Test
    void detectsWebp() {
        assertEquals(Optional.of(ImageType.WEBP), ImageType.detect(bytes("RIFF\0\0\0\0WEBP")));
    }

    @Test
    void detectsHeicAndHeif() {
        assertEquals(Optional.of(ImageType.HEIC), ImageType.detect(bytes("\0\0\0\u0018ftypheic")));
        assertEquals(Optional.of(ImageType.HEIF), ImageType.detect(bytes("\0\0\0\u0018ftypmif1")));
    }

    @Test
    void rejectsOtherContent() {
        assertTrue(ImageType.detect(bytes("MZ\u0090\0 executable")).isEmpty());
        assertTrue(ImageType.detect(bytes("<html><body>")).isEmpty());
        assertTrue(ImageType.detect(bytes("%PDF-1.7......")).isEmpty());
        assertTrue(ImageType.detect(new byte[0]).isEmpty());
        assertTrue(ImageType.detect(bytes("RIFF\0\0\0\0WAVE")).isEmpty());
    }

    @Test
    void rejectsTooShortHeader() {
        assertTrue(ImageType.detect(new byte[] {(byte) 0xFF, (byte) 0xD8}).isEmpty());
    }
}
