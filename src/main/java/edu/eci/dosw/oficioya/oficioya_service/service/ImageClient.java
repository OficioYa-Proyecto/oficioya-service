package edu.eci.dosw.oficioya.oficioya_service.service;

import java.util.List;
import java.util.UUID;

import edu.eci.dosw.oficioya.oficioya_service.dto.ImageRequest;
import edu.eci.dosw.oficioya.oficioya_service.dto.ImageResponse;

/** Puente hacia el microservicio de imágenes a través del Orchestrator. */
public interface ImageClient {

    List<ImageResponse> uploadImages(List<ImageRequest> images, UUID rootId, String token);

    List<ImageResponse> getRequestImages(UUID rootId, String token);
}
