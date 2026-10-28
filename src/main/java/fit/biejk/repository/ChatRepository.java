package fit.biejk.repository;

import fit.biejk.entity.Chat;
import io.quarkus.hibernate.orm.panache.PanacheRepositoryBase;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.List;
import java.util.UUID;

/**
 * Repository class for managing {@link Chat} entities.
 * <p>
 * Provides database operations for Chat entities using Panache.
 * </p>
 */
@ApplicationScoped
public class ChatRepository implements PanacheRepositoryBase<Chat, UUID> {

    /**
     * Finds a chat by the IDs of two users.
     * <p>
     * Ensures consistent ordering of user IDs to match unique constraint.
     * </p>
     *
     * @param userId1 ID of the first user
     * @param userId2 ID of the second user
     * @return The {@link Chat} entity if found, otherwise {@code null}
     */
    public Chat findByUsersId(final UUID userId1, final UUID userId2) {
        UUID first = userId1.compareTo(userId2) <= 0 ? userId1 : userId2;
        UUID second = first.equals(userId1) ? userId2 : userId1;
        return find("user1.id = ?1 AND user2.id = ?2", first, second).firstResult();
    }

    /**
     * Finds all chats where the given user is a participant.
     *
     * @param userId ID of the user
     * @param page   page number for pagination
     * @param size   number of chats per page
     * @return list of {@link Chat} entities
     */
    public List<Chat> findByUserId(final UUID userId, final int page, final int size) {
        return find("user1.id = ?1 OR user2.id = ?1", userId)
                .page(page - 1, size)
                .list();
    }
}
