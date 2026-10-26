package fit.biejk.service;

import fit.biejk.entity.Chat;
import fit.biejk.entity.ChatMessage;
import fit.biejk.entity.User;
import fit.biejk.repository.ChatMessageRepository;
import fit.biejk.repository.ChatRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.ForbiddenException;
import jakarta.ws.rs.NotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.List;

/**
 * Service for handling chat operations, such as creating new chats,
 * retrieving chat history, and checking chat existence.
 */
@RequiredArgsConstructor
@ApplicationScoped
@Slf4j
public class ChatService {

    /**
     * Repository for performing CRUD operations on {@link Chat} entities.
     */
    private final ChatRepository chatRepository;

    /**
     * Repository for performing CRUD operations on {@link ChatMessage} entities.
     */
    private final ChatMessageRepository chatMessageRepository;

    /**
     * Service for user-related operations.
     */
    private final UserService userService;

    /**
     * Creates a new chat between two users if it doesn't already exist.
     *
     * @param userId1 ID of the first user
     * @param userId2 ID of the second user
     * @return the newly created {@link Chat} entity
     * @throws IllegalArgumentException if both user IDs are the same or chat already exists
     */
    @Transactional
    public Chat create(final Long userId1, final Long userId2) {
        log.info("Creating new chat");
        Long first = Math.min(userId1, userId2);
        Long second = Math.max(userId1, userId2);
        if (first.equals(second)) {
            log.warn("First and Second chat id {} are the same", first);
            throw new IllegalArgumentException("first and second are the same");
        }
        if (existByUsersId(first, second)) {
            log.info("Chat id {} already exists", first);
            return getByUsersId(first, second);
        }
        User user1 = userService.getById(first);
        User user2 = userService.getById(second);

        Chat chat = new Chat();
        chat.setUser1(user1);
        chat.setUser2(user2);

        chatRepository.persist(chat);
        log.info("Created chat with id: {}", chat.getId());
        return chat;
    }

    /**
     * Retrieves a chat by its ID.
     *
     * @param chatId ID of the chat to retrieve
     * @return the found {@link Chat} entity
     * @throws NotFoundException if no chat is found with the given ID
     */
    public Chat getById(final Long chatId) {
        Chat result = chatRepository.findById(chatId);
        if (result == null) {
            throw new NotFoundException("Chat with id " + chatId + " not found");
        }
        return result;
    }

    /**
     * Retrieves the list of messages in a given chat.
     *
     * @param chatId ID of the chat
     * @param userId ID of the user requesting the history
     * @param page   page number for pagination
     * @param size   number of messages per page
     * @return list of {@link ChatMessage} in the chat
     * @throws NotFoundException  if the chat does not exist
     * @throws ForbiddenException if the user is not a participant of the chat
     */
    public List<ChatMessage> getMessagesById(final Long chatId, final Long userId,
                                             final int page, final int size) {
        log.info("Getting history for chat {}", chatId);

        Chat chat = getById(chatId);
        if (!chat.hasParticipant(userId)) {
            log.warn("User {} is not a participant of chat {}", userId, chatId);
            throw new ForbiddenException("You are not a participant of this chat");
        }

        return chatMessageRepository.findByChatId(chatId, page, size);
    }

    /**
     * Checks whether a user is a participant of a chat.
     *
     * @param chatId ID of the chat
     * @param userId ID of the user
     * @return true if the chat exists and the user is one of its participants
     */
    public boolean isParticipant(final Long chatId, final Long userId) {
        Chat chat = chatRepository.findById(chatId);
        return chat != null && chat.hasParticipant(userId);
    }

    /**
     * Checks if a chat with the given ID exists.
     *
     * @param chatId ID of the chat
     * @return true if chat exists, false otherwise
     */
    public boolean existById(final Long chatId) {
        Chat chat = chatRepository.findById(chatId);
        if (chat == null) {
            log.warn("Chat with id {} not found", chatId);
            return false;
        }
        log.info("Chat with id {} found", chatId);
        return true;
    }

    /**
     * Checks if a chat exists between two users.
     *
     * @param userId1 ID of the first user
     * @param userId2 ID of the second user
     * @return true if a chat exists, false otherwise
     */
    public boolean existByUsersId(final Long userId1, final Long userId2) {
        Chat chat  = chatRepository.findByUsersId(userId1, userId2);
        if (chat == null) {
            log.info("Chat with user id {} and {} not found", userId1, userId2);
            return false;
        }
        log.info("Chat with user id {} and {} found", userId1, userId2);
        return true;
    }

    /**
     * Retrieves all chats that a user participates in.
     *
     * @param userId ID of the user
     * @param page   page number for pagination
     * @param size   number of chats per page
     * @return list of {@link Chat} entities
     */
    public List<Chat> getByUserId(final Long userId, final int page, final int size) {
        return chatRepository.findByUserId(userId, page, size);
    }
    /**
     * Retrieves a chat between two users if it exists.
     *
     * @param userId1 ID of the first user
     * @param userId2 ID of the second user
     * @return the {@link Chat} entity between the two users, or {@code null} if not found
     */
    public Chat getByUsersId(final Long userId1, final Long userId2) {
        return chatRepository.findByUsersId(userId1, userId2);
    }

}
