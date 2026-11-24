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
import org.springframework.security.core.Authentication;

import edu.eci.dosw.oficioya.oficioya_service.client.HttpImageClient;
import edu.eci.dosw.oficioya.oficioya_service.client.ImageClient;

@ExtendWith(MockitoExtension.class)
class ImageServiceTest {

    @Mock
    private HttpImageClient imageClient;

    @InjectMocks
    private ImageService imageService;

    @Test
    @DisplayName("Sube las imágenes cuando la lista no está vacía")
    void subeImágenes() {
        UUID rootId = UUID.randomUUID();
        List<org.springframework.web.multipart.MultipartFile> files =
                List.of(new MockMultipartFile("files", "fuga.jpg", "image/jpeg", new byte[]{1}));
        Authentication auth = org.mockito.Mockito.mock(Authentication.class);

        imageService.addImages(files, rootId, null, auth);

        verify(imageClient).uploadImages(files, rootId, null, auth);
    }

    @Test
    @DisplayName("No intenta subir imágenes cuando no hay archivos")
    void sinImágenes() {
        UUID rootId = UUID.randomUUID();
        Authentication auth = org.mockito.Mockito.mock(Authentication.class);

        imageService.addImages(null, rootId, null, auth);
        imageService.addImages(List.of(), rootId, null, auth);

        verify(imageClient, never()).uploadImages(org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.any());
    }
}
