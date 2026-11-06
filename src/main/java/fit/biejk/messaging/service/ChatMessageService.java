package fit.biejk.messaging.service;

import fit.biejk.auth.entity.User;
import fit.biejk.auth.service.UserService;
import fit.biejk.messaging.entity.Chat;
import fit.biejk.messaging.entity.ChatMessage;
import fit.biejk.messaging.repository.ChatMessageRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

import java.util.UUID;

/**
 * Service class responsible for creating chat messages.
 */
@ApplicationScoped
@RequiredArgsConstructor
public class ChatMessageService {

    /**
     * Repository for performing database operations on {@link ChatMessage} entities.
     */
    private final ChatMessageRepository chatMessageRepository;

    /**
     * Service used for retrieving user information.
     */
    private final UserService userService;

    /**
     * Service used for retrieving chat information.
     */
    private final ChatService chatService;

    /**
     * Creates and persists a new {@link ChatMessage} entity in the specified chat.
     *
     * @param chatId  the ID of the chat to which the message belongs
     * @param fromId  the ID of the user sending the message
     * @param content the content of the message
     * @return the newly created {@link ChatMessage}
     */
    @Transactional
    public ChatMessage create(final UUID chatId, final UUID fromId, final String content) {
        User fromUser = userService.getById(fromId);
        Chat chat = chatService.getById(chatId);

        ChatMessage chatMessage = new ChatMessage();
        chatMessage.setContent(content);
        chatMessage.setSender(fromUser);
        chatMessage.setChat(chat);

        chatMessageRepository.persist(chatMessage);

        return chatMessage;
    }
}
