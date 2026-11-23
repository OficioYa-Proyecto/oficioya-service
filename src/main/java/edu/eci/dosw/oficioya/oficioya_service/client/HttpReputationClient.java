package edu.eci.dosw.oficioya.oficioya_service.client;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

@Service
public class HttpReputationClient implements ReputationClient {

    private final RestTemplate restTemplate;
    private final String baseUrl;

    public HttpReputationClient(@Value("${reputation.service.url:http://localhost:8080}") String baseUrl) {
        this.restTemplate = new RestTemplate();
        this.baseUrl = baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
    }

    @Override
    public void penalize(UUID workerId, double points) {
        // Delegado al evento de cancelación tardía
        System.out.println("Penalizar reputacion " + points + " para worker " + workerId);
    }

    @Override
    public void notifyLateCancellation(UUID solicitudId, UUID trabajadorId, LocalDateTime fechaHoraServicio, LocalDateTime fechaCancelacion) {
        try {
            Map<String, Object> payload = Map.of(
                    "solicitudId", solicitudId,
                    "trabajadorId", trabajadorId,
                    "fechaHoraServicio", fechaHoraServicio.toString(),
                    "fechaCancelacion", fechaCancelacion.toString()
            );
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            restTemplate.postForEntity(baseUrl + "/api/v1/reputation/eventos/cancelaciones-tardias", new HttpEntity<>(payload, headers), String.class);
        } catch (Exception e) {
            System.out.println("Error notificando cancelacion tardia: " + e.getMessage());
        }
    }

    @Override
    public void notifyServiceCompleted(UUID solicitudId, UUID trabajadorId, UUID contratanteId, LocalDateTime fechaCumplida) {
        try {
            Map<String, Object> payload = Map.of(
                    "solicitudId", solicitudId,
                    "trabajadorId", trabajadorId,
                    "contratanteId", contratanteId,
                    "fechaCumplida", fechaCumplida.toString()
            );
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            restTemplate.postForEntity(baseUrl + "/api/v1/reputation/eventos/servicios-cumplidos", new HttpEntity<>(payload, headers), String.class);
        } catch (Exception e) {
            System.out.println("Error notificando servicio cumplido: " + e.getMessage());
        }
    }
}