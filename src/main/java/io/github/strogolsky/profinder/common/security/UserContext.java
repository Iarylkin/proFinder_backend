package io.github.strogolsky.profinder.common.security;

import java.util.UUID;

/**
 * The user who makes the current request. The identity comes from the access token that the request carries, so
 * asking for it never calls another service or the database. Every module uses this interface instead of the
 * classes of the authentication module.
 */
public interface UserContext {

    /**
     * Returns the ID of the authenticated user of the current request.
     *
     * @return the user ID
     */
    UUID id();

    /**
     * Checks whether the authenticated user of the current request is the given user.
     *
     * @param userId the user ID to compare with
     * @return {@code true} if it is the same user
     */
    boolean is(UUID userId);
}
