package io.github.strogolsky.profinder.common.event;

import java.util.UUID;

/**
 * Fired by the auth module after a user is deleted. Every module that keeps data of the user
 * (the profile, the chats) observes it and removes that data from its own database.
 *
 * @param userId ID of the deleted user
 */
public record UserDeleted(UUID userId) {
}
