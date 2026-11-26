package edu.eci.dosw.oficioya.oficioya_service.client;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.jsonPath;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import java.time.LocalDateTime;
import java.util.List;
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
    @DisplayName("El carrusel viaja como listas paralelas de fotos y tipos MIME")
    void notifyNewRequestIncluyeCarrusel() {
        when(emailActionLinkService.acceptUrl(workerId, requestId)).thenReturn("https://service.test/accept");
        when(emailActionLinkService.rejectUrl(workerId, requestId)).thenReturn("https://service.test/reject");
        RequestNotificationDetails details = new RequestNotificationDetails(
                LocalDateTime.of(2026, 11, 25, 10, 30), "Chapinero", "Calle 1", "Reparar fuga", null,
                "YQ==", "image/jpeg",
                List.of(new RequestNotificationPhoto("YQ==", "image/jpeg"),
                        new RequestNotificationPhoto("Yg==", "image/png")));

        server.expect(requestTo("https://notification.test/api/v1/notification"))
                .andExpect(jsonPath("$.photoBase64s[0]").value("YQ=="))
                .andExpect(jsonPath("$.photoBase64s[1]").value("Yg=="))
                .andExpect(jsonPath("$.photoContentTypes[1]").value("image/png"))
                .andRespond(withSuccess("{}", MediaType.APPLICATION_JSON));

        service.notifyNewRequest(workerId, requestId, "worker@test.com", details);
        server.verify();
    }

    @Test
    @DisplayName("Las demás notificaciones publican el tipo y el correo del destinatario")
    void notificacionesSinAcciones() {
        server.expect(requestTo("https://notification.test/api/v1/notification"))
                .andExpect(jsonPath("$.type").value("REQUEST_CREATED"))
                .andRespond(withSuccess("{}", MediaType.APPLICATION_JSON));
        server.expect(requestTo("https://notification.test/api/v1/notification"))
                .andExpect(jsonPath("$.type").value("ACCEPTED_REQUEST"))
                .andExpect(jsonPath("$.receiverEmail").value("destino@test.com"))
                .andRespond(withSuccess("{}", MediaType.APPLICATION_JSON));
        server.expect(requestTo("https://notification.test/api/v1/notification"))
                .andExpect(jsonPath("$.type").value("CANCELLED_BY_CONTRACTOR"))
                .andExpect(jsonPath("$.receiverEmail").value("destino@test.com"))
                .andRespond(withSuccess("{}", MediaType.APPLICATION_JSON));
        server.expect(requestTo("https://notification.test/api/v1/notification"))
                .andExpect(jsonPath("$.type").value("COMPLETED_WORK"))
                .andExpect(jsonPath("$.receiverEmail").value("destino@test.com"))
                .andRespond(withSuccess("{}", MediaType.APPLICATION_JSON));
        server.expect(requestTo("https://notification.test/api/v1/notification"))
                .andExpect(jsonPath("$.type").value("REJECTED_REQUEST"))
                .andExpect(jsonPath("$.receiverEmail").value("destino@test.com"))
                .andRespond(withSuccess("{}", MediaType.APPLICATION_JSON));

        service.notifyRequestCreated(workerId, requestId, null);
        service.notifyAccepted(workerId, requestId, "destino@test.com");
        service.notifyCancelled(workerId, requestId, "destino@test.com");
        service.notifyCompleted(workerId, requestId, "destino@test.com");
        service.notifyRejected(workerId, requestId, "destino@test.com");
        server.verify();
    }

    @Test
    @DisplayName("El conflicto publica el tipo CONFLICT_REQUEST y el rootId para el enlace de elección")
    void notifyConflictPublicaRootId() {
        UUID contractorId = UUID.randomUUID();
        UUID root = UUID.randomUUID();

        server.expect(requestTo("https://notification.test/api/v1/notification"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(jsonPath("$.receiverId").value(contractorId.toString()))
                .andExpect(jsonPath("$.type").value("CONFLICT_REQUEST"))
                .andExpect(jsonPath("$.requestId").value(requestId.toString()))
                .andExpect(jsonPath("$.rootId").value(root.toString()))
                .andExpect(jsonPath("$.receiverEmail").value("contratante@test.com"))
                .andRespond(withSuccess("{}", MediaType.APPLICATION_JSON));

        service.notifyConflict(contractorId, requestId, root, " contratante@test.com ");
        server.verify();
    }

    @Test
    @DisplayName("La cancelación del trabajador usa su propio tipo de notificación")
    void cancelacionPorTrabajador() {
        server.expect(requestTo("https://notification.test/api/v1/notification"))
                .andExpect(jsonPath("$.type").value("CANCELLED_BY_WORKER"))
                .andExpect(jsonPath("$.receiverEmail").value("contratante@test.com"))
                .andRespond(withSuccess("{}", MediaType.APPLICATION_JSON));

        service.notifyCancelled(workerId, requestId, "contratante@test.com", "CANCELLED_BY_WORKER");
        server.verify();
    }

    @Test
    @DisplayName("Normaliza la URL de Notification cuando llega sin esquema")
    void notificationUrlSinEsquema() {
        RestTemplate rest = new RestTemplate();
        HttpNotificationService sinEsquema = new HttpNotificationService(
                "notification.test/", emailActionLinkService, "", null, rest);
        MockRestServiceServer servidor = MockRestServiceServer.bindTo(rest).build();

        servidor.expect(requestTo("https://notification.test/api/v1/notification"))
                .andExpect(jsonPath("$.type").value("REQUEST_CREATED"))
                .andRespond(withSuccess("{}", MediaType.APPLICATION_JSON));

        sinEsquema.notifyRequestCreated(contractorId(), requestId, "contractor@test.com");
        servidor.verify();
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

    @Test
    @DisplayName("El correo de quita de rol lleva el token de servicio")
    void correoDeQuitaDeRolConTokenDeServicio() {
        when(jwtUtil.generateServiceToken()).thenReturn("service-token");
        RestTemplate rest = new RestTemplate();
        HttpNotificationService autenticada = new HttpNotificationService(
                "https://notification.test/", emailActionLinkService, "", jwtUtil, rest);
        MockRestServiceServer servidor = MockRestServiceServer.bindTo(rest).build();

        servidor.expect(requestTo("https://notification.test/api/v1/notification/email"))
                .andExpect(header(HttpHeaders.AUTHORIZATION, "Bearer service-token"))
                .andExpect(jsonPath("$.type").value("ROLE_REMOVAL_APPROVED"))
                .andRespond(withSuccess("{}", MediaType.APPLICATION_JSON));

        autenticada.notifyRoleRemovalEmail("ROLE_REMOVAL_APPROVED", contractorId(),
                "contratante@test.com", requestId, "Asunto", "Cuerpo");
        servidor.verify();
    }

    private UUID contractorId() {
        return UUID.fromString("00000000-0000-0000-0000-000000000099");
    }
}
