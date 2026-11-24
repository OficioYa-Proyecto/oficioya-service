package edu.eci.dosw.oficioya.oficioya_service.client;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import edu.eci.dosw.oficioya.oficioya_service.security.JwtUtil;

/** Construye los enlaces firmados que aparecen como botones en el correo del trabajador. */
@Service
public class EmailActionLinkService {

    private final JwtUtil jwtUtil;
    private final String serviceBaseUrl;

    public EmailActionLinkService(JwtUtil jwtUtil,
                                  @Value("${app.public-url:http://localhost:8086}") String serviceBaseUrl) {
        this.jwtUtil = jwtUtil;
        this.serviceBaseUrl = serviceBaseUrl.endsWith("/")
                ? serviceBaseUrl.substring(0, serviceBaseUrl.length() - 1)
                : serviceBaseUrl;
    }

    public String acceptUrl(UUID workerId, UUID requestId) {
        return build("ACCEPT", "accept", workerId, requestId);
    }

    public String rejectUrl(UUID workerId, UUID requestId) {
        return build("REJECT", "reject", workerId, requestId);
    }

    private String build(String action, String path, UUID workerId, UUID requestId) {
        String token = jwtUtil.generateEmailActionToken(workerId, requestId, action);
        return serviceBaseUrl + "/api/v1/requests/email/" + path
                + "?requestId=" + requestId
                + "&token=" + URLEncoder.encode(token, StandardCharsets.UTF_8);
    }
}
