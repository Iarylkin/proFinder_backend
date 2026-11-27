package io.github.strogolsky.profinder.messaging.service;

import io.github.strogolsky.profinder.auth.service.UserService;
import io.github.strogolsky.profinder.common.event.UserDeleted;
import io.github.strogolsky.profinder.messaging.entity.Chat;
import io.github.strogolsky.profinder.messaging.repository.ChatMessageRepository;
import io.github.strogolsky.profinder.messaging.repository.ChatRepository;
import jakarta.ws.rs.NotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ChatServiceTest {

    private ChatRepository chatRepository;
    private UserService userService;
    private ChatService service;

    @BeforeEach
    void setUp() {
        chatRepository = mock(ChatRepository.class);
        userService = mock(UserService.class);
        service = new ChatService(chatRepository, mock(ChatMessageRepository.class), userService);
    }

    @Test
    void chatKeepsTheIdsOfTheTwoUsersInOrder() {
        UUID a = UUID.randomUUID();
        UUID b = UUID.randomUUID();
        when(userService.existById(a)).thenReturn(true);
        when(userService.existById(b)).thenReturn(true);

        Chat chat = service.create(a, b);

        UUID first = a.compareTo(b) <= 0 ? a : b;
        assertEquals(first, chat.getUser1Id());
        assertEquals(first.equals(a) ? b : a, chat.getUser2Id());
        verify(chatRepository).persist(chat);
    }

    @Test
    void chatWithAMissingUserIsRejected() {
        UUID a = UUID.randomUUID();
        UUID b = UUID.randomUUID();
        when(userService.existById(a)).thenReturn(true);
        when(userService.existById(b)).thenReturn(false);

        assertThrows(NotFoundException.class, () -> service.create(a, b));
        verify(chatRepository, never()).persist(any(Chat.class));
    }

    @Test
    void chatsOfADeletedUserAreRemoved() {
        UUID userId = UUID.randomUUID();
        Chat chat = new Chat();
        when(chatRepository.list("user1Id = ?1 OR user2Id = ?1", userId)).thenReturn(List.of(chat));

        service.onUserDeleted(new UserDeleted(userId));

        verify(chatRepository).delete(chat);
    }
}
