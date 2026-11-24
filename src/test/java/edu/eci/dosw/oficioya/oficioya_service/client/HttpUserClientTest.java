package edu.eci.dosw.oficioya.oficioya_service.client;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

import java.lang.reflect.Field;
import java.util.List;
import java.util.UUID;

import edu.eci.dosw.oficioya.oficioya_service.dto.UserResponse;
import edu.eci.dosw.oficioya.oficioya_service.security.JwtUtil;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class HttpUserClientTest {

    @Test
    void getUserWithoutTokenReturnsNull() {
        HttpUserClient client = new HttpUserClient("http://localhost:8081", "");
        assertNull(client.getUser(UUID.randomUUID()));
    }
/* 
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

    @Test
    void getUserFallaConRolServiceYUsaEndpointAdmin() throws Exception {
        HttpUserClient client = new HttpUserClient("http://localhost:8081", "token");
        RestTemplate mockRest = Mockito.mock(RestTemplate.class);
        Field f = HttpUserClient.class.getDeclaredField("restTemplate");
        f.setAccessible(true);
        f.set(client, mockRest);

        UserResponse resp = new UserResponse(UUID.randomUUID(), "name", "email", java.util.List.of(), java.util.List.of("CONTRATANTE"), "ACTIVE", null, null);
        when(mockRest.exchange(eq("http://localhost:8081/api/v1/users/" + resp.id() + "/account"),
                eq(HttpMethod.GET), any(HttpEntity.class), eq(UserResponse.class)))
                .thenThrow(new org.springframework.web.client.HttpClientErrorException(
                        org.springframework.http.HttpStatus.FORBIDDEN));
        when(mockRest.exchange(eq("http://localhost:8081/api/v1/admin/users/" + resp.id()),
                eq(HttpMethod.GET), any(HttpEntity.class), eq(UserResponse.class)))
                .thenReturn(ResponseEntity.ok(resp));

        assertEquals(resp, client.getUser(resp.id()));
        verify(mockRest).exchange(eq("http://localhost:8081/api/v1/admin/users/" + resp.id()),
                eq(HttpMethod.GET), any(HttpEntity.class), eq(UserResponse.class));
    }*/

    @Test
    void getUserWithGeneratedServiceToken() {
        JwtUtil jwtUtil = new JwtUtil("una-clave-de-prueba-de-al-menos-32-caracteres-123456", "oficioya", 30);
        RestTemplate mockRest = Mockito.mock(RestTemplate.class);
        HttpUserClient client = new HttpUserClient("http://localhost:8081/", "", jwtUtil, mockRest);

        UserResponse resp = new UserResponse(UUID.randomUUID(), "Juan", "juan@example.com", List.of(), List.of("CONTRATANTE"), "ACTIVE", null, null);
        when(mockRest.exchange(any(String.class), eq(HttpMethod.GET), any(HttpEntity.class), eq(UserResponse.class)))
                .thenReturn(ResponseEntity.ok(resp));

        assertEquals(resp, client.getUser(resp.id()));

        ArgumentCaptor<HttpEntity> entity = ArgumentCaptor.forClass(HttpEntity.class);
        verify(mockRest).exchange(eq("http://localhost:8081/api/v1/users/" + resp.id() + "/account"),
                eq(HttpMethod.GET), entity.capture(), eq(UserResponse.class));
        String auth = entity.getValue().getHeaders().getFirst(HttpHeaders.AUTHORIZATION);
        assertTrue(auth.startsWith("Bearer "));
        assertEquals(List.of("SERVICE", "SISTEMA", "ADMIN"),
                jwtUtil.extractRoles(auth.substring("Bearer ".length())));
    }
}
