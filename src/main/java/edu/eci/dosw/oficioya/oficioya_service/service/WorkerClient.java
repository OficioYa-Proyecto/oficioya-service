package edu.eci.dosw.oficioya.oficioya_service.service;

import java.util.UUID;

public interface WorkerClient {
    boolean exists(UUID workerId);
    void reserveSlot(UUID workerId, java.time.LocalDateTime slot);
    void releaseSlot(UUID workerId, java.time.LocalDateTime slot);
    String getTrade(UUID workerId);
}