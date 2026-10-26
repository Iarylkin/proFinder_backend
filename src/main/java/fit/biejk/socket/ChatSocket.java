package fit.biejk.socket;

import fit.biejk.converter.ChatMessageDecoder;
import fit.biejk.converter.ChatMessageEncoder;
import fit.biejk.dto.ChatInputMessage;
import fit.biejk.dto.ChatOutputMessage;
import fit.biejk.entity.ChatMessage;
import fit.biejk.mapper.ChatMessageMapper;
import fit.biejk.service.ChatMessageService;
import fit.biejk.service.ChatService;
import fit.biejk.service.UserService;
import io.smallrye.jwt.auth.principal.JWTParser;
import io.smallrye.jwt.auth.principal.ParseException;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.websocket.*;
import jakarta.websocket.server.PathParam;
import jakarta.websocket.server.ServerEndpoint;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.microprofile.context.ManagedExecutor;
import org.eclipse.microprofile.jwt.JsonWebToken;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * WebSocket endpoint for managing chat sessions and message broadcasting.
 */
@Slf4j
@ApplicationScoped
@RequiredArgsConstructor
@ServerEndpoint(
        value = "/v1/chats/{chatId}",
        decoders = ChatMessageDecoder.class,
        encoders = ChatMessageEncoder.class
)
public class ChatSocket {

    /**
     * Parses JWT tokens to extract user information.
     */
    private final JWTParser jwtParser;

    /**
     * Service for creating and storing chat messages.
     */
    private final ChatMessageService chatMessageService;

    /**
     * Executor for running background tasks outside IO thread.
     */
    private final ManagedExecutor executor;

    /**
     * Service for chat-related operations such as validation.
     */
    private final ChatService chatService;

    /**
     * Mapper to convert between entity and DTO for chat messages.
     */
    private final ChatMessageMapper chatMessageMapper;

    /**
     * Service for managing user-related logic.
     */
    private final UserService userService;

    /**
     * Map storing active chat sessions, keyed by chat ID.
     */
    private static final Map<Long, Set<Session>> CHAT_SESSIONS = new ConcurrentHashMap<>();

    /**
     * Handles new WebSocket connections.
     *
     * @param session the WebSocket session
     * @param chatId  ID of the chat the user is joining
     */
    @OnOpen
    public void onOpen(final Session session, @PathParam("chatId") final Long chatId) {
        String token = getTokenFromQuery(session);
        if (token == null) {
            closeSession(session, "Unauthorized");
            return;
        }

        executor.execute(() -> handleOpenSession(token, chatId, session));
    }

    private void handleOpenSession(final String token, final Long chatId, final Session session) {
        Long userId = extractUserId(token);

        if (userId == null || !userService.existById(userId)) {
            closeSession(session, "Unauthorized");
            return;
        }

        if (!chatService.existById(chatId)) {
            closeSession(session, "Chat does not exist");
            return;
        }

        session.getUserProperties().put("userId", userId);
        CHAT_SESSIONS.computeIfAbsent(chatId, k -> ConcurrentHashMap.newKeySet()).add(session);

        log.info("User {} joined chat {}", userId, chatId);
    }

    /**
     * Handles incoming messages from users.
     *
     * @param inputMessage the received message
     * @param session      the session of the sender
     * @param chatId       ID of the chat
     */
    @OnMessage
    public void onMessage(final ChatInputMessage inputMessage, final Session session,
                          @PathParam("chatId") final Long chatId) {
        Long senderId = (Long) session.getUserProperties().get("userId");
        if (senderId == null) {
            log.warn("No userId associated with session: {}", session.getId());
            return;
        }

        executor.execute(() -> handleIncomingMessage(inputMessage, senderId, chatId));
    }

    private void handleIncomingMessage(final ChatInputMessage inputMessage, final Long senderId, final Long chatId) {
        if (!userService.existById(senderId)) {
            return;
        }

        try {
            ChatMessage message = chatMessageService.create(chatId, senderId, inputMessage.getContent());
            ChatOutputMessage outputMessage = chatMessageMapper.toDto(message);
            log.info("Sending message {} in chat {}", message.getId(), chatId);
            broadcastMessage(chatId, outputMessage);
        } catch (Exception e) {
            log.error("Failed to process message", e);
        }
    }

    private void broadcastMessage(final Long chatId, final ChatOutputMessage outputMessage) {
        Set<Session> sessions = CHAT_SESSIONS.getOrDefault(chatId, Set.of());
        for (Session s : sessions) {
            if (s.isOpen()) {
                s.getAsyncRemote().sendObject(outputMessage);
            }
        }
    }

    /**
     * Handles WebSocket disconnections.
     *
     * @param session the WebSocket session
     * @param chatId  ID of the chat
     */
    @OnClose
    public void onClose(final Session session, @PathParam("chatId") final Long chatId) {
        Set<Session> sessions = CHAT_SESSIONS.get(chatId);
        if (sessions != null) {
            sessions.remove(session);
        }
        log.info("User disconnected from chat {}: {}", chatId, session.getId());
    }

    /**
     * Handles WebSocket errors.
     *
     * @param session the WebSocket session
     * @param error   the error occurred
     * @param chatId  ID of the chat
     */
    @OnError
    public void onError(final Session session, final Throwable error, @PathParam("chatId") final Long chatId) {
        log.warn("Error in chat {}: {}", chatId, error.getMessage());
    }

    /**
     * Extracts JWT token from session query string.
     *
     * @param session WebSocket session
     * @return extracted token or null if missing
     */
    private String getTokenFromQuery(final Session session) {
        String query = session.getQueryString();
        if (query == null) {
            return null;
        }
        for (String param : query.split("&")) {
            if (param.startsWith("token=")) {
                return param.substring("token=".length());
            }
        }
        return null;
    }

    /**
     * Closes the WebSocket session with the given reason.
     *
     * @param session session to close
     * @param reason  reason for closing
     */
    private void closeSession(final Session session, final String reason) {
        try {
            session.close(new CloseReason(CloseReason.CloseCodes.VIOLATED_POLICY, reason));
        } catch (Exception ignored) {
            log.warn("Failed to close session {}: {}", session.getId());
        }
        log.warn("Closed session {}: {}", session.getId(), reason);
    }

    /**
     * Extracts user ID from a JWT token.
     *
     * @param token JWT token
     * @return extracted user ID
     * @throws IllegalArgumentException if token is invalid
     */
    private Long extractUserId(final String token) {
        try {
            JsonWebToken jwt = jwtParser.parse(token);
            return Long.parseLong(jwt.getSubject());
        } catch (ParseException e) {
            throw new IllegalArgumentException("Invalid JWT", e);
        }
    }
}
