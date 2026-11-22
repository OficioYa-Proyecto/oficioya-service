package edu.eci.dosw.oficioya.oficioya_service.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import java.lang.reflect.Field;
import java.time.LocalDateTime;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestTemplate;

class HttpWorkerClientTest {

    private static final String BASE = "http://worker.test";

    private HttpWorkerClient client;
    private MockRestServiceServer server;

    @BeforeEach
    void setUp() throws Exception {
        client = new HttpWorkerClient(BASE, "service-token");
        RestTemplate restTemplate = new RestTemplate();
        server = MockRestServiceServer.bindTo(restTemplate).build();
        Field field = HttpWorkerClient.class.getDeclaredField("restTemplate");
        field.setAccessible(true);
        field.set(client, restTemplate);
    }

    @Test
    @DisplayName("obtiene el oficio del trabajador desde Worker")
    void getTrade() {
        UUID workerId = UUID.randomUUID();
        server.expect(requestTo(BASE + "/api/v1/workers/" + workerId))
                .andExpect(header(HttpHeaders.AUTHORIZATION, "Bearer service-token"))
                .andRespond(withSuccess("{\"trade\":\"Fontanería\"}", MediaType.APPLICATION_JSON));

        assertEquals("Fontanería", client.getTrade(workerId));
        server.verify();
    }

    @Test
    @DisplayName("acepta oficio en oficio/tradeName y devuelve null si falla Worker")
    void getTradeAlternativas() {
        UUID workerId = UUID.randomUUID();
        server.expect(requestTo(BASE + "/api/v1/workers/" + workerId))
                .andRespond(withSuccess("{\"oficio\":\"Electricidad\"}", MediaType.APPLICATION_JSON));
        server.expect(requestTo(BASE + "/api/v1/workers/" + workerId))
                .andRespond(withSuccess("{\"tradeName\":\"Pintura\"}", MediaType.APPLICATION_JSON));
        server.expect(requestTo(BASE + "/api/v1/workers/" + workerId))
                .andRespond(withServerError());

        assertEquals("Electricidad", client.getTrade(workerId));
        assertEquals("Pintura", client.getTrade(workerId));
        assertNull(client.getTrade(workerId));
        server.verify();
    }

    @Test
    @DisplayName("sin token de servicio no consulta Worker")
    void getTradeSinToken() {
        assertNull(new HttpWorkerClient(BASE, "").getTrade(UUID.randomUUID()));
    }

    @Test
    @DisplayName("los métodos de disponibilidad son delegados sin fallar")
    void operacionesDisponibilidad() {
        UUID workerId = UUID.randomUUID();
        LocalDateTime slot = LocalDateTime.of(2026, 11, 22, 10, 0);

        assertTrue(client.exists(workerId));
        client.reserveSlot(workerId, slot);
        client.releaseSlot(workerId, slot);
    }
}
