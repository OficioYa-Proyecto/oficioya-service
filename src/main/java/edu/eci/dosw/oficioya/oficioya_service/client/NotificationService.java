package edu.eci.dosw.oficioya.oficioya_service.client;

import java.util.UUID;

public interface NotificationService {

    void notifyNewRequest(UUID workerId, UUID requestId);

    /**
     * Variante con correo y contexto de la solicitud. Las implementaciones que
     * todavía no usan el contexto siguen recibiendo la notificación básica.
     */
    default void notifyNewRequest(UUID workerId, UUID requestId, String receiverEmail,
                                  RequestNotificationDetails details) {
        notifyNewRequest(workerId, requestId);
    }

    void notifyRequestCreated(UUID contractorId, UUID requestId, String receiverEmail);

    default void notifyRequestCreated(UUID contractorId, UUID requestId, String receiverEmail,
                                      RequestNotificationDetails details) {
        notifyRequestCreated(contractorId, requestId, receiverEmail);
    }

    void notifyAccepted(UUID receiverId, UUID requestId);
    void notifyCancelled(UUID receiverId, UUID requestId);
    void notifyCompleted(UUID receiverId, UUID requestId);
    void notifyRejected(UUID receiverId, UUID requestId);

    /**
     * Variantes con correo del destinatario. Las implementaciones que no envían
     * correo siguen recibiendo la notificación básica.
     */
    default void notifyAccepted(UUID receiverId, UUID requestId, String receiverEmail) {
        notifyAccepted(receiverId, requestId);
    }

    default void notifyRejected(UUID receiverId, UUID requestId, String receiverEmail) {
        notifyRejected(receiverId, requestId);
    }

    default void notifyCompleted(UUID receiverId, UUID requestId, String receiverEmail) {
        notifyCompleted(receiverId, requestId);
    }

    default void notifyCancelled(UUID receiverId, UUID requestId, String receiverEmail) {
        notifyCancelled(receiverId, requestId);
    }

    /**
     * Permite distinguir quién canceló: Notification tiene un tipo por lado.
     * Por defecto se mantiene el tipo histórico de cancelación por contratante.
     */
    default void notifyCancelled(UUID receiverId, UUID requestId, String receiverEmail, String type) {
        notifyCancelled(receiverId, requestId, receiverEmail);
    }

    /**
     * Variantes con el contexto de la solicitud para que el correo de cambio de
     * estado muestre la misma información que el correo de entrada.
     */
    default void notifyAccepted(UUID receiverId, UUID requestId, String receiverEmail,
                                RequestNotificationDetails details) {
        notifyAccepted(receiverId, requestId, receiverEmail);
    }

    default void notifyCancelled(UUID receiverId, UUID requestId, String receiverEmail,
                                 String type, RequestNotificationDetails details) {
        notifyCancelled(receiverId, requestId, receiverEmail, type);
    }

    /**
     * RF-13. Correos de quita de rol. El cuerpo ya viene armado desde Service;
     * Notification sólo lo envía por SMTP.
     */
    default void notifyRoleRemovalEmail(String type, UUID receiverId, String receiverEmail,
                                        UUID requestId, String subject, String body) {
        // Las implementaciones que no envían correo no hacen nada.
    }

    /**
     * Dos trabajadores aceptaron la misma publicación y el contratante debe elegir.
     * Las implementaciones que no envían correo no hacen nada.
     */
    default void notifyConflict(UUID contractorId, UUID requestId, UUID rootId, String receiverEmail) {
        // Sin correo: solo la implementación HTTP publica el evento en Notification.
    }

    default void notifyConflict(UUID contractorId, UUID requestId, UUID rootId, String receiverEmail,
                                RequestNotificationDetails details) {
        notifyConflict(contractorId, requestId, rootId, receiverEmail);
    }
}
