package edu.eci.dosw.oficioya.oficioya_service.client;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Contexto de la solicitud que Notification usa para armar el correo.
 *
 * La foto viaja tambien como base64 para que Notification la incruste en el
 * correo (CID). La URL de Images se mantiene como respaldo para productores
 * que todavia no adjuntan bytes. Las fotos adicionales alimentan el carrusel.
 */
public record RequestNotificationDetails(
        LocalDateTime scheduledDateTime,
        String neighborhood,
        String exactAddress,
        String description,
        String imageUrl,
        String imageBase64,
        String imageContentType,
        List<RequestNotificationPhoto> photos
) {
    public RequestNotificationDetails(LocalDateTime scheduledDateTime, String neighborhood,
                                      String exactAddress, String description) {
        this(scheduledDateTime, neighborhood, exactAddress, description, null, null, null, List.of());
    }

    public RequestNotificationDetails(LocalDateTime scheduledDateTime, String neighborhood,
                                      String exactAddress, String description, String imageUrl) {
        this(scheduledDateTime, neighborhood, exactAddress, description, imageUrl, null, null, List.of());
    }

    public RequestNotificationDetails(LocalDateTime scheduledDateTime, String neighborhood,
                                      String exactAddress, String description, String imageUrl,
                                      String imageBase64, String imageContentType) {
        this(scheduledDateTime, neighborhood, exactAddress, description, imageUrl, imageBase64,
                imageContentType, List.of());
    }
}
