package edu.eci.dosw.oficioya.oficioya_service.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestTemplate;

import edu.eci.dosw.oficioya.oficioya_service.dto.ImageRequest;
import edu.eci.dosw.oficioya.oficioya_service.dto.ImageResponse;

class HttpImageClientTest {

    private static final String BASE = "https://orchestrator.test";

    private MockRestServiceServer server;
    private HttpImageClient client;

    @BeforeEach
    void setUp() {
        RestTemplate restTemplate = new RestTemplate();
        server = MockRestServiceServer.bindTo(restTemplate).build();
        client = new HttpImageClient(restTemplate, BASE);
    }

    @Test
    @DisplayName("sube imágenes como multipart con el token del usuario")
    void subeImagenes() {
        UUID rootId = UUID.randomUUID();
        UUID imageId = UUID.randomUUID();
        server.expect(requestTo(BASE + "/api/v1/images/batch"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header(HttpHeaders.AUTHORIZATION, "Bearer jwt-token"))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("rootId")))
                .andRespond(withSuccess("[{\"id\":\"" + imageId + "\"}]", MediaType.APPLICATION_JSON));

        List<ImageResponse> result = client.uploadImages(
                List.of(new ImageRequest("a.png", "image/png", new byte[] { 1 }, null)),
                rootId,
                "jwt-token");

        assertEquals(1, result.size());
        assertEquals(imageId, result.get(0).id());
        server.verify();
    }

    @Test
    @DisplayName("consulta las imágenes por rootId filtrando por tipo REQUEST")
    void consultaImagenes() {
        UUID rootId = UUID.randomUUID();
        server.expect(requestTo(BASE + "/api/v1/images/by-root/" + rootId + "?tipo=REQUEST"))
                .andExpect(method(HttpMethod.GET))
                .andExpect(header(HttpHeaders.AUTHORIZATION, "Bearer jwt-token"))
                .andRespond(withSuccess("[]", MediaType.APPLICATION_JSON));

        assertTrue(client.getRequestImages(rootId, "jwt-token").isEmpty());
        server.verify();
    }

    @Test
    @DisplayName("devuelve lista vacía si el microservicio falla")
    void falloServicio() {
        UUID rootId = UUID.randomUUID();
        server.expect(requestTo(BASE + "/api/v1/images/by-root/" + rootId + "?tipo=REQUEST"))
                .andRespond(withServerError());

        assertTrue(client.getRequestImages(rootId, "jwt-token").isEmpty());
        server.verify();
    }
}
