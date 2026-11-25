package edu.eci.dosw.oficioya.oficioya_service.client;

import java.net.URI;
import java.net.http.HttpClient;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
import org.springframework.web.util.UriBuilder;

import edu.eci.dosw.oficioya.oficioya_service.dto.ImageResponse;
import edu.eci.dosw.oficioya.oficioya_service.security.JwtUtil;

@Service
public class HttpImageClient implements ImageClient {

    private static final Logger log = LoggerFactory.getLogger(HttpImageClient.class);

    private final RestClient restClient;
    private final String baseUrl;
    private final String configuredToken;
    private final JwtUtil jwtUtil;

    @Autowired
    public HttpImageClient(@Value("${image.service.url:http://localhost:8083}") String baseUrl,
                           @Value("${service.token:}") String serviceToken,
                           JwtUtil jwtUtil) {
        this(crearRestClient(baseUrl), normalizarBaseUrl(baseUrl), serviceToken, jwtUtil);
    }

    public HttpImageClient(String baseUrl) {
        this(crearRestClient(baseUrl), normalizarBaseUrl(baseUrl), null, null);
    }

    public HttpImageClient(RestClient restClient) {
        this(restClient, null, null, null);
    }

    public HttpImageClient(RestClient restClient, String serviceToken, JwtUtil jwtUtil) {
        this(restClient, null, serviceToken, jwtUtil);
    }

    public HttpImageClient(RestClient restClient, String baseUrl, String serviceToken, JwtUtil jwtUtil) {
        this.restClient = restClient;
        this.baseUrl = normalizarBaseUrl(baseUrl);
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

    private static String normalizarBaseUrl(String url) {
        if (url == null || url.isBlank()) {
            return null;
        }
        String base = url.trim();
        if (!base.matches("(?i)^https?://.*")) {
            base = "https://" + base;
        }
        return base.endsWith("/") ? base.substring(0, base.length() - 1) : base;
    }

    @Override
    public List<ImageResponse> getImages(UUID rootId) {
        return getImages(rootId, null);
    }

    @Override
    public List<ImageResponse> getImages(UUID rootId, String authHeader) {
        List<ImageResponse> body = new ArrayList<>();

        try {
            RestClient.RequestHeadersSpec<?> spec = restClient.get()
                    .uri(uri -> uri.path("/api/v1/images/by-root/{rootId}")
                        .queryParam("tipo", "REQUEST")
                        .build(rootId));
            String token = resolveToken(authHeader);
            if (token != null && !token.isBlank()) {
                spec.header(HttpHeaders.AUTHORIZATION, "Bearer " + token);
            }
            body = spec.retrieve()
                    .body(new ParameterizedTypeReference<List<ImageResponse>>() {});
        } catch (RestClientException exception) {
            log.warn("No se pudieron obtener las imágenes de la solicitud {}: {}",
                    rootId, exception.getMessage());
            return new ArrayList<>();
        }

        if (body == null) {
            return new ArrayList<>();
        }
        // Images devuelve una URL relativa (/api/v1/images/{id}); el correo necesita
        // una URL absoluta para que el cliente de correo pueda descargar la foto.
        return body.stream().map(this::conUrlAbsoluta).toList();
    }

    private ImageResponse conUrlAbsoluta(ImageResponse image) {
        return new ImageResponse(
                image.id(),
                image.rootId(),
                image.nombre(),
                image.tipoContenido(),
                image.tamano(),
                image.fechaCarga(),
                image.referenciaExterna(),
                urlAbsoluta(image.url()),
                image.subidoPor());
    }

    private String urlAbsoluta(String url) {
        if (url == null || url.isBlank() || baseUrl == null || baseUrl.isBlank()) {
            return url;
        }
        try {
            if (URI.create(url).isAbsolute()) {
                return url;
            }
        } catch (IllegalArgumentException ignored) {
            // Se trata como relativa.
        }
        return url.startsWith("/") ? baseUrl + url : baseUrl + "/" + url;
    }

    /**
     * Images exige autenticación para listar por rootId. Se prefiere el token del
     * usuario que subió la imagen; si no viene, se reutiliza el token de servicio
     * configurado o se genera uno con el secreto compartido.
     */
    private String resolveToken(String authHeader) {
        if (authHeader != null && !authHeader.isBlank()) {
            return sinPrefijoBearer(authHeader);
        }
        if (configuredToken != null && !configuredToken.isBlank()) {
            return sinPrefijoBearer(configuredToken);
        }
        return jwtUtil == null ? null : jwtUtil.generateServiceToken();
    }

    private static String sinPrefijoBearer(String value) {
        return value.trim().replaceFirst("(?i)^Bearer\\s+", "");
    }

    @Override
    public void uploadImages(List<MultipartFile> files, UUID rootId, String referenciaExterna, String authHeader) {
        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();

        for (MultipartFile file : files) {
            body.add("files", file.getResource());
        }
        RestClient.RequestBodySpec spec = restClient.post()
            .uri(uri -> {
                UriBuilder builder = uri.path("/api/v1/images/batch")
                    .queryParam("rootId", rootId)
                    .queryParam("tipo", "REQUEST");
                if (referenciaExterna != null) {
                    builder.queryParam("referenciaExterna", referenciaExterna);
                }
                return builder.build();
            });
        String token = resolveToken(authHeader);
        if (token != null && !token.isBlank()) {
            spec.header(HttpHeaders.AUTHORIZATION, "Bearer " + token);
        }
        spec.contentType(MediaType.MULTIPART_FORM_DATA)
            .body(body)
            .retrieve()
            .toBodilessEntity();
    }
}
