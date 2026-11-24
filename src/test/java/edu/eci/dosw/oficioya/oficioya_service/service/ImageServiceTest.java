package edu.eci.dosw.oficioya.oficioya_service.service;

import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;

import edu.eci.dosw.oficioya.oficioya_service.client.ImageClient;

@ExtendWith(MockitoExtension.class)
class ImageServiceTest {

    @Mock
    private RequestService requestService;

    @Mock
    private ImageClient imageClient;

    @InjectMocks
    private ImageService imageService;

    @Test
    @DisplayName("Sube las imágenes cuando la lista no está vacía")
    void subeImágenes() {
        UUID rootId = UUID.randomUUID();
        List<MultipartFile> files = List.of(new MockMultipartFile("images", "fuga.jpg", "image/jpeg", new byte[]{1}));

        imageService.addImages(files, rootId, null, "Bearer token");

        verify(imageClient).uploadImages(files, rootId, null, "Bearer token");
    }

    @Test
    @DisplayName("No intenta subir imágenes cuando no hay archivos")
    void sinImágenes() {
        UUID rootId = UUID.randomUUID();

        imageService.addImages(null, rootId, null, "Bearer token");
        imageService.addImages(List.of(), rootId, null, "Bearer token");

        verify(imageClient, never()).uploadImages(org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.any());
    }
}
