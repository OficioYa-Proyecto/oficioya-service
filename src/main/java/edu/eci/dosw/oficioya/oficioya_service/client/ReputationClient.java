package edu.eci.dosw.oficioya.oficioya_service.client;

import java.time.LocalDateTime;
import java.util.UUID;

public interface ReputationClient {
    void penalize(UUID workerId, double points);

    void notifyLateCancellation(UUID solicitudId, UUID trabajadorId, LocalDateTime fechaHoraServicio, LocalDateTime fechaCancelacion);

    void notifyServiceCompleted(UUID solicitudId, UUID trabajadorId, UUID contratanteId, LocalDateTime fechaCumplida);
}