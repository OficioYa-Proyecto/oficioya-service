package edu.eci.dosw.oficioya.oficioya_service.service;

import org.springframework.stereotype.Service;

import edu.eci.dosw.oficioya.oficioya_service.client.NotificationService;
import edu.eci.dosw.oficioya.oficioya_service.dto.RoleRemovalNotificationRequest;

/**
 * RF-13. Construye el cuerpo de los correos de quita de rol y lo entrega al
 * microservicio de Notification. Users conserva la lógica de la petición; acá
 * sólo se resuelve el texto que recibe cada implicado.
 */
@Service
public class RoleRemovalNotificationService {

    private static final String TYPE_REQUESTED = "ROLE_REMOVAL_REQUESTED";
    private static final String TYPE_APPROVED = "ROLE_REMOVAL_APPROVED";
    private static final String TYPE_REJECTED = "ROLE_REMOVAL_REJECTED";

    private final NotificationService notificationService;

    public RoleRemovalNotificationService(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    public void notify(RoleRemovalNotificationRequest request) {
        String action = request.action() == null ? "" : request.action().trim().toUpperCase();
        switch (action) {
            case "REQUESTED" -> send(TYPE_REQUESTED, request.adminEmail(), request.userId(),
                    requestId(request), "Solicitud para dejar de ser " + request.role(),
                    requestedBody(request));
            case "APPROVED" -> send(TYPE_APPROVED, request.userEmail(), request.userId(),
                    requestId(request), "Rol " + request.role() + " retirado",
                    resolvedBody(request, "aprobada", request.adminReason()));
            case "REJECTED" -> send(TYPE_REJECTED, request.userEmail(), request.userId(),
                    requestId(request), "Solicitud para dejar de ser " + request.role() + " rechazada",
                    resolvedBody(request, "rechazada", request.adminReason()));
            default -> throw new IllegalArgumentException("Acción de quita de rol no soportada: " + request.action());
        }
    }

    private void send(String type, String receiverEmail, java.util.UUID receiverId,
                      java.util.UUID roleRemovalRequestId, String title, String body) {
        notificationService.notifyRoleRemovalEmail(type, receiverId, receiverEmail,
                roleRemovalRequestId, type + " - " + title, body);
    }

    private java.util.UUID requestId(RoleRemovalNotificationRequest request) {
        return request.roleRemovalRequestId() != null
                ? request.roleRemovalRequestId()
                : request.userId();
    }

    private String requestedBody(RoleRemovalNotificationRequest request) {
        return "OficioYa\n"
                + "Solicitud para dejar de ser un rol\n\n"
                + "Usuario: " + value(request.userName()) + " (" + request.userId() + ")\n"
                + "Rol solicitado: " + request.role() + "\n"
                + "Razón: " + value(request.reason()) + "\n"
                + "ID de la petición: " + requestId(request) + "\n\n"
                + "Revisa la petición en el panel de administración.\n\n"
                + "OficioYa";
    }

    private String resolvedBody(RoleRemovalNotificationRequest request, String result, String adminReason) {
        return "OficioYa\n"
                + "Tu petición para dejar de ser " + request.role() + " fue " + result + ".\n\n"
                + "Usuario: " + value(request.userName()) + " (" + request.userId() + ")\n"
                + "Motivo del administrador: " + value(adminReason) + "\n"
                + "ID de la petición: " + requestId(request) + "\n\n"
                + "OficioYa";
    }

    private String value(String text) {
        return text == null || text.isBlank() ? "(no indicado)" : text.trim();
    }
}
