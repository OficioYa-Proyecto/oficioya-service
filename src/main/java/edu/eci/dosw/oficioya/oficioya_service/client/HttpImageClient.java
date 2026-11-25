package edu.eci.dosw.oficioya.oficioya_service.client;

import java.net.http.HttpClient;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.multipart.MultipartFile;

import edu.eci.dosw.oficioya.oficioya_service.dto.ImageResponse;
import edu.eci.dosw.oficioya.oficioya_service.security.JwtUtil;

@Service
public class HttpImageClient implements ImageClient {

    private final RestClient restClient;
    private final String configuredToken;
    private final JwtUtil jwtUtil;

    @Autowired
    public HttpImageClient(@Value("${image.service.url:http://localhost:8083}") String baseUrl,
                           @Value("${service.token:}") String serviceToken,
                           JwtUtil jwtUtil) {
        this(crearRestClient(baseUrl), serviceToken, jwtUtil);
    }

    public HttpImageClient(String baseUrl) {
        this(crearRestClient(baseUrl), null, null);
    }

    public HttpImageClient(RestClient restClient) {
        this(restClient, null, null);
    }

    public HttpImageClient(RestClient restClient, String serviceToken, JwtUtil jwtUtil) {
        this.restClient = restClient;
        this.configuredToken = serviceToken;
        this.jwtUtil = jwtUtil;
    }

    private static RestClient crearRestClient(String url) {
        HttpClient httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(15))
                .build();
        JdkClientHttpRequestFactory factory = new JdkClientHttpRequestFactory(httpClient);
        factory.setReadTimeout(Duration.ofSeconds(30));
        return RestClient.builder().baseUrl(url).requestFactory(factory).build();
    }

    @Override
    public List<ImageResponse> getImages(UUID rootId) {
        List<ImageResponse> body = new ArrayList<>();

        try {
            RestClient.RequestHeadersSpec<?> spec = restClient.get()
                    .uri(uri -> uri.path("/api/v1/by-root/{rootId}")
                        .queryParam("tipo", "REQUESTS")
                        .build(rootId));
            String token = resolveToken();
            if (token != null && !token.isBlank()) {
                spec.header(HttpHeaders.AUTHORIZATION, "Bearer " + token.trim());
            }
            body = spec.retrieve()
                    .body(new ParameterizedTypeReference<List<ImageResponse>>() {});
        } catch (RestClientException exception) {
            // Images no responde: se crea la solicitud sin imagen.
        }

        return body == null ? new ArrayList<>() : body;
    }

    /**
     * El listado de imágenes está autenticado. Se reutiliza el token de servicio
     * del orquestador, o se genera uno de corta duración con el mismo secreto JWT.
     */
    private String resolveToken() {
        if (configuredToken != null && !configuredToken.isBlank()) {
            return configuredToken;
        }
        return jwtUtil == null ? null : jwtUtil.generateServiceToken();
    }

    @Override
    public void uploadImages(List<MultipartFile> files, UUID rootId, String referenciaExterna, String authHeader) {
        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();

        for (MultipartFile file : files) {
            body.add("files", file.getResource());
        }
        body.add("rootId", rootId.toString());
        body.add("tipo", "REQUESTS");
        if (referenciaExterna != null) {
            body.add("referenciaExterna", referenciaExterna);
        }
        restClient.post()
            .uri("/api/v1/batch")
            .header(HttpHeaders.AUTHORIZATION, authHeader)
            .contentType(MediaType.MULTIPART_FORM_DATA)
            .body(body)
            .retrieve()
            .toBodilessEntity();
    }
}
