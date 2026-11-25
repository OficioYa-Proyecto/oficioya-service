package edu.eci.dosw.oficioya.oficioya_service.client;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Primary;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import edu.eci.dosw.oficioya.oficioya_service.security.JwtUtil;

/**
 * Implementación real del envío de notificaciones. Service sólo construye el
 * payload; el correo y el push los resuelve el microservicio de Notification.
 */
@Service
@Primary
public class HttpNotificationService implements NotificationService {

    private static final Logger log = LoggerFactory.getLogger(HttpNotificationService.class);

    private final RestTemplate restTemplate;
    private final String baseUrl;
    private final EmailActionLinkService emailActionLinkService;
    private final String configuredToken;
    private final JwtUtil jwtUtil;

    @Autowired
    public HttpNotificationService(@Value("${notification.service.url:http://localhost:8082}") String baseUrl,
                                   EmailActionLinkService emailActionLinkService,
                                   @Value("${service.token:}") String serviceToken,
                                   JwtUtil jwtUtil) {
        this(baseUrl, emailActionLinkService, serviceToken, jwtUtil, new RestTemplate());
    }

    HttpNotificationService(String baseUrl, EmailActionLinkService emailActionLinkService) {
        this(baseUrl, emailActionLinkService, "", null, new RestTemplate());
    }

    HttpNotificationService(String baseUrl, EmailActionLinkService emailActionLinkService,
                            String serviceToken, JwtUtil jwtUtil, RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
        this.baseUrl = baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
        this.emailActionLinkService = emailActionLinkService;
        this.configuredToken = serviceToken;
        this.jwtUtil = jwtUtil;
    }

    @Override
    public void notifyNewRequest(UUID workerId, UUID requestId) {
        notifyNewRequest(workerId, requestId, null, null);
    }

    @Override
    public void notifyNewRequest(UUID workerId, UUID requestId, String receiverEmail,
                                 RequestNotificationDetails details) {
        Map<String, Object> context = new HashMap<>();
        if (details != null) {
            if (details.scheduledDateTime() != null) {
                context.put("scheduledDateTime", details.scheduledDateTime().toString());
            }
            if (details.neighborhood() != null) {
                context.put("neighborhood", details.neighborhood());
            }
            if (details.exactAddress() != null) {
                context.put("exactAddress", details.exactAddress());
            }
            if (details.description() != null) {
                context.put("description", details.description());
            }
            if (details.imageUrl() != null) {
                context.put("imageUrl", details.imageUrl());
            }
        }
        sendWithActions(workerId, "INCOMING_REQUEST", requestId,
                "Nueva solicitud de servicio cerca de ti", receiverEmail, context);
    }

    @Override
    public void notifyRequestCreated(UUID contractorId, UUID requestId, String receiverEmail) {
        notifyRequestCreated(contractorId, requestId, receiverEmail, null);
    }

    @Override
    public void notifyRequestCreated(UUID contractorId, UUID requestId, String receiverEmail,
                                     RequestNotificationDetails details) {
        send(contractorId, "REQUEST_CREATED", requestId,
                "Tu solicitud fue creada y enviada a los trabajadores", receiverEmail, details);
    }

    @Override
    public void notifyAccepted(UUID contractorId, UUID requestId) {
        send(contractorId, "ACCEPTED_REQUEST", requestId, "Tu solicitud fue aceptada", null, null);
    }

    @Override
    public void notifyCancelled(UUID receiverId, UUID requestId) {
        send(receiverId, "CANCELLED_BY_CONTRACTOR", requestId, "Una solicitud fue cancelada", null, null);
    }

    @Override
    public void notifyCompleted(UUID contractorId, UUID requestId) {
        send(contractorId, "COMPLETED_WORK", requestId, "El trabajo fue reportado como terminado", null, null);
    }

    @Override
    public void notifyRejected(UUID contractorId, UUID requestId) {
        send(contractorId, "REJECTED_REQUEST", requestId, "El trabajo fue rechazado por el trabajador", null, null);
    }

    @Override
    public void notifyAccepted(UUID receiverId, UUID requestId, String receiverEmail) {
        send(receiverId, "ACCEPTED_REQUEST", requestId,
                "Tu solicitud fue aceptada", receiverEmail, null);
    }

    @Override
    public void notifyRejected(UUID receiverId, UUID requestId, String receiverEmail) {
        send(receiverId, "REJECTED_REQUEST", requestId,
                "El trabajo fue rechazado por el trabajador", receiverEmail, null);
    }

    @Override
    public void notifyCompleted(UUID receiverId, UUID requestId, String receiverEmail) {
        send(receiverId, "COMPLETED_WORK", requestId,
                "El trabajo fue reportado como terminado", receiverEmail, null);
    }

    @Override
    public void notifyCancelled(UUID receiverId, UUID requestId, String receiverEmail) {
        notifyCancelled(receiverId, requestId, receiverEmail, "CANCELLED_BY_CONTRACTOR");
    }

    @Override
    public void notifyCancelled(UUID receiverId, UUID requestId, String receiverEmail, String type) {
        String cancellationType = "CANCELLED_BY_WORKER".equals(type)
                ? "CANCELLED_BY_WORKER" : "CANCELLED_BY_CONTRACTOR";
        send(receiverId, cancellationType, requestId,
                "Una solicitud fue cancelada", receiverEmail, null);
    }

    @Override
    public void notifyRoleRemovalEmail(String type, UUID receiverId, String receiverEmail,
                                       UUID requestId, String subject, String body) {
        if (receiverEmail == null || receiverEmail.isBlank()) {
            log.warn("No se envió correo {} de quita de rol: destinatario vacío", type);
            return;
        }
        try {
            Map<String, Object> payload = new HashMap<>();
            payload.put("to", receiverEmail.trim());
            payload.put("subject", subject);
            payload.put("body", body);
            payload.put("type", type);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(payload, headers);
            restTemplate.postForEntity(baseUrl + "/api/v1/notification/email", entity, String.class);
            log.info("Correo {} de quita de rol enviado a {}", type, receiverEmail.trim());
        } catch (Exception e) {
            log.warn("No se pudo enviar correo {} de quita de rol a {}: {}", type, receiverEmail, e.getMessage());
        }
    }

    private void send(UUID receiverId, String type, UUID requestId, String message, String receiverEmail,
                      RequestNotificationDetails details) {
        try {
            Map<String, Object> payload = new HashMap<>();
            payload.put("receiverId", receiverId);
            payload.put("type", type);
            payload.put("requestId", requestId);
            payload.put("message", message);
            if (receiverEmail != null && !receiverEmail.isBlank()) {
                payload.put("receiverEmail", receiverEmail.trim());
            }
            if (details != null) {
                if (details.scheduledDateTime() != null) {
                    payload.put("scheduledDateTime", details.scheduledDateTime().toString());
                }
                payload.put("neighborhood", details.neighborhood());
                payload.put("exactAddress", details.exactAddress());
                payload.put("description", details.description());
                if (details.imageUrl() != null) {
                    payload.put("imageUrl", details.imageUrl());
                }
            }

            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(payload, buildHeaders());
            restTemplate.postForEntity(baseUrl + "/api/v1/notification", entity, String.class);
        } catch (Exception e) {
            log.warn("No se pudo enviar notificación {} a {} vía {}: {}", type, receiverId, baseUrl, e.getMessage());
        }
    }

    /**
     * Notification exige JWT (rol SERVICE) desde el cambio de seguridad. Se reutiliza
     * el mismo token de servicio que Users: configurable por SERVICE_TOKEN o generado
     * con el secreto compartido.
     */
    private HttpHeaders buildHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        String token = (configuredToken != null && !configuredToken.isBlank())
                ? configuredToken
                : (jwtUtil == null ? null : jwtUtil.generateServiceToken());
        if (token != null && !token.isBlank()) {
            headers.setBearerAuth(token.trim());
        }
        return headers;
    }

    /** Igual que {@link #send} pero agrega los enlaces firmados de Aceptar y Rechazar. */
    private void sendWithActions(UUID receiverId, String type, UUID requestId, String message,
                                 String receiverEmail, Map<String, Object> context) {
        try {
            Map<String, Object> payload = new HashMap<>();
            payload.put("receiverId", receiverId);
            payload.put("type", type);
            payload.put("requestId", requestId);
            payload.put("message", message);
            if (receiverEmail != null && !receiverEmail.isBlank()) {
                payload.put("receiverEmail", receiverEmail.trim());
            }
            payload.putAll(context);
            payload.put("acceptUrl", emailActionLinkService.acceptUrl(receiverId, requestId));
            payload.put("rejectUrl", emailActionLinkService.rejectUrl(receiverId, requestId));

            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(payload, buildHeaders());
            restTemplate.postForEntity(baseUrl + "/api/v1/notification", entity, String.class);
        } catch (Exception e) {
            log.warn("No se pudo enviar notificación {} a {} vía {}: {}", type, receiverId, baseUrl, e.getMessage());
        }
    }
}
