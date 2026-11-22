package edu.eci.dosw.oficioya.oficioya_service.service;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import java.lang.reflect.Field;
import java.time.LocalDateTime;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestTemplate;

class HttpReputationClientTest {

    private static final String BASE = "https://orchestrator.test";

    private HttpReputationClient client;
    private MockRestServiceServer server;

    @BeforeEach
    void setUp() throws Exception {
        client = new HttpReputationClient(BASE);
        RestTemplate restTemplate = new RestTemplate();
        server = MockRestServiceServer.bindTo(restTemplate).build();
        Field field = HttpReputationClient.class.getDeclaredField("restTemplate");
        field.setAccessible(true);
        field.set(client, restTemplate);
    }

    @Test
    @DisplayName("envía cancelación tardía a Reputation vía Orchestrator")
    void cancelacionTardia() {
        UUID solicitud = UUID.randomUUID();
        UUID trabajador = UUID.randomUUID();
        server.expect(requestTo(BASE + "/api/v1/reputation/eventos/cancelaciones-tardias"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(content().string(org.hamcrest.Matchers.containsString(solicitud.toString())))
                .andRespond(withSuccess("{}", MediaType.APPLICATION_JSON));

        client.notifyLateCancellation(solicitud, trabajador,
                LocalDateTime.of(2026, 11, 22, 14, 30),
                LocalDateTime.of(2026, 11, 22, 14, 45));
        server.verify();
    }

    @Test
    @DisplayName("envía servicio cumplido a Reputation vía Orchestrator")
    void servicioCumplido() {
        UUID solicitud = UUID.randomUUID();
        server.expect(requestTo(BASE + "/api/v1/reputation/eventos/servicios-cumplidos"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(content().string(org.hamcrest.Matchers.containsString(solicitud.toString())))
                .andRespond(withSuccess("{}", MediaType.APPLICATION_JSON));

        client.notifyServiceCompleted(solicitud, UUID.randomUUID(), UUID.randomUUID(),
                LocalDateTime.of(2026, 11, 22, 15, 0));
        server.verify();
    }

    @Test
    @DisplayName("no propaga errores del Orchestrator")
    void falloServicio() {
        server.expect(requestTo(BASE + "/api/v1/reputation/eventos/cancelaciones-tardias"))
                .andRespond(withServerError());

        assertDoesNotThrow(() -> client.notifyLateCancellation(
                UUID.randomUUID(), UUID.randomUUID(), LocalDateTime.now(), LocalDateTime.now()));
        server.verify();
    }
}
