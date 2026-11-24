package edu.eci.dosw.oficioya.oficioya_service.service;

import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import edu.eci.dosw.oficioya.oficioya_service.client.ImageClient;
import edu.eci.dosw.oficioya.oficioya_service.dto.ImageResponse;

@Service
public class ImageService {
    private final RequestService requestService;
    private final ImageClient imageClient;

    public ImageService(RequestService requestService, ImageClient imageClient) {
        this.requestService = requestService;
        this.imageClient = imageClient;
    }

    public void addImages(List<MultipartFile> files, UUID rootId, String referenciaExterna, String authHeader) {
        if (files == null || files.isEmpty()) {
            return;
        }
        imageClient.uploadImages(files, rootId, referenciaExterna, authHeader);
    }

    public List<ImageResponse> findRequestImages(UUID requestId, UUID userId) {
        return requestService.getRequest(requestId)
                .filter(r -> userId.equals(r.getContractorId()) || userId.equals(r.getWorkerId()))
                .map(r -> imageClient.getImages(r.getRootId()))
                .orElseGet(List::of);
    }
}
