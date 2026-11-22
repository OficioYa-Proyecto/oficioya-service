package edu.eci.dosw.oficioya.oficioya_service.security;

import java.util.UUID;

import org.springframework.security.core.Authentication;

/**
 * Punto único para obtener el id (UUID) del usuario autenticado.
 * El id viaja en el "sub" del JWT y JwtAuthFilter lo deja como nombre del Authentication.
 */
public final class CurrentUser {

    private CurrentUser() {
    }

    public static UUID id(Authentication authentication) {
        return UUID.fromString(authentication.getName());
    }

    /**
     * Token JWT original, guardado como credentials por JwtAuthFilter.
     * Se reenvía al Orchestrator para que el microservicio de imágenes conozca al usuario.
     */
    public static String token(Authentication authentication) {
        if (authentication == null) {
            return null;
        }
        Object credentials = authentication.getCredentials();
        return credentials instanceof String value && !value.isBlank() ? value : null;
    }
}
