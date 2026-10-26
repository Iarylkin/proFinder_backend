package fit.biejk.repository;

import fit.biejk.entity.ChatMessage;
import io.quarkus.hibernate.orm.panache.PanacheRepository;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.List;

/**
 * Repository class for managing {@link ChatMessage} entities.
 * <p>
 * Provides access to basic CRUD operations using Panache.
 * Additional custom queries can be added here as needed.
 * </p>
 */
@ApplicationScoped
public class ChatMessageRepository implements PanacheRepository<ChatMessage> {

    /**
     * Retrieves a paginated list of messages for a specific chat, ordered by creation date.
     *
     * @param chatId the ID of the chat
     * @param page   page number for pagination
     * @param size   number of messages per page
     * @return a list of {@link ChatMessage} entities
     */
    public List<ChatMessage> findByChatId(final Long chatId, final int page, final int size) {
        return find("chat.id = ?1 ORDER BY createAt DESC", chatId)
                .page(page - 1, size)
                .list();
    }
}
