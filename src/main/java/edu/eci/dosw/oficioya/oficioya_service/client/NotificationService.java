package edu.eci.dosw.oficioya.oficioya_service.client;

import java.util.UUID;

public interface NotificationService {
    void notifyNewRequest(UUID workerId, UUID requestId);
    void notifyRequestCreated(UUID contractorId, UUID requestId, String receiverEmail);
    void notifyAccepted(UUID contractorId, UUID requestId);
    void notifyCancelled(UUID receiverId, UUID requestId);
    void notifyCompleted(UUID contractorId, UUID requestId);
    
    void notifyRejected(UUID contractorId, UUID requestId);
    
   /*
    void notifyConflict(UUID contractorId, UUID requestId);???
    */
}