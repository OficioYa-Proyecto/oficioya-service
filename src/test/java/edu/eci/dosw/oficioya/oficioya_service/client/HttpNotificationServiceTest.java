package edu.eci.dosw.oficioya.oficioya_service.client;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.jsonPath;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import java.time.LocalDateTime;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestTemplate;

import edu.eci.dosw.oficioya.oficioya_service.security.JwtUtil;

@ExtendWith(MockitoExtension.class)
class HttpNotificationServiceTest {

    @Mock
    private EmailActionLinkService emailActionLinkService;

    @Mock
    private JwtUtil jwtUtil;

    private HttpNotificationService service;
    private MockRestServiceServer server;
    private final UUID workerId = UUID.randomUUID();
    private final UUID requestId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        service = new HttpNotificationService("https://notification.test/", emailActionLinkService);
        server = MockRestServiceServer.bindTo((RestTemplate) ReflectionTestUtils.getField(service, "restTemplate"))
                .build();
    }

    @Test
    @DisplayName("El correo del trabajador incluye contexto, imagen y enlaces firmados")
    void notifyNewRequestEnvíaAcciones() {
        String accept = "https://service.test/email/accept?token=a";
        String reject = "https://service.test/email/reject?token=r";
        when(emailActionLinkService.acceptUrl(workerId, requestId)).thenReturn(accept);
        when(emailActionLinkService.rejectUrl(workerId, requestId)).thenReturn(reject);
        RequestNotificationDetails details = new RequestNotificationDetails(
                LocalDateTime.of(2026, 11, 25, 10, 30), "Chapinero", "Calle 1", "Reparar fuga",
                "https://images.test/fuga.jpg");

        server.expect(requestTo("https://notification.test/api/v1/notification"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(jsonPath("$.receiverId").value(workerId.toString()))
                .andExpect(jsonPath("$.type").value("INCOMING_REQUEST"))
                .andExpect(jsonPath("$.requestId").value(requestId.toString()))
                .andExpect(jsonPath("$.receiverEmail").value("worker@test.com"))
                .andExpect(jsonPath("$.neighborhood").value("Chapinero"))
                .andExpect(jsonPath("$.imageUrl").value("https://images.test/fuga.jpg"))
                .andExpect(jsonPath("$.acceptUrl").value(accept))
                .andExpect(jsonPath("$.rejectUrl").value(reject))
                .andRespond(withSuccess("{}", MediaType.APPLICATION_JSON));

        service.notifyNewRequest(workerId, requestId, " worker@test.com ", details);
        server.verify();
    }

    @Test
    @DisplayName("Las demás notificaciones publican el tipo correcto sin enlaces de acción")
    void notificacionesSinAcciones() {
        server.expect(requestTo("https://notification.test/api/v1/notification"))
                .andExpect(jsonPath("$.type").value("REQUEST_CREATED"))
                .andRespond(withSuccess("{}", MediaType.APPLICATION_JSON));
        server.expect(requestTo("https://notification.test/api/v1/notification"))
                .andExpect(jsonPath("$.type").value("ACCEPTED_REQUEST"))
                .andRespond(withSuccess("{}", MediaType.APPLICATION_JSON));
        server.expect(requestTo("https://notification.test/api/v1/notification"))
                .andExpect(jsonPath("$.type").value("CANCELLED_BY_CONTRACTOR"))
                .andRespond(withSuccess("{}", MediaType.APPLICATION_JSON));
        server.expect(requestTo("https://notification.test/api/v1/notification"))
                .andExpect(jsonPath("$.type").value("COMPLETED_WORK"))
                .andRespond(withSuccess("{}", MediaType.APPLICATION_JSON));
        server.expect(requestTo("https://notification.test/api/v1/notification"))
                .andExpect(jsonPath("$.type").value("REJECTED_REQUEST"))
                .andRespond(withSuccess("{}", MediaType.APPLICATION_JSON));

        service.notifyRequestCreated(workerId, requestId, null);
        service.notifyAccepted(workerId, requestId);
        service.notifyCancelled(workerId, requestId);
        service.notifyCompleted(workerId, requestId);
        service.notifyRejected(workerId, requestId);
        server.verify();
    }

    @Test
    @DisplayName("Cada llamada a Notification lleva el token de servicio")
    void notificacionesConTokenDeServicio() {
        when(jwtUtil.generateServiceToken()).thenReturn("service-token");
        RestTemplate rest = new RestTemplate();
        HttpNotificationService autenticada = new HttpNotificationService(
                "https://notification.test/", emailActionLinkService, "", jwtUtil, rest);
        MockRestServiceServer servidorAutenticado = MockRestServiceServer.bindTo(rest).build();

        servidorAutenticado.expect(requestTo("https://notification.test/api/v1/notification"))
                .andExpect(header(HttpHeaders.AUTHORIZATION, "Bearer service-token"))
                .andExpect(jsonPath("$.type").value("REQUEST_CREATED"))
                .andRespond(withSuccess("{}", MediaType.APPLICATION_JSON));
        servidorAutenticado.expect(requestTo("https://notification.test/api/v1/notification"))
                .andExpect(header(HttpHeaders.AUTHORIZATION, "Bearer service-token"))
                .andExpect(jsonPath("$.type").value("INCOMING_REQUEST"))
                .andRespond(withSuccess("{}", MediaType.APPLICATION_JSON));

        when(emailActionLinkService.acceptUrl(workerId, requestId)).thenReturn("https://service.test/accept");
        when(emailActionLinkService.rejectUrl(workerId, requestId)).thenReturn("https://service.test/reject");
        autenticada.notifyRequestCreated(contractorId(), requestId, "contractor@test.com");
        autenticada.notifyNewRequest(workerId, requestId, "worker@test.com", null);
        servidorAutenticado.verify();
    }

    private UUID contractorId() {
        return UUID.fromString("00000000-0000-0000-0000-000000000099");
    }
}
