package fit.biejk.core.dto;

import java.io.InputStream;

public record AvatarData(InputStream stream, String contentType) {
}
