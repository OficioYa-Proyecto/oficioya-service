package edu.eci.dosw.oficioya.oficioya_service.client;

import edu.eci.dosw.oficioya.oficioya_service.dto.ImageResponse;
import java.net.http.HttpClient;
import java.net.http.HttpHeaders;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import javax.naming.ServiceUnavailableException;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.MediaType;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;

@Service 
public class HttpImageClient implements ImageClient{
    private RestClient restClient;

    @Autowired
    public HttpImageClient(@Value("${reputation.service.url:http://localhost:8080}") String baseUrl) {
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
                            .queryParam("tipo", rootId)
                            .build(rootId))
                    
                    .retrieve()
                    .body(new ParameterizedTypeReference<List<ImageResponse>>() {});
        } catch (RestClientException exception) {
            //log.warn("Images no respondió: {}", exception.getMessage());
        }

        return body;
    }

    @Override 
    public void uploadImages(List<MultipartFile> files, UUID rootId, String referenciaExterna, Authentication authentication){
        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();

        for (MultipartFile file : files) {
            body.add("files", file.getResource());
        }

        body.add("rootId", rootId.toString());
        body.add("tipo", "REQUESTS"); 

        if (referenciaExterna != null) {
            body.add("referenciaExterna", referenciaExterna);
        }

        try {
            restClient.post()
                .uri("/batch") 
                .header("Authorization", "Bearer" + authentication)
                .contentType(MediaType.MULTIPART_FORM_DATA)
                .body(body);
        } catch (RestClientException exception) {
            //log.warn("Images no respondió: {}", exception.getMessage());
        }
    }
}
