package edu.eci.dosw.oficioya.oficioya_service.client;

import java.net.http.HttpClient;
import java.time.Duration;
import java.util.ArrayList;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import edu.eci.dosw.oficioya.oficioya_service.dto.ImageResponse;
import edu.eci.dosw.oficioya.oficioya_service.dto.WorkerResponse;
import edu.eci.dosw.oficioya.oficioya_service.dto.WorkerStatus;

@Service
public class HttpWorkerClient implements WorkerClient {
    private RestClient restClient;
    private static final Logger log = LoggerFactory.getLogger(HttpNotificationService.class);

    @Autowired
    public HttpWorkerClient(@Value("${reputation.service.url:http://localhost:8080}") String baseUrl) {
        this(crearRestClient(baseUrl));
    }

    public HttpWorkerClient(RestClient restClient){
        this.restClient = restClient;
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
    public boolean exists(UUID workerId) {
        // TODO: llamar a GET /api/v1/workers/{workerId}
        WorkerResponse response = null;

        try {
            ResponseEntity<WorkerResponse> body = restClient.get()
                    .uri("/workers/{id}", workerId)
                    .retrieve()
                    .toEntity(WorkerResponse.class);
            
            response = body.getBody();
        } catch (Exception exception) {
            //log.warn("worker no respondió: {}", exception.getMessage());
            return false;
        }

        if(response.status() == WorkerStatus.PAUSED){
            return false;
        }
        else{
            return true;
        }
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
}