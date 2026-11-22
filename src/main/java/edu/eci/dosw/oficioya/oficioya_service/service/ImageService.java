package edu.eci.dosw.oficioya.oficioya_service.service;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Service;

import edu.eci.dosw.oficioya.oficioya_service.dto.ImageRequest;
import edu.eci.dosw.oficioya.oficioya_service.dto.ImageResponse;
import edu.eci.dosw.oficioya.oficioya_service.entity.Request;

@Service
public class ImageService {

    private final RequestService requestService;
    private final ImageClient imageClient;

    public ImageService(RequestService requestService, ImageClient imageClient) {
        this.requestService = requestService;
        this.imageClient = imageClient;
    }

    public List<ImageResponse> addImages(List<ImageRequest> images, UUID rootId, String token) {
        if (images == null || images.isEmpty()) {
            return List.of();
        }
        return imageClient.uploadImages(images, rootId, token);
    }

    public List<ImageResponse> findRequestImages(UUID requestId, UUID userId, String token) {
        Optional<Request> found = requestService.getRequest(requestId);
        if (found.isEmpty()) {
            return List.of();
        }

        Request request = found.get();
        boolean owner = (request.getContractorId() != null && request.getContractorId().equals(userId))
                || (request.getWorkerId() != null && request.getWorkerId().equals(userId));
        if (!owner) {
            return List.of();
        }

        return imageClient.getRequestImages(request.getRootId(), token);
    }
}
