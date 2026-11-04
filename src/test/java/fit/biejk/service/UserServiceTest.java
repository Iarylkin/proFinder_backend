package fit.biejk.service;

import fit.biejk.entity.User;
import fit.biejk.exception.EmailAlreadyRegisteredException;
import fit.biejk.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class UserServiceTest {

    private UserRepository userRepository;
    private UserService service;

    @BeforeEach
    void setUp() {
        userRepository = mock(UserRepository.class);
        service = new UserService(userRepository, mock(MailService.class));
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
}
