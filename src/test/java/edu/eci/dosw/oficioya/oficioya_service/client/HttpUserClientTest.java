package edu.eci.dosw.oficioya.oficioya_service.client;

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

import java.lang.reflect.Field;
import java.util.UUID;

import edu.eci.dosw.oficioya.oficioya_service.dto.UserResponse;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

class HttpUserClientTest {

    @Test
    void getUserWithoutTokenReturnsNull() {
        HttpUserClient client = new HttpUserClient("http://localhost:8081", "");
        assertNull(client.getUser(UUID.randomUUID()));
    }

    @Test
    void getUserWithToken() throws Exception {
        HttpUserClient client = new HttpUserClient("http://localhost:8081", "token");
        RestTemplate mockRest = Mockito.mock(RestTemplate.class);
        Field f = HttpUserClient.class.getDeclaredField("restTemplate");
        f.setAccessible(true);
        f.set(client, mockRest);

        UserResponse resp = new UserResponse(UUID.randomUUID(), "name", "email", java.util.List.of(), java.util.List.of("CONTRATANTE"), "ACTIVE", null, null);
        when(mockRest.exchange(any(String.class), eq(HttpMethod.GET), any(HttpEntity.class), eq(UserResponse.class)))
                .thenReturn(ResponseEntity.ok(resp));

        assertEquals(resp, client.getUser(resp.id()));
    }
}