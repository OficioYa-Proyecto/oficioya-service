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

    void notifyAccepted(UUID contractorId, UUID requestId);
    void notifyCancelled(UUID receiverId, UUID requestId);
    void notifyCompleted(UUID contractorId, UUID requestId);
    void notifyRejected(UUID contractorId, UUID requestId);

    /*
     void notifyConflict(UUID contractorId, UUID requestId);???
     */
}
