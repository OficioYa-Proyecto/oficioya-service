package edu.eci.dosw.oficioya.oficioya_service.client;

import static org.hamcrest.Matchers.startsWith;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
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
import org.springframework.web.client.RestClient;

import edu.eci.dosw.oficioya.oficioya_service.security.JwtUtil;

class HttpWorkerClientTest {

    private HttpWorkerClient client;
    private MockRestServiceServer server;
    private final UUID workerId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        RestClient.Builder builder = RestClient.builder().baseUrl("https://worker.test");
        server = MockRestServiceServer.bindTo(builder).build();
        client = new HttpWorkerClient(builder.build());

        // El constructor (RestClient) no asigna jwtUtil, y exists() lo necesita
        // para generar el token de servicio.
        JwtUtil jwtUtil = new JwtUtil("clave-de-prueba-de-al-menos-32-caracteres-123456", "oficioya", 30);
        ReflectionTestUtils.setField(client, "jwtUtil", jwtUtil);
    }

    @Test
    @DisplayName("Un trabajador activo existe y se consulta con un token de servicio")
    void trabajadorActivoExiste() {
        server.expect(requestTo("https://worker.test/api/v1/workers/" + workerId))
                .andExpect(method(HttpMethod.GET))
                .andExpect(header(HttpHeaders.AUTHORIZATION, startsWith("Bearer ")))
                .andRespond(withSuccess(respuesta("ACTIVE"), MediaType.APPLICATION_JSON));

        assertTrue(client.exists(workerId, "Bearer token-de-usuario"));
        server.verify();
    }

    @Test
    @DisplayName("Un trabajador pausado no es destinatario de nuevas solicitudes")
    void trabajadorPausadoNoExiste() {
        server.expect(requestTo("https://worker.test/api/v1/workers/" + workerId))
                .andRespond(withSuccess(respuesta("PAUSED"), MediaType.APPLICATION_JSON));

        assertFalse(client.exists(workerId, "Bearer token"));
        server.verify();
    }

    @Test
    @DisplayName("Si Workers falla, el trabajador se descarta sin romper la creación")
    void errorDeWorkersDescartaTrabajador() {
        server.expect(requestTo("https://worker.test/api/v1/workers/" + workerId))
                .andRespond(withServerError());

        assertFalse(client.exists(workerId, "Bearer token"));
        server.verify();
    }

    @Test
    @DisplayName("La reserva y liberación de franjas no lanzan excepciones")
    void reservaYLiberacionDeFranja() {
        LocalDateTime slot = LocalDateTime.of(2026, 11, 25, 10, 0);

        assertDoesNotThrow(() -> client.reserveSlot(workerId, slot));
        assertDoesNotThrow(() -> client.releaseSlot(workerId, slot));
    }

    @Test
    @DisplayName("El constructor Spring crea el cliente con JwtUtil y base URL")
    void constructorSpring() {
        JwtUtil jwtUtil = new JwtUtil("clave-de-prueba-de-al-menos-32-caracteres-123456", "oficioya", 30);

        assertDoesNotThrow(() -> new HttpWorkerClient("https://worker.test", jwtUtil));
    }

    private String respuesta(String status) {
        return "{\"id\":\"" + workerId + "\",\"userId\":\"" + UUID.randomUUID()
                + "\",\"status\":\"" + status + "\"}";
    }
}