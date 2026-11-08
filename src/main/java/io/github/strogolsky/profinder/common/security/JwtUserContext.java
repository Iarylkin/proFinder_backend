package io.github.strogolsky.profinder.common.security;

import io.quarkus.security.identity.SecurityIdentity;
import jakarta.enterprise.context.ApplicationScoped;
import lombok.RequiredArgsConstructor;

import java.util.UUID;

/**
 * {@link UserContext} that reads the user from the verified access token: its subject ({@code sub}) is the user ID.
 */
@ApplicationScoped
@RequiredArgsConstructor
public class JwtUserContext implements UserContext {

    /**
     * The identity of the current request, made from the verified token.
     */
    private final SecurityIdentity identity;

    /**
     * Returns the subject of the token as the user ID.
     *
     * @return the user ID
     */
    @Override
    public UUID id() {
        return UUID.fromString(identity.getPrincipal().getName());
    }

    /**
     * Compares the user ID of the token with the given one.
     *
     * @param userId the user ID to compare with
     * @return {@code true} if it is the same user
     */
    @Override
    public boolean is(final UUID userId) {
        return id().equals(userId);
    }
}
