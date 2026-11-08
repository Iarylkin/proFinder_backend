package io.github.strogolsky.profinder.messaging.socket;

import io.github.strogolsky.profinder.messaging.mapper.ChatMessageMapper;
import io.github.strogolsky.profinder.messaging.service.ChatMessageService;
import io.github.strogolsky.profinder.messaging.service.ChatService;
import io.smallrye.jwt.auth.principal.JWTParser;
import io.smallrye.jwt.auth.principal.ParseException;
import jakarta.websocket.CloseReason;
import jakarta.websocket.Session;
import org.eclipse.microprofile.context.ManagedExecutor;
import org.eclipse.microprofile.jwt.JsonWebToken;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ChatSocketTest {

    private static final UUID CHAT_ID = UUID.randomUUID();
    private static final UUID USER_ID = UUID.randomUUID();

    private JWTParser jwtParser;
    private ChatService chatService;
    private Session session;
    private Map<String, Object> userProperties;
    private ChatSocket socket;

    @BeforeEach
    void setUp() {
        jwtParser = mock(JWTParser.class);
        chatService = mock(ChatService.class);
        ManagedExecutor executor = mock(ManagedExecutor.class);
        doAnswer(call -> {
            ((Runnable) call.getArgument(0)).run();
            return null;
        }).when(executor).execute(any(Runnable.class));
        socket = new ChatSocket(jwtParser, mock(ChatMessageService.class), executor, chatService,
                mock(ChatMessageMapper.class));

        session = mock(Session.class);
        userProperties = new HashMap<>();
        when(session.getUserProperties()).thenReturn(userProperties);
        when(session.getQueryString()).thenReturn("token=the-token");
    }

    @Test
    void anInvalidTokenClosesTheSession() throws Exception {
        when(jwtParser.parse("the-token")).thenThrow(new ParseException("bad token"));

        socket.onOpen(session, CHAT_ID.toString());

        assertClosedWith("Unauthorized");
    }

    @Test
    void aSubjectThatIsNotAnIdClosesTheSession() throws Exception {
        returnTokenOf("not-a-uuid");

        socket.onOpen(session, CHAT_ID.toString());

        assertClosedWith("Unauthorized");
    }

    @Test
    void aTokenWithoutSubjectClosesTheSession() throws Exception {
        returnTokenOf(null);

        socket.onOpen(session, CHAT_ID.toString());

        assertClosedWith("Unauthorized");
    }

    @Test
    void aUserWhoIsNotAParticipantIsRefused() throws Exception {
        returnTokenOf(USER_ID.toString());
        when(chatService.existById(CHAT_ID)).thenReturn(true);
        when(chatService.isParticipant(CHAT_ID, USER_ID)).thenReturn(false);

        socket.onOpen(session, CHAT_ID.toString());

        assertClosedWith("Forbidden");
        assertFalse(userProperties.containsKey("userId"));
    }

    @Test
    void aParticipantJoinsTheChat() throws Exception {
        returnTokenOf(USER_ID.toString());
        when(chatService.existById(CHAT_ID)).thenReturn(true);
        when(chatService.isParticipant(CHAT_ID, USER_ID)).thenReturn(true);

        socket.onOpen(session, CHAT_ID.toString());

        assertEquals(USER_ID, userProperties.get("userId"));
        verify(session, never()).close(any());
    }

    private void returnTokenOf(final String subject) throws ParseException {
        JsonWebToken jwt = mock(JsonWebToken.class);
        when(jwt.getSubject()).thenReturn(subject);
        when(jwtParser.parse("the-token")).thenReturn(jwt);
    }

    private void assertClosedWith(final String reason) throws IOException {
        ArgumentCaptor<CloseReason> captor = ArgumentCaptor.forClass(CloseReason.class);
        verify(session).close(captor.capture());
        assertEquals(reason, captor.getValue().getReasonPhrase());
        assertEquals(CloseReason.CloseCodes.VIOLATED_POLICY, captor.getValue().getCloseCode());
    }
}
