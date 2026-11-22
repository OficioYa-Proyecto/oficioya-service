package edu.eci.dosw.oficioya.oficioya_service.service;

import java.net.URI;
import java.util.List;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import edu.eci.dosw.oficioya.oficioya_service.dto.ImageRequest;
import edu.eci.dosw.oficioya.oficioya_service.dto.ImageResponse;

/**
 * Cliente HTTP del microservicio de imágenes. Todas las llamadas pasan por el
 * Orchestrator (/api/v1/images/**), que enruta al microservicio y reenvía el JWT del usuario.
 */
@Service
public class HttpImageClient implements ImageClient {

    private static final Logger log = LoggerFactory.getLogger(HttpImageClient.class);
    private static final String IMAGE_TYPE = "REQUEST";

    private final RestTemplate restTemplate;
    private final String baseUrl;

    @Autowired
    public HttpImageClient(@Value("${images.service.url:https://oficioya-orchestor-api-ghaxfaacfabxh4bv.chilecentral-01.azurewebsites.net}") String baseUrl) {
        this(new RestTemplate(), baseUrl);
    }

    HttpImageClient(RestTemplate restTemplate, String baseUrl) {
        this.restTemplate = restTemplate;
        this.baseUrl = baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
    }

    @Override
    public List<ImageResponse> uploadImages(List<ImageRequest> images, UUID rootId, String token) {
        if (images == null || images.isEmpty()) {
            return List.of();
        }
        try {
            MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
            for (ImageRequest image : images) {
                byte[] bytes = image.datos() == null ? new byte[0] : image.datos();
                ByteArrayResource file = new ByteArrayResource(bytes) {
                    @Override
                    public String getFilename() {
                        return image.nombre() == null ? "image" : image.nombre();
                    }
                };
                body.add("files", file);
            }
            body.add("rootId", rootId.toString());
            body.add("tipo", IMAGE_TYPE);
            String referencia = images.stream()
                    .map(ImageRequest::referenciaExterna)
                    .filter(value -> value != null && !value.isBlank())
                    .findFirst()
                    .orElse(null);
            if (referencia != null) {
                body.add("referenciaExterna", referencia);
            }

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.MULTIPART_FORM_DATA);
            applyToken(headers, token);

            ResponseEntity<ImageResponse[]> response = restTemplate.exchange(
                    baseUrl + "/api/v1/images/batch",
                    HttpMethod.POST,
                    new HttpEntity<>(body, headers),
                    ImageResponse[].class);
            return toList(response.getBody());
        } catch (Exception e) {
            log.warn("No se pudieron subir las imágenes de la solicitud {}: {}", rootId, e.getMessage());
            return List.of();
        }
    }

    @Override
    public List<ImageResponse> getRequestImages(UUID rootId, String token) {
        try {
            URI uri = UriComponentsBuilder.fromUriString(baseUrl)
                    .path("/api/v1/images/by-root/{rootId}")
                    .queryParam("tipo", IMAGE_TYPE)
                    .buildAndExpand(rootId)
                    .toUri();

            HttpHeaders headers = new HttpHeaders();
            applyToken(headers, token);

            ResponseEntity<ImageResponse[]> response = restTemplate.exchange(
                    uri,
                    HttpMethod.GET,
                    new HttpEntity<>(headers),
                    ImageResponse[].class);
            return toList(response.getBody());
        } catch (Exception e) {
            log.warn("No se pudieron consultar las imágenes de la solicitud {}: {}", rootId, e.getMessage());
            return List.of();
        }
    }

    private void applyToken(HttpHeaders headers, String token) {
        if (token != null && !token.isBlank()) {
            headers.setBearerAuth(token);
        }
    }

    private List<ImageResponse> toList(ImageResponse[] body) {
        return body == null ? List.of() : List.of(body);
    }
}
