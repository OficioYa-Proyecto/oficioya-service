package edu.eci.dosw.oficioya.oficioya_service.service;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import edu.eci.dosw.oficioya.oficioya_service.client.HttpImageClient;
import edu.eci.dosw.oficioya.oficioya_service.client.ImageClient;
import edu.eci.dosw.oficioya.oficioya_service.dto.ImageResponse;
import edu.eci.dosw.oficioya.oficioya_service.entity.Request;

@Service 
public class ImageService {
    private RequestService requestService;

    private ImageClient imageClient;

    public ImageService(RequestService requestService, HttpImageClient imageClient){
        this.requestService = requestService;
        this.imageClient = imageClient;
    }

    public void addImages(List<MultipartFile> files, UUID rootId, String referenciaExterna, Authentication authentication){
        if(files != null && !files.isEmpty()){
            imageClient.uploadImages(files, rootId, referenciaExterna, authentication);
        }
    }

    public List<ImageResponse> findRequestImages(UUID requestId,UUID userId){
        Optional<Request> found = requestService.getRequest(requestId);

        if(found.isEmpty() || (found.orElse(null).getContractorId() != userId && found.orElse(null).getWorkerId() != userId)){
            return new ArrayList<>();
        }

        return imageClient.getImages(found.orElse(null).getRootId());
  
    }
}
