package edu.eci.dosw.oficioya.oficioya_service.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import edu.eci.dosw.oficioya.oficioya_service.dto.ImageRequest;
import edu.eci.dosw.oficioya.oficioya_service.dto.ImageResponse;
import edu.eci.dosw.oficioya.oficioya_service.entity.Request;

@ExtendWith(MockitoExtension.class)
class ImageServiceTest {

    @Mock
    private RequestService requestService;

    @Mock
    private ImageClient imageClient;

    @InjectMocks
    private ImageService imageService;

    private UUID userId;
    private UUID requestId;
    private UUID rootId;
    private Request request;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        requestId = UUID.randomUUID();
        rootId = UUID.randomUUID();
        request = new Request();
        request.setRootId(rootId);
    }

    @Test
    @DisplayName("delega la subida al microservicio con el rootId y el token")
    void delegaSubida() {
        List<ImageRequest> images = List.of(new ImageRequest("a.png", "image/png", new byte[] { 1 }, null));
        List<ImageResponse> expected = List.of();
        when(imageClient.uploadImages(images, rootId, "jwt-token")).thenReturn(expected);

        assertEquals(expected, imageService.addImages(images, rootId, "jwt-token"));
        verify(imageClient).uploadImages(images, rootId, "jwt-token");
    }

    @Test
    @DisplayName("no llama al microservicio si no hay imágenes")
    void sinImagenes() {
        assertTrue(imageService.addImages(List.of(), rootId, "jwt-token").isEmpty());
        verifyNoInteractions(imageClient);
    }

    @Test
    @DisplayName("el contratante puede consultar las imágenes de su solicitud")
    void contratantePuedeConsultar() {
        request.setContractorId(userId);
        when(requestService.getRequest(requestId)).thenReturn(Optional.of(request));

        imageService.findRequestImages(requestId, userId, "jwt-token");

        verify(imageClient).getRequestImages(rootId, "jwt-token");
    }

    @Test
    @DisplayName("el trabajador asignado puede consultar las imágenes de su solicitud")
    void trabajadorPuedeConsultar() {
        request.setWorkerId(userId);
        when(requestService.getRequest(requestId)).thenReturn(Optional.of(request));

        imageService.findRequestImages(requestId, userId, "jwt-token");

        verify(imageClient).getRequestImages(rootId, "jwt-token");
    }

    @Test
    @DisplayName("un usuario ajeno no puede consultar las imágenes")
    void usuarioAjenoNoPuedeConsultar() {
        request.setContractorId(UUID.randomUUID());
        when(requestService.getRequest(requestId)).thenReturn(Optional.of(request));

        assertTrue(imageService.findRequestImages(requestId, userId, "jwt-token").isEmpty());
        verifyNoInteractions(imageClient);
    }

    @Test
    @DisplayName("una solicitud inexistente devuelve lista vacía")
    void solicitudInexistente() {
        when(requestService.getRequest(requestId)).thenReturn(Optional.empty());

        assertTrue(imageService.findRequestImages(requestId, userId, "jwt-token").isEmpty());
        verifyNoInteractions(imageClient);
    }
}
