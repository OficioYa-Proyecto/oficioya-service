package edu.eci.dosw.oficioya.oficioya_service.security;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.Authentication;

class CurrentUserTest {

    @Test
    @DisplayName("obtiene el id del claim sub")
    void id() {
        Authentication authentication = mock(Authentication.class);
        UUID id = UUID.randomUUID();
        when(authentication.getName()).thenReturn(id.toString());

        assertEquals(id, CurrentUser.id(authentication));
    }

    @Test
    @DisplayName("obtiene el token JWT de las credentials")
    void token() {
        Authentication authentication = mock(Authentication.class);
        when(authentication.getCredentials()).thenReturn("jwt-token");

        assertEquals("jwt-token", CurrentUser.token(authentication));
    }

    @Test
    @DisplayName("devuelve null si no hay autenticación o el token no es texto")
    void tokenInvalido() {
        assertNull(CurrentUser.token(null));

        Authentication authentication = mock(Authentication.class);
        when(authentication.getCredentials()).thenReturn(new Object());
        assertNull(CurrentUser.token(authentication));
    }
}
