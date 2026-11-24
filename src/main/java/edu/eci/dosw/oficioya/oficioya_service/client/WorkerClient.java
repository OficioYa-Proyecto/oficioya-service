package edu.eci.dosw.oficioya.oficioya_service.client;

import java.util.UUID;

public interface WorkerClient {
    boolean exists(UUID workerId, String authHeader);
    void reserveSlot(UUID workerId, java.time.LocalDateTime slot);
    void releaseSlot(UUID workerId, java.time.LocalDateTime slot);
}