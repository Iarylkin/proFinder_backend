package io.github.strogolsky.profinder.auth.service;

import io.github.strogolsky.profinder.auth.entity.User;
import io.github.strogolsky.profinder.auth.exception.EmailAlreadyRegisteredException;
import io.github.strogolsky.profinder.auth.repository.UserRepository;
import io.github.strogolsky.profinder.auth.utilits.CryptoUtils;
import io.github.strogolsky.profinder.common.event.UserDeleted;
import io.github.strogolsky.profinder.notification.service.MailService;
import jakarta.enterprise.event.Event;
import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.NotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * The account of a user: reading it, changing the password and the email, and deleting it. An email is
 * matched exactly as it was sent: {@code A@x.com} and {@code a@x.com} are two different addresses.
 */
class UserServiceAccountTest {

    private static final UUID USER_ID = UUID.randomUUID();
    private static final String EMAIL = "User@Example.com";
    private static final String PASSWORD = "Passw0rd!x1";
    private static final String NEW_PASSWORD = "NewPassw0rd!x";
    private static final String NEW_EMAIL = "new@example.com";

    private UserRepository userRepository;
    private Event<UserDeleted> userDeleted;
    private MailService mailService;
    private UserService service;

    private User user;

    @SuppressWarnings("unchecked")
    @BeforeEach
    void setUp() {
        userRepository = mock(UserRepository.class);
        userDeleted = mock(Event.class);
        mailService = mock(MailService.class);
        service = new UserService(userRepository, userDeleted, mailService);

        user = new User();
        user.setId(USER_ID);
        user.setEmail(EMAIL);
        user.setPassword(CryptoUtils.hash(PASSWORD));

        when(userRepository.findById(USER_ID)).thenReturn(user);
        when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.of(user));
        when(userRepository.findByEmail(NEW_EMAIL)).thenReturn(Optional.empty());
    }

    // ---- read ----

    @Test
    void userIsFoundByEmailExactlyAsSent() {
        assertEquals(Optional.of(user), service.findByEmail(EMAIL));
        assertEquals(Optional.empty(), service.findByEmail("user@example.com"));
    }

    @Test
    void userIsReadByEmail() {
        assertSame(user, service.getByEmail(EMAIL));
    }

    @Test
    void unknownEmailIsNotFound() {
        assertThrows(NotFoundException.class, () -> service.getByEmail("nobody@example.com"));
    }

    @Test
    void userIsReadById() {
        assertSame(user, service.getById(USER_ID));
    }

    @Test
    void unknownIdIsNotFound() {
        UUID unknownId = UUID.randomUUID();
        assertThrows(NotFoundException.class, () -> service.getById(unknownId));
    }

    @Test
    void existenceIsAnswerWithoutAnError() {
        assertTrue(service.existById(USER_ID));
        assertFalse(service.existById(UUID.randomUUID()));
    }

    @Test
    void takenEmailIsNotUnique() {
        assertThrows(EmailAlreadyRegisteredException.class, () -> service.checkUniqueEmail(EMAIL));
    }

    @Test
    void freeEmailIsUnique() {
        service.checkUniqueEmail(NEW_EMAIL);
    }

    // ---- passwords ----

    @Test
    void newPasswordIsStoredOnlyAsAHash() {
        User updated = service.updatePassword(USER_ID, NEW_PASSWORD);

        assertNotEquals(NEW_PASSWORD, updated.getPassword());
        assertTrue(CryptoUtils.verify(NEW_PASSWORD, updated.getPassword()));
        assertFalse(CryptoUtils.verify(PASSWORD, updated.getPassword()));
    }

    @Test
    void passwordIsChangedWhenTheOldOneIsRightAndTheNewOnesMatch() {
        User updated = service.changePassword(USER_ID, PASSWORD, NEW_PASSWORD, NEW_PASSWORD);

        assertTrue(CryptoUtils.verify(NEW_PASSWORD, updated.getPassword()));
    }

    @Test
    void wrongOldPasswordIsRejectedAndNothingChanges() {
        String before = user.getPassword();

        assertThrows(BadRequestException.class,
                () -> service.changePassword(USER_ID, "Wrong0rd!x1", NEW_PASSWORD, NEW_PASSWORD));

        assertEquals(before, user.getPassword());
    }

    @Test
    void differentNewPasswordsAreRejectedAndNothingChanges() {
        String before = user.getPassword();

        assertThrows(BadRequestException.class,
                () -> service.changePassword(USER_ID, PASSWORD, NEW_PASSWORD, "Other0rd!xx"));

        assertEquals(before, user.getPassword());
    }

    @Test
    void passwordOfAMissingUserIsNotFound() {
        UUID unknownId = UUID.randomUUID();
        assertThrows(NotFoundException.class,
                () -> service.changePassword(unknownId, PASSWORD, NEW_PASSWORD, NEW_PASSWORD));
    }

    // ---- email ----

    @Test
    void emailIsChangedAndTheOldAddressIsToldAboutIt() {
        User updated = service.changeEmail(USER_ID, NEW_EMAIL, PASSWORD);

        assertEquals(NEW_EMAIL, updated.getEmail());
        ArgumentCaptor<String> message = ArgumentCaptor.forClass(String.class);
        verify(mailService).send(eq(EMAIL), anyString(), message.capture());
        assertTrue(message.getValue().contains(NEW_EMAIL));
    }

    @Test
    void theSameEmailIsRejected() {
        assertThrows(BadRequestException.class, () -> service.changeEmail(USER_ID, EMAIL, PASSWORD));

        assertEquals(EMAIL, user.getEmail());
        verify(mailService, never()).send(anyString(), anyString(), anyString());
    }

    @Test
    void theSameLettersInAnotherCaseAreAnotherAddressAndAreAccepted() {
        when(userRepository.findByEmail("user@example.com")).thenReturn(Optional.empty());

        User updated = service.changeEmail(USER_ID, "user@example.com", PASSWORD);

        assertEquals("user@example.com", updated.getEmail());
    }

    @Test
    void wrongPasswordRejectsTheEmailChange() {
        assertThrows(BadRequestException.class, () -> service.changeEmail(USER_ID, NEW_EMAIL, "Wrong0rd!x1"));

        assertEquals(EMAIL, user.getEmail());
        verify(mailService, never()).send(anyString(), anyString(), anyString());
    }

    @Test
    void anEmailOfAnotherAccountIsRejected() {
        when(userRepository.findByEmail(NEW_EMAIL)).thenReturn(Optional.of(new User()));

        assertThrows(EmailAlreadyRegisteredException.class,
                () -> service.changeEmail(USER_ID, NEW_EMAIL, PASSWORD));

        assertEquals(EMAIL, user.getEmail());
        verify(mailService, never()).send(anyString(), anyString(), anyString());
    }

    @Test
    void emailOfAMissingUserIsNotFound() {
        UUID unknownId = UUID.randomUUID();
        assertThrows(NotFoundException.class, () -> service.changeEmail(unknownId, NEW_EMAIL, PASSWORD));
    }

    // ---- delete ----

    @Test
    void deletingAMissingUserIsNotFoundAndAnnouncesNothing() {
        UUID unknownId = UUID.randomUUID();
        assertThrows(NotFoundException.class, () -> service.delete(unknownId));

        verify(userDeleted, never()).fire(any(UserDeleted.class));
    }
}
