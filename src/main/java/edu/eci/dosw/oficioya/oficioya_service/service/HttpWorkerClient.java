package edu.eci.dosw.oficioya.oficioya_service.service;

import java.util.Map;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

@Service
public class HttpWorkerClient implements WorkerClient {

    private final RestTemplate restTemplate;
    private final String baseUrl;
    private final String serviceToken;

    public HttpWorkerClient(
            @Value("${worker.service.url:http://localhost:8083}") String baseUrl,
            @Value("${service.token:}") String serviceToken) {
        this.restTemplate = new RestTemplate();
        this.baseUrl = baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
        this.serviceToken = serviceToken;
    }

    @Override
    public boolean exists(UUID workerId) {
        // TODO: llamar a GET /api/v1/workers/{workerId}
        System.out.println("Verificar worker: " + workerId);
        return true;
    }

    @Override
    public void reserveSlot(UUID workerId, java.time.LocalDateTime slot) {
        // TODO: POST /workers/{workerId}/availability-exceptions
        System.out.println("Reservar franja para worker " + workerId + " en " + slot);
    }

    @Override
    public void releaseSlot(UUID workerId, java.time.LocalDateTime slot) {
        // TODO: DELETE /workers/{workerId}/availability-exceptions/{id}
        System.out.println("Liberar franja para worker " + workerId + " en " + slot);
    }

    @Override
    public String getTrade(UUID workerId) {
        if (serviceToken == null || serviceToken.isBlank()) {
            return null;
        }
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setBearerAuth(serviceToken);
            headers.setAccept(java.util.List.of(MediaType.APPLICATION_JSON));
            ResponseEntity<Map> response = restTemplate.exchange(
                    baseUrl + "/api/v1/workers/" + workerId,
                    HttpMethod.GET,
                    new HttpEntity<>(headers),
                    Map.class);
            Map body = response.getBody();
            if (body == null) return null;
            Object trade = body.get("trade");
            if (trade == null) trade = body.get("oficio");
            if (trade == null) trade = body.get("tradeName");
            return trade != null ? trade.toString() : null;
        } catch (Exception e) {
            System.out.println("Error obteniendo trade del worker " + workerId + ": " + e.getMessage());
            return null;
        }
    }
}
