package edu.eci.dosw.oficioya.oficioya_service.client;

import java.time.LocalDateTime;

/**
 * Contexto de la solicitud que Notification usa para armar el correo.
 */
public record RequestNotificationDetails(
        LocalDateTime scheduledDateTime,
        String neighborhood,
        String exactAddress,
        String description
) {
}
