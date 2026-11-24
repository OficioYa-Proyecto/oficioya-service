package edu.eci.dosw.oficioya.oficioya_service.client;

import java.util.List;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import edu.eci.dosw.oficioya.oficioya_service.dto.UserResponse;
import edu.eci.dosw.oficioya.oficioya_service.security.JwtUtil;

/**
 * Cliente HTTP hacia Users. Si no hay un token estático configurado, genera un
 * token de servicio de corta duración con el mismo secreto JWT que el
 * orquestador, para poder resolver el correo del contratante al crear una
 * solicitud y pasárselo a Notification.
 */
@Service
public class HttpUserClient implements UserClient {

    private static final String ACCOUNT_USERS_PATH = "/api/v1/users/";
    private static final String ACCOUNT_SUFFIX = "/account";
    private static final String ADMIN_USERS_PATH = "/api/v1/admin/users/";

    private final RestTemplate restTemplate;
    private final String baseUrl;
    private final String configuredToken;
    private final JwtUtil jwtUtil;

    @Autowired
    public HttpUserClient(
            @Value("${user.service.url:http://localhost:8081}") String baseUrl,
            @Value("${service.token:}") String serviceToken,
            JwtUtil jwtUtil) {
        this(baseUrl, serviceToken, jwtUtil, new RestTemplate());
    }

    HttpUserClient(String baseUrl, String serviceToken, JwtUtil jwtUtil, RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
        this.baseUrl = baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
        this.configuredToken = serviceToken;
        this.jwtUtil = jwtUtil;
    }

    HttpUserClient(String baseUrl, String serviceToken) {
        this(baseUrl, serviceToken, null, new RestTemplate());
    }

    @Override
    public UserResponse getUser(UUID userId) {
        String token = resolveToken();
        if (token == null) {
            return null;
        }
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token);
        headers.setAccept(List.of(MediaType.APPLICATION_JSON));
        HttpEntity<Void> entity = new HttpEntity<>(headers);
        try {
            // Users permite este endpoint con el rol SERVICE, que es el que usa el orquestador.
            ResponseEntity<UserResponse> response = restTemplate.exchange(
                    baseUrl + ACCOUNT_USERS_PATH + userId + ACCOUNT_SUFFIX,
                    HttpMethod.GET,
                    entity,
                    UserResponse.class);
            return response.getBody();
        } catch (HttpClientErrorException e) {
            // Versiones anteriores sólo exponían el perfil completo bajo /admin.
            try {
                ResponseEntity<UserResponse> response = restTemplate.exchange(
                        baseUrl + ADMIN_USERS_PATH + userId,
                        HttpMethod.GET,
                        entity,
                        UserResponse.class);
                return response.getBody();
            } catch (HttpClientErrorException adminError) {
                return null;
            }
        }
    }

    private String resolveToken() {
        if (configuredToken != null && !configuredToken.isBlank()) {
            return configuredToken;
        }
        return jwtUtil == null ? null : jwtUtil.generateServiceToken();
    }
}
