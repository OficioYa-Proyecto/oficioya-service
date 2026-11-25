package edu.eci.dosw.oficioya.oficioya_service.client;

import static org.hamcrest.Matchers.startsWith;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
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
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;
import org.springframework.web.multipart.MultipartFile;

import edu.eci.dosw.oficioya.oficioya_service.dto.ImageResponse;
import edu.eci.dosw.oficioya.oficioya_service.security.JwtUtil;

class HttpImageClientTest {

    private HttpImageClient client;
    private MockRestServiceServer server;
    private final UUID rootId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        RestClient.Builder builder = RestClient.builder().baseUrl("https://images.test");
        server = MockRestServiceServer.bindTo(builder).build();
        client = new HttpImageClient(builder.build(), "service-token", null);
    }

    @Test
    @DisplayName("Obtiene las imágenes de una solicitud filtrando por el tipo REQUEST")
    void obtieneImagenes() {
        String url = "https://images.test/api/v1/images/by-root/" + rootId + "?tipo=REQUEST";
        server.expect(requestTo(url))
                .andExpect(method(HttpMethod.GET))
                .andExpect(header(HttpHeaders.AUTHORIZATION, "Bearer service-token"))
                .andRespond(withSuccess("[{\"id\":\"" + UUID.randomUUID()
                        + "\",\"rootId\":\"" + rootId
                        + "\",\"nombre\":\"fuga.jpg\",\"tipoContenido\":\"image/jpeg\","
                        + "\"tamano\":10,\"fechaCarga\":\"2026-11-24T10:00:00\","
                        + "\"referenciaExterna\":null,\"url\":\"https://images.test/api/v1/fuga.jpg\","
                        + "\"subidoPor\":\"" + UUID.randomUUID() + "\"}]", MediaType.APPLICATION_JSON));

        List<ImageResponse> images = client.getImages(rootId);

        assertEquals(1, images.size());
        assertEquals("fuga.jpg", images.get(0).nombre());
        server.verify();
    }

    @Test
    @DisplayName("Sin token configurado genera un token de servicio para consultar las imágenes")
    void obtieneImagenesConTokenDeServicio() {
        JwtUtil jwtUtil = mock(JwtUtil.class);
        when(jwtUtil.generateServiceToken()).thenReturn("generated-token");
        RestClient.Builder builder = RestClient.builder().baseUrl("https://images.test");
        server = MockRestServiceServer.bindTo(builder).build();
        client = new HttpImageClient(builder.build(), null, jwtUtil);

        server.expect(requestTo("https://images.test/api/v1/images/by-root/" + rootId + "?tipo=REQUEST"))
                .andExpect(header(HttpHeaders.AUTHORIZATION, "Bearer generated-token"))
                .andRespond(withSuccess("[]", MediaType.APPLICATION_JSON));

        assertTrue(client.getImages(rootId).isEmpty());
        server.verify();
    }

    @Test
    @DisplayName("Si Images no responde, la solicitud se crea sin imágenes")
    void errorDeImagesDevuelveListaVacia() {
        server.expect(requestTo("https://images.test/api/v1/images/by-root/" + rootId + "?tipo=REQUEST"))
                .andRespond(withServerError());

        assertTrue(client.getImages(rootId).isEmpty());
        server.verify();
    }

    @Test
    @DisplayName("Sube las imágenes en multipart con la referencia externa y el token")
    void subeImagenesConReferencia() {
        List<MultipartFile> files = List.of(new MockMultipartFile("images", "fuga.jpg", "image/jpeg", new byte[]{1, 2}));

        server.expect(requestTo("https://images.test/api/v1/images/batch?rootId=" + rootId
                        + "&tipo=REQUEST&referenciaExterna=REF-1"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header(HttpHeaders.AUTHORIZATION, "Bearer token"))
                .andExpect(header(HttpHeaders.CONTENT_TYPE, startsWith("multipart/form-data")))
                .andRespond(withSuccess("{}", MediaType.APPLICATION_JSON));

        assertDoesNotThrow(() -> client.uploadImages(files, rootId, "REF-1", "Bearer token"));
        server.verify();
    }

    @Test
    @DisplayName("La subida también funciona cuando no hay referencia externa")
    void subeImagenesSinReferencia() {
        List<MultipartFile> files = List.of(new MockMultipartFile("images", "fuga.jpg", "image/jpeg", new byte[]{1}));

        server.expect(requestTo("https://images.test/api/v1/images/batch?rootId=" + rootId
                        + "&tipo=REQUEST"))
                .andRespond(withSuccess("{}", MediaType.APPLICATION_JSON));

        assertDoesNotThrow(() -> client.uploadImages(files, rootId, null, "Bearer token"));
        server.verify();
    }

    @Test
    @DisplayName("El constructor Spring crea el cliente con la URL configurada")
    void constructorSpring() {
        assertDoesNotThrow(() -> new HttpImageClient("https://images.test"));
    }
}
