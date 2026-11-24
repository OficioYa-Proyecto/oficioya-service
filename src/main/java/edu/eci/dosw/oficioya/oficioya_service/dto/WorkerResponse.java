package edu.eci.dosw.oficioya.oficioya_service.dto;

import java.util.UUID;

public record WorkerResponse(UUID id, UUID userId, WorkerStatus status) {
}
