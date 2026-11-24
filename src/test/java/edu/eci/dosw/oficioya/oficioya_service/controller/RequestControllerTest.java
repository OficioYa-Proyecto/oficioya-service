package edu.eci.dosw.oficioya.oficioya_service.controller;

import edu.eci.dosw.oficioya.oficioya_service.client.ImageClient;
import edu.eci.dosw.oficioya.oficioya_service.dto.RequestRequest;
import edu.eci.dosw.oficioya.oficioya_service.dto.RequestResponse;
import edu.eci.dosw.oficioya.oficioya_service.model.RequestModel;
import edu.eci.dosw.oficioya.oficioya_service.security.CurrentUser;
import edu.eci.dosw.oficioya.oficioya_service.security.JwtUtil;
import edu.eci.dosw.oficioya.oficioya_service.security.JwtUtil.EmailAction;
import edu.eci.dosw.oficioya.oficioya_service.service.ImageService;
import edu.eci.dosw.oficioya.oficioya_service.service.RequestCancellationService;
import edu.eci.dosw.oficioya.oficioya_service.service.RequestCompletionService;
import edu.eci.dosw.oficioya.oficioya_service.service.RequestService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.core.Authentication;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class RequestControllerTest {

    @Mock
    private RequestService requestService;

    @Mock
    private ImageService imageService;

    @Mock
    private RequestCancellationService requestCancellationService;

    @Mock
    private RequestCompletionService requestCompletionService;

    @Mock
    private Authentication authentication;

    @Mock
    private ImageClient imageClient;

    @Mock
    private JwtUtil jwtUtil;

    @InjectMocks
    private RequestController requestController;

    private MockedStatic<CurrentUser> currentUserMock;
    private UUID userId;
    private UUID requestId;

    @BeforeEach
    void setup() {
        userId = UUID.randomUUID();
        requestId = UUID.randomUUID();
        currentUserMock = mockStatic(CurrentUser.class);
        lenient().when(CurrentUser.id(authentication)).thenReturn(userId);
    }

    @AfterEach
    void tearDown() {
        currentUserMock.close();
    }
    @Test
    @DisplayName("Debe crear la solicitud y subir las imágenes con el mismo rootId")
    void shouldCreateRequestAndSaveImages() {
        String authHeader = "Bearer token-de-prueba";
        List<MultipartFile> images = List.of(
                new MockMultipartFile("images", "a.png", "image/png", new byte[]{1}),
                new MockMultipartFile("images", "b.png", "image/png", new byte[]{2}));
        RequestRequest req = mock(RequestRequest.class);
        when(requestService.create(eq(req), eq(userId), any(UUID.class),eq(authHeader))).thenAnswer(invocation ->
                new RequestResponse(invocation.getArgument(2), List.of(), "Fuga", null, 0));

        RequestResponse result = requestController.create(req, images, authHeader, authentication);

        ArgumentCaptor<UUID> rootIdCaptor = ArgumentCaptor.forClass(UUID.class);
        verify(imageService).addImages(eq(images), rootIdCaptor.capture(), isNull(), eq(authHeader));
        verify(requestService).create(eq(req), eq(userId), eq(rootIdCaptor.getValue()),eq(authHeader));
        assertEquals(rootIdCaptor.getValue(), result.rootId());
    }

    @Test
    @DisplayName("Debe listar las solicitudes de un trabajador")
    void shouldListRequestsForWorker() {
        List<RequestModel> expected = List.of(new RequestModel(), new RequestModel());
        when(requestService.listRequestsForWorker(userId)).thenReturn(expected);

        List<RequestModel> result = requestController.listForWorker(authentication);

        assertEquals(2, result.size());
        assertEquals(expected, result);
        verify(requestService).listRequestsForWorker(userId);
    }

    @Test
    @DisplayName("Debe listar las solicitudes de un contratante")
    void shouldListRequestsForContractor() {
        List<RequestModel> expected = List.of(new RequestModel());
        when(requestService.listRequestsForContractor(userId)).thenReturn(expected);

        List<RequestModel> result = requestController.listForContractor(authentication);

        assertEquals(1, result.size());
        assertEquals(expected, result);
        verify(requestService).listRequestsForContractor(userId);
    }

    @Test
    @DisplayName("Debe permitir al trabajador aceptar una solicitud")
    void shouldAcceptRequest() {
        RequestModel expected = new RequestModel();
        when(requestService.acceptRequest(requestId, userId)).thenReturn(expected);

        RequestModel result = requestController.accept(requestId, authentication);

        assertEquals(expected, result);
        verify(requestService).acceptRequest(requestId, userId);
    }

    @Test
    @DisplayName("Debe permitir al trabajador rechazar una solicitud")
    void shouldRejectRequest() {
        RequestModel expected = new RequestModel();
        when(requestService.rejectRequest(requestId, userId)).thenReturn(expected);

        RequestModel result = requestController.reject(requestId, authentication);

        assertEquals(expected, result);
        verify(requestService).rejectRequest(requestId, userId);
    }

    @Test
    @DisplayName("Debe permitir al contratante resolver un conflicto")
    void shouldAcceptConflict() {
        RequestModel expected = new RequestModel();
        when(requestService.acceptConflict(requestId, userId)).thenReturn(expected);

        RequestModel result = requestController.acceptConflict(requestId, authentication);

        assertEquals(expected, result);
        verify(requestService).acceptConflict(requestId, userId);
    }

    @Test
    @DisplayName("Debe permitir cancelar una solicitud")
    void shouldCancelRequest() {
        RequestModel expected = new RequestModel();
        when(requestCancellationService.cancelRequest(requestId, userId)).thenReturn(expected);

        RequestModel result = requestController.cancelRequest(requestId, authentication);

        assertEquals(expected, result);
        verify(requestCancellationService).cancelRequest(requestId, userId);
    }

    @Test
    @DisplayName("Debe permitir completar una solicitud")
    void shouldCompleteRequest() {
        RequestModel expected = new RequestModel();
        when(requestCompletionService.completeRequest(requestId, userId)).thenReturn(expected);

        RequestModel result = requestController.completeRequest(requestId, authentication);

        assertEquals(expected, result);
        verify(requestCompletionService).completeRequest(requestId, userId);
    }

    @Test
    @DisplayName("Debe retornar OK cuando la solicitud se elimina correctamente")
    void shouldDeleteRequestSuccessfully() {
        when(requestService.deleteRequest(requestId)).thenReturn(true);

        ResponseEntity<String> response = requestController.deleteRequest(requestId);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals("Request deleted", response.getBody());
        verify(requestService).deleteRequest(requestId);
    }

    @Test
    @DisplayName("Debe retornar BAD_REQUEST cuando la solicitud no se puede eliminar")
    void shouldReturnBadRequestWhenDeleteFails() {
        when(requestService.deleteRequest(requestId)).thenReturn(false);

        ResponseEntity<String> response = requestController.deleteRequest(requestId);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertEquals("Request not deleted", response.getBody());
        verify(requestService).deleteRequest(requestId);
    }

    @Test
    @DisplayName("El enlace del correo acepta la solicitud y redirige al front")
    void shouldAcceptRequestFromEmailLink() {
        EmailAction action = new EmailAction(userId, requestId, "ACCEPT");
        when(jwtUtil.validateEmailActionToken("token")).thenReturn(action);

        ResponseEntity<Void> response = requestController.acceptFromEmail(requestId, "token");

        assertEquals(HttpStatus.FOUND, response.getStatusCode());
        assertTrue(response.getHeaders().getLocation().toString().contains("result=accepted"));
        verify(requestService).acceptRequest(requestId, userId);
    }

    @Test
    @DisplayName("El enlace del correo rechaza la solicitud y redirige al front")
    void shouldRejectRequestFromEmailLink() {
        EmailAction action = new EmailAction(userId, requestId, "REJECT");
        when(jwtUtil.validateEmailActionToken("token")).thenReturn(action);

        ResponseEntity<Void> response = requestController.rejectFromEmail(requestId, "token");

        assertEquals(HttpStatus.FOUND, response.getStatusCode());
        assertTrue(response.getHeaders().getLocation().toString().contains("result=rejected"));
        verify(requestService).rejectRequest(requestId, userId);
    }

    @Test
    @DisplayName("Un enlace manipulado no ejecuta la acción")
    void shouldRejectTamperedEmailLink() {
        EmailAction action = new EmailAction(userId, requestId, "ACCEPT");
        when(jwtUtil.validateEmailActionToken("token")).thenReturn(action);
        UUID otroRequestId = UUID.randomUUID();

        ResponseEntity<Void> response = requestController.acceptFromEmail(otroRequestId, "token");

        assertEquals(HttpStatus.FOUND, response.getStatusCode());
        assertTrue(response.getHeaders().getLocation().toString().contains("result=invalid"));
        verify(requestService, never()).acceptRequest(any(), any());
    }
}
