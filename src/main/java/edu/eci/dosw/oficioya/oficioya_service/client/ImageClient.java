package edu.eci.dosw.oficioya.oficioya_service.client;

import java.util.List;
import java.util.UUID;

import org.springframework.security.core.Authentication;
import org.springframework.web.multipart.MultipartFile;

import edu.eci.dosw.oficioya.oficioya_service.dto.ImageResponse;

public interface ImageClient {
    List<ImageResponse> getImages(UUID rootId);

    void uploadImages(List<MultipartFile> files, UUID rootId, String referenciaExterna, String authHeader);
}
