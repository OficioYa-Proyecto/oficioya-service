package edu.eci.dosw.oficioya.oficioya_service.client;

import java.util.List;
import java.util.UUID;

import org.springframework.security.core.Authentication;
import org.springframework.web.multipart.MultipartFile;

import edu.eci.dosw.oficioya.oficioya_service.dto.ImageResponse;

public interface ImageClient {
    List<ImageResponse> getImages(UUID rootId);

    /**
     * Consulta las imágenes con el token del usuario que las subió. Images exige
     * autenticación para listar por rootId; reutilizar ese token evita depender
     * de un token de servicio con un secreto distinto.
     */
    default List<ImageResponse> getImages(UUID rootId, String authHeader) {
        return getImages(rootId);
    }

    void uploadImages(List<MultipartFile> files, UUID rootId, String referenciaExterna, String authHeader);
}
