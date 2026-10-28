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
    private static final Map<UUID, Set<Session>> CHAT_SESSIONS = new ConcurrentHashMap<>();

    /**
     * Handles new WebSocket connections.
     *
     * @param session the WebSocket session
     * @param chatIdParam ID of the chat the user is joining, as it stands in the path
     */
    @OnOpen
    public void onOpen(final Session session, @PathParam("chatId") final String chatIdParam) {
        String token = getTokenFromQuery(session);
        if (token == null) {
            closeSession(session, "Unauthorized");
            return;
        }
        UUID chatId = parseChatId(chatIdParam);
        if (chatId == null) {
            closeSession(session, "Chat does not exist");
            return;
        }

        executor.execute(() -> handleOpenSession(token, chatId, session));
    }

    private void handleOpenSession(final String token, final UUID chatId, final Session session) {
        UUID userId = extractUserId(token);

        if (userId == null || !userService.existById(userId)) {
            closeSession(session, "Unauthorized");
            return;
        }

        if (!chatService.existById(chatId)) {
            closeSession(session, "Chat does not exist");
            return;
        }

        if (!chatService.isParticipant(chatId, userId)) {
            log.warn("User {} tried to join chat {} without being a participant", userId, chatId);
            closeSession(session, "Forbidden");
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
     * @param chatIdParam  ID of the chat, as it stands in the path
     */
    @OnMessage
    public void onMessage(final ChatInputMessage inputMessage, final Session session,
                          @PathParam("chatId") final String chatIdParam) {
        UUID chatId = parseChatId(chatIdParam);
        UUID senderId = (UUID) session.getUserProperties().get("userId");
        if (chatId == null || senderId == null) {
            log.warn("No userId associated with session: {}", session.getId());
            return;
        }

        executor.execute(() -> handleIncomingMessage(inputMessage, senderId, chatId));
    }

    private void handleIncomingMessage(final ChatInputMessage inputMessage, final UUID senderId, final UUID chatId) {
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

    private void broadcastMessage(final UUID chatId, final ChatOutputMessage outputMessage) {
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
     * @param chatIdParam ID of the chat, as it stands in the path
     */
    @OnClose
    public void onClose(final Session session, @PathParam("chatId") final String chatIdParam) {
        UUID chatId = parseChatId(chatIdParam);
        Set<Session> sessions = chatId == null ? null : CHAT_SESSIONS.get(chatId);
        if (sessions != null) {
            sessions.remove(session);
        }
        log.info("User disconnected from chat {}: {}", chatIdParam, session.getId());
    }

    /**
     * Handles WebSocket errors.
     *
     * @param session the WebSocket session
     * @param error   the error occurred
     * @param chatIdParam ID of the chat, as it stands in the path
     */
    @OnError
    public void onError(final Session session, final Throwable error,
                        @PathParam("chatId") final String chatIdParam) {
        log.warn("Error in chat {}: {}", chatIdParam, error.getMessage());
    }

    /**
     * Parses the chat ID from the path.
     * <p>
     * WebSocket path parameters cannot be bound to a {@link UUID}, so they arrive as text.
     * </p>
     *
     * @param chatIdParam the path parameter
     * @return the chat ID, or {@code null} if it is not a valid UUID
     */
    private static UUID parseChatId(final String chatIdParam) {
        try {
            return UUID.fromString(chatIdParam);
        } catch (IllegalArgumentException e) {
            return null;
        }
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
    private UUID extractUserId(final String token) {
        try {
            JsonWebToken jwt = jwtParser.parse(token);
            return UUID.fromString(jwt.getSubject());
        } catch (ParseException e) {
            throw new IllegalArgumentException("Invalid JWT", e);
        }
    }
}
