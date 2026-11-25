package edu.eci.dosw.oficioya.oficioya_service.client;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.jsonPath;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import java.time.LocalDateTime;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestTemplate;

class HttpReputationClientTest {

    private HttpReputationClient client;
    private MockRestServiceServer server;

    private final UUID solicitudId = UUID.randomUUID();
    private final UUID trabajadorId = UUID.randomUUID();
    private final UUID contratanteId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        client = new HttpReputationClient("https://reputation.test/");
        RestTemplate restTemplate = (RestTemplate) ReflectionTestUtils.getField(client, "restTemplate");
        server = MockRestServiceServer.bindTo(restTemplate).build();
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
