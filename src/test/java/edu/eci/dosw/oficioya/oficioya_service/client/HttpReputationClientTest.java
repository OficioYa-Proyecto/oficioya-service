package edu.eci.dosw.oficioya.oficioya_service.client;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.jsonPath;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;
import org.mockito.quality.Strictness;
import java.time.LocalDateTime;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestTemplate;

import edu.eci.dosw.oficioya.oficioya_service.security.JwtUtil;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class HttpReputationClientTest {

    private HttpReputationClient client;
    private MockRestServiceServer server;
    
    @Mock
    private JwtUtil jwtUtil;

    private final UUID solicitudId = UUID.randomUUID();
    private final UUID trabajadorId = UUID.randomUUID();
    private final UUID contratanteId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        jwtUtil = mock(JwtUtil.class);
        client = new HttpReputationClient("https://reputation.test/", jwtUtil);
        RestTemplate restTemplate = (RestTemplate) ReflectionTestUtils.getField(client, "restTemplate");
        server = MockRestServiceServer.bindTo(restTemplate).build();
        when(jwtUtil.generateServiceToken()).thenReturn("fake-system-token");
    }

    @Test
    @DisplayName("Notifica la cancelación tardía con el payload esperado y sin barra final duplicada")
    void notificaCancelacionTardia() {
        LocalDateTime fechaServicio = LocalDateTime.of(2026, 11, 24, 10, 0);
        LocalDateTime fechaCancelacion = LocalDateTime.of(2026, 11, 24, 9, 30);

        server.expect(requestTo("https://reputation.test/api/v1/reputation/eventos/cancelaciones-tardias"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE))
                .andExpect(jsonPath("$.solicitudId").value(solicitudId.toString()))
                .andExpect(jsonPath("$.trabajadorId").value(trabajadorId.toString()))
                .andExpect(jsonPath("$.fechaHoraServicio").value(fechaServicio.toString()))
                .andExpect(jsonPath("$.fechaCancelacion").value(fechaCancelacion.toString()))
                .andExpect(header(HttpHeaders.AUTHORIZATION, "Bearer fake-system-token"))
                .andRespond(withSuccess("{}", MediaType.APPLICATION_JSON));

        client.notifyLateCancellation(solicitudId, trabajadorId, fechaServicio, fechaCancelacion);
        server.verify();
    }

    @Test
    @DisplayName("Notifica el servicio cumplido con el contratante y la fecha de cumplimiento")
    void notificaServicioCumplido() {
        LocalDateTime fechaCumplida = LocalDateTime.of(2026, 11, 25, 12, 15);

        server.expect(requestTo("https://reputation.test/api/v1/reputation/eventos/servicios-cumplidos"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(jsonPath("$.solicitudId").value(solicitudId.toString()))
                .andExpect(jsonPath("$.trabajadorId").value(trabajadorId.toString()))
                .andExpect(jsonPath("$.contratanteId").value(contratanteId.toString()))
                .andExpect(jsonPath("$.fechaCumplida").value(fechaCumplida.toString()))
                .andExpect(header(HttpHeaders.AUTHORIZATION, "Bearer fake-system-token"))
                .andRespond(withSuccess("{}", MediaType.APPLICATION_JSON));

        client.notifyServiceCompleted(solicitudId, trabajadorId, contratanteId, fechaCumplida);
        server.verify();
    }

    @Test
    @DisplayName("Un error de Reputation no se propaga al flujo de la solicitud")
    void errorDeReputationEsTolerado() {
        server.expect(requestTo("https://reputation.test/api/v1/reputation/eventos/cancelaciones-tardias"))
                .andRespond(withServerError());
        server.expect(requestTo("https://reputation.test/api/v1/reputation/eventos/servicios-cumplidos"))
                .andRespond(withServerError());

        assertDoesNotThrow(() -> client.notifyLateCancellation(
                solicitudId, trabajadorId, LocalDateTime.now(), LocalDateTime.now()));
        assertDoesNotThrow(() -> client.notifyServiceCompleted(
                solicitudId, trabajadorId, contratanteId, LocalDateTime.now()));
        server.verify();
    }

    @Test
    @DisplayName("La penalización directa está delegada al evento de cancelación tardía")
    void penalizacionDelegada() {
        assertDoesNotThrow(() -> client.penalize(trabajadorId, 2.5));
        server.verify();
    }
}
