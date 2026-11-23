package edu.eci.dosw.oficioya.oficioya_service.client;

import java.net.http.HttpClient;
import java.time.Duration;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

@Service
public class HttpWorkerClient implements WorkerClient {
    private RestClient restClient;

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
}