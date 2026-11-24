package edu.eci.dosw.oficioya.oficioya_service.client;

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
import edu.eci.dosw.oficioya.oficioya_service.dto.ImageResponse;

@Service 
public class HttpImageClient implements ImageClient{
    private RestClient restClient;

    @Autowired
    public HttpImageClient(@Value("${image.service.url:http://localhost:8083}") String baseUrl) {
        this(crearRestClient(baseUrl));
    }

    public HttpImageClient(RestClient restClient){
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
    public List<ImageResponse> getImages(UUID rootId){
        List<ImageResponse> body = new ArrayList<>();
        
        try {
            body = restClient.get()
                    .uri(uri -> uri.path("/by-root/{rootId}")
                        .queryParam("tipo", "REQUESTS")
                        .build(rootId))
                    
                    .retrieve()
                    .body(new ParameterizedTypeReference<List<ImageResponse>>() {});
        } catch (RestClientException exception) {
            //log.warn("Images no respondió: {}", exception.getMessage());
        }

        return body;
    }

    @Override
    public void uploadImages(List<MultipartFile> files, UUID rootId, String referenciaExterna, String authHeader) {
        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();

        for (MultipartFile file : files) {
        body.add("files", file.getResource());
        }
        body.add("rootId", rootId.toString());
        body.add("tipo", "REQUESTS");
        if (referenciaExterna != null) {
            body.add("referenciaExterna", referenciaExterna);
        }
        restClient.post()
            .uri("/batch")
            .header(HttpHeaders.AUTHORIZATION, authHeader)
            .contentType(MediaType.MULTIPART_FORM_DATA)
            .body(body)
            .retrieve()
            .toBodilessEntity();
}
}
