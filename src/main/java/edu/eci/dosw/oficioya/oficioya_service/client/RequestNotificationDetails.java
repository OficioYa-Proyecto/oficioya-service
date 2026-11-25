package edu.eci.dosw.oficioya.oficioya_service.client;

import java.time.LocalDateTime;

/**
 * Contexto de la solicitud que Notification usa para armar el correo.
 *
 * La foto viaja tambien como base64 para que Notification la incruste en el
 * correo (CID). La URL de Images se mantiene como respaldo para productores
 * que todavia no adjuntan bytes.
 */
public record RequestNotificationDetails(
        LocalDateTime scheduledDateTime,
        String neighborhood,
        String exactAddress,
        String description,
        String imageUrl,
        String imageBase64,
        String imageContentType
) {
    public RequestNotificationDetails(LocalDateTime scheduledDateTime, String neighborhood,
                                      String exactAddress, String description) {
        this(scheduledDateTime, neighborhood, exactAddress, description, null, null, null);
    }

    public RequestNotificationDetails(LocalDateTime scheduledDateTime, String neighborhood,
                                      String exactAddress, String description, String imageUrl) {
        this(scheduledDateTime, neighborhood, exactAddress, description, imageUrl, null, null);
    }
}
