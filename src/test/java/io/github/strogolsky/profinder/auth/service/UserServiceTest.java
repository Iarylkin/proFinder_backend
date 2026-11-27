package io.github.strogolsky.profinder.auth.service;

import io.github.strogolsky.profinder.auth.entity.User;
import io.github.strogolsky.profinder.auth.exception.EmailAlreadyRegisteredException;
import io.github.strogolsky.profinder.auth.repository.UserRepository;
import io.github.strogolsky.profinder.common.event.UserDeleted;
import io.github.strogolsky.profinder.notification.service.MailService;
import jakarta.enterprise.event.Event;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class UserServiceTest {

    private UserRepository userRepository;
    private Event<UserDeleted> userDeleted;
    private UserService service;

    @SuppressWarnings("unchecked")
    @BeforeEach
    void setUp() {
        userRepository = mock(UserRepository.class);
        userDeleted = mock(Event.class);
        service = new UserService(userRepository, userDeleted, mock(MailService.class));
    }

    @Test
    void createPersistsTheUser() {
        User created = new User();
        created.setEmail("new@example.com");
        when(userRepository.findByEmail("new@example.com")).thenReturn(Optional.empty());

        User result = service.create(created);

        assertSame(created, result);
        verify(userRepository).persist(created);
    }

    @Test
    void createRejectsAnEmailThatIsAlreadyRegistered() {
        User created = new User();
        created.setEmail("taken@example.com");
        when(userRepository.findByEmail("taken@example.com")).thenReturn(Optional.of(new User()));

        assertThrows(EmailAlreadyRegisteredException.class, () -> service.create(created));

        verify(userRepository, never()).persist(created);
    }

    @Test
    void deleteAnnouncesTheDeletedUser() {
        UUID id = UUID.randomUUID();
        User existing = new User();
        when(userRepository.findById(id)).thenReturn(existing);

        service.delete(id);

        verify(userRepository).delete(existing);
        verify(userDeleted).fire(new UserDeleted(id));
    }
}
