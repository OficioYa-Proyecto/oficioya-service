package edu.eci.dosw.oficioya.oficioya_service.security;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.List;
import java.util.UUID;

import javax.crypto.SecretKey;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

class JwtUtilTest {

    private static final String SECRET = "una-clave-de-prueba-de-al-menos-32-caracteres-123456";
    private static final String ISSUER = "oficioya";

    private final JwtUtil jwtUtil = new JwtUtil(SECRET, ISSUER, 30);
    private final SecretKey key = Keys.hmacShaKeyFor(SECRET.getBytes(StandardCharsets.UTF_8));

    private String tokenConClaims(java.util.function.Consumer<io.jsonwebtoken.JwtBuilder> extra) {
        io.jsonwebtoken.JwtBuilder builder = Jwts.builder()
                .subject(UUID.randomUUID().toString())
                .issuer(ISSUER)
                .expiration(new Date(System.currentTimeMillis() + 60_000));
        extra.accept(builder);
        return builder.signWith(key, Jwts.SIG.HS256).compact();
    }

    @Test
    @DisplayName("Lee los roles cuando vienen como lista en el claim roles")
    void leeRolesEnPlural() {
        String token = tokenConClaims(b -> b.claim("roles", List.of("CONTRATANTE", "TRABAJADOR")));

        assertEquals(List.of("CONTRATANTE", "TRABAJADOR"), jwtUtil.extractRoles(token));
        assertEquals("CONTRATANTE", jwtUtil.extractRole(token));
    }

    @Test
    @DisplayName("Sigue leyendo el claim role en singular (formato actual del servicio)")
    void leeRolEnSingular() {
        String token = jwtUtil.generateToken(UUID.randomUUID(), "CONTRATANTE");

        assertEquals(List.of("CONTRATANTE"), jwtUtil.extractRoles(token));
        assertEquals("CONTRATANTE", jwtUtil.extractRole(token));
    }

    @Test
    @DisplayName("Si vienen roles y role, gana roles")
    void rolesTienePrioridad() {
        String token = tokenConClaims(b -> b.claim("roles", List.of("ADMIN")).claim("role", "CONTRATANTE"));

        assertEquals(List.of("ADMIN"), jwtUtil.extractRoles(token));
    }

    @Test
    @DisplayName("Genera token de servicio con subject y roles compartidos con el orquestador")
    void generaTokenDeServicio() {
        String token = jwtUtil.generateServiceToken();
        Claims claims = jwtUtil.validateToken(token);

        assertEquals(JwtUtil.SERVICE_SUBJECT, claims.getSubject());
        assertEquals(List.of("SERVICE", "SISTEMA", "ADMIN"), claims.get("roles", List.class));
        assertTrue(jwtUtil.isTokenValid(token));
    }

    @Test
    @DisplayName("Sin roles ni role devuelve lista vacía y rol nulo")
    void sinRoles() {
        String token = tokenConClaims(b -> { });

        assertTrue(jwtUtil.extractRoles(token).isEmpty());
        assertNull(jwtUtil.extractRole(token));
    }
}