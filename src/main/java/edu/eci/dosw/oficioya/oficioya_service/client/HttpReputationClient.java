package edu.eci.dosw.oficioya.oficioya_service.client;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import edu.eci.dosw.oficioya.oficioya_service.security.JwtUtil;

@Service
public class HttpReputationClient implements ReputationClient {

    private final RestTemplate restTemplate;
    private final String baseUrl;
    private static final Logger log = LoggerFactory.getLogger(HttpReputationClient.class);
    private JwtUtil jwtUtil;

    public HttpReputationClient(@Value("${reputation.service.url:http://localhost:8080}") String baseUrl, JwtUtil jwtUtil) {
        this.restTemplate = new RestTemplate();
        this.baseUrl = baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
        this.jwtUtil = jwtUtil;
    }

    @Override
    public void penalize(UUID workerId, double points) {
        log.warn("Entró a donde no es");
        // Delegado al evento de cancelación tardía
        System.out.println("Penalizar reputacion " + points + " para worker " + workerId);
    }

    @Override
    public void notifyLateCancellation(UUID solicitudId, UUID trabajadorId, LocalDateTime fechaHoraServicio, LocalDateTime fechaCancelacion) {
        log.info("Notificando en reputación una cancelación tardía en solicitud: {}, worker: {}, con hora de trabajo: {}, cancelada en:{}",solicitudId,trabajadorId,fechaHoraServicio,fechaCancelacion);
        try {
            Map<String, Object> payload = Map.of(
                    "solicitudId", solicitudId,
                    "trabajadorId", trabajadorId,
                    "fechaHoraServicio", fechaHoraServicio.toString(),
                    "fechaCancelacion", fechaCancelacion.toString()
            );
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setBearerAuth(jwtUtil.generateServiceToken());
            restTemplate.postForEntity(baseUrl + "/api/v1/reputation/eventos/cancelaciones-tardias", new HttpEntity<>(payload, headers), String.class);
            log.info("Servicio notificado");
        } catch (Exception e) {
            log.error("Error notificando cancelacion tardia: " + e);
        }
    }

    @Override
    public void notifyServiceCompleted(UUID solicitudId, UUID trabajadorId, UUID contratanteId, LocalDateTime fechaCumplida) {
        log.info("Notificando en reputation el servicio completado con id: {}, worker: {}, contractor: {}",solicitudId,trabajadorId,contratanteId);
        try {
            Map<String, Object> payload = Map.of(
                    "solicitudId", solicitudId,
                    "trabajadorId", trabajadorId,
                    "contratanteId", contratanteId,
                    "fechaCumplida", fechaCumplida.toString()
            );
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setBearerAuth(jwtUtil.generateServiceToken());
            restTemplate.postForEntity(baseUrl + "/api/v1/reputation/eventos/servicios-cumplidos", new HttpEntity<>(payload, headers), String.class);
            log.info("Servicio notificado");
        } catch (Exception e) {
            log.error("Error notificando servicio cumplido: " + e);
        }
    }
}