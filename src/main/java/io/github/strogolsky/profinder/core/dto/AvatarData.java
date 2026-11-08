package io.github.strogolsky.profinder.core.dto;

import java.io.InputStream;

public record AvatarData(InputStream stream, String contentType) {
}
