package edu.eci.dosw.oficioya.oficioya_service.client;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import edu.eci.dosw.oficioya.oficioya_service.security.JwtUtil;

class EmailActionLinkServiceTest {

    private static final String SECRET = "una-clave-de-prueba-de-al-menos-32-caracteres-123456";
    private final JwtUtil jwtUtil = new JwtUtil(SECRET, "oficioya", 30);
    private final EmailActionLinkService links =
            new EmailActionLinkService(jwtUtil, "https://service.test/");

    @Test
    @DisplayName("Construye enlaces de aceptar y rechazar con token firmado")
    void construyeEnlacesFirmados() {
        UUID workerId = UUID.randomUUID();
        UUID requestId = UUID.randomUUID();

        String accept = links.acceptUrl(workerId, requestId);
        String reject = links.rejectUrl(workerId, requestId);

        assertTrue(accept.startsWith("https://service.test/api/v1/requests/email/accept?requestId=" + requestId));
        assertTrue(reject.startsWith("https://service.test/api/v1/requests/email/reject?requestId=" + requestId));
        assertTrue(accept.contains("&token="));
        assertTrue(reject.contains("&token="));
    }
}
