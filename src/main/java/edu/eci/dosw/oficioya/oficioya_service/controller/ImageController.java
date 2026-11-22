package edu.eci.dosw.oficioya.oficioya_service.controller;

import java.util.List;
import java.util.UUID;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import edu.eci.dosw.oficioya.oficioya_service.dto.ImageResponse;
import edu.eci.dosw.oficioya.oficioya_service.security.CurrentUser;
import edu.eci.dosw.oficioya.oficioya_service.service.ImageService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@RequestMapping("/api/v1/requests/images")
@Tag(name="Images", description = "API to control the images of the requests")
public class ImageController {

    private final ImageService imageService;

    public ImageController(ImageService imageService) {
        this.imageService = imageService;
    }

    @GetMapping("/{requestId}")
    @PreAuthorize("hasAnyRole('CONTRATANTE', 'TRABAJADOR')")
    @Operation(summary="Get images of a request", description = "Get all the images of a request from the image microservice")
    public List<ImageResponse> getRequestImages(@PathVariable UUID requestId, Authentication authentication) {
        return imageService.findRequestImages(
                requestId,
                CurrentUser.id(authentication),
                CurrentUser.token(authentication));
    }
}
