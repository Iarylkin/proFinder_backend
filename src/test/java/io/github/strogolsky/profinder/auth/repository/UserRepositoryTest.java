package io.github.strogolsky.profinder.auth.repository;

import io.github.strogolsky.profinder.auth.entity.User;
import io.github.strogolsky.profinder.auth.entity.UserRole;
import io.quarkus.test.TestTransaction;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Runs on a real PostgreSQL database of the {@code auth} module: the email is looked up exactly as it was saved.
 */
@QuarkusTest
class UserRepositoryTest {

    @Inject
    UserRepository repository;

    private User saved(final String email) {
        User user = new User();
        user.setEmail(email);
        user.setPassword("hash");
        user.setRole(UserRole.CUSTOMER);
        repository.persist(user);
        return user;
    }

    @Test
    @TestTransaction
    void userIsFoundByItsEmail() {
        User user = saved("Anna@Example.com");

        Optional<User> found = repository.findByEmail("Anna@Example.com");

        assertTrue(found.isPresent());
        assertEquals(user.getId(), found.get().getId());
    }

    @Test
    @TestTransaction
    void emailInAnotherCaseIsNotTheSameUser() {
        saved("Anna@Example.com");

        assertTrue(repository.findByEmail("anna@example.com").isEmpty());
    }

    @Test
    @TestTransaction
    void unknownEmailIsNotFound() {
        assertTrue(repository.findByEmail("nobody@example.com").isEmpty());
    }
}
