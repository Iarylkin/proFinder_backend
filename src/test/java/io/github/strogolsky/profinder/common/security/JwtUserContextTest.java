package io.github.strogolsky.profinder.common.security;

import io.quarkus.security.identity.SecurityIdentity;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.security.Principal;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class JwtUserContextTest {

    private static final UUID USER_ID = UUID.randomUUID();

    private SecurityIdentity identity;
    private Principal principal;
    private JwtUserContext context;

    @BeforeEach
    void setUp() {
        identity = mock(SecurityIdentity.class);
        principal = mock(Principal.class);
        when(identity.getPrincipal()).thenReturn(principal);
        when(principal.getName()).thenReturn(USER_ID.toString());
        context = new JwtUserContext(identity);
    }

    @Test
    void idIsTheSubjectOfTheToken() {
        assertEquals(USER_ID, context.id());
    }

    @Test
    void isComparesWithTheUserOfTheToken() {
        assertTrue(context.is(USER_ID));
        assertFalse(context.is(UUID.randomUUID()));
    }

    @Test
    void aSubjectThatIsNotAnIdIsAnError() {
        when(principal.getName()).thenReturn("not-a-uuid");

        assertThrows(IllegalArgumentException.class, () -> context.id());
    }
}
