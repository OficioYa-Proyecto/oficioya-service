package edu.eci.dosw.oficioya.oficioya_service.security;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.UUID;

import javax.crypto.SecretKey;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

@Component
public class JwtUtil {

    /** Subject del token interno compartido con el orquestador y Users. */
    public static final String SERVICE_SUBJECT = "00000000-0000-0000-0000-000000000001";
    private static final long SERVICE_TOKEN_MILLIS = 5 * 60 * 1000L;

    private final SecretKey key;
    private final String issuer;
    private final long ttlMinutes;

    public JwtUtil(@Value("${app.jwt.secret}") String secret,
                   @Value("${app.jwt.issuer}") String issuer,
                   @Value("${app.jwt.ttl-minutes}") long ttlMinutes) {
        this.key = Keys.hmacShaKeyFor(decodificarSecreto(secret));
        this.issuer = issuer;
        this.ttlMinutes = ttlMinutes;
    }

    private static byte[] decodificarSecreto(String secret) {
        try {
            byte[] decodificado = java.util.Base64.getDecoder().decode(secret);
            if (decodificado.length >= 32) {
                return decodificado;
            }
        } catch (IllegalArgumentException e) {
            // No es Base64: se usa el texto tal cual.
        }
        return secret.getBytes(StandardCharsets.UTF_8);
    }

    public String generateToken(UUID userId, String rol) {
        java.time.Instant ahora = java.time.Instant.now();
        return Jwts.builder()
                .subject(userId.toString())
                .claim("role", rol)
                .issuer(issuer)
                .issuedAt(Date.from(ahora))
                .expiration(Date.from(ahora.plus(ttlMinutes, java.time.temporal.ChronoUnit.MINUTES)))
                .signWith(key, Jwts.SIG.HS256)
                .compact();
    }

    /**
     * Token interno de vida corta para llamar a Users y obtener el correo del
     * contratante. Usa el mismo secreto y los mismos roles que el orquestador.
     */
    public String generateServiceToken() {
        java.time.Instant ahora = java.time.Instant.now();
        return Jwts.builder()
                .subject(SERVICE_SUBJECT)
                .claim("roles", List.of("SERVICE", "SISTEMA", "ADMIN"))
                .issuedAt(Date.from(ahora))
                .expiration(Date.from(ahora.plusMillis(SERVICE_TOKEN_MILLIS)))
                .signWith(key, Jwts.SIG.HS256)
                .compact();
    }

    public Claims validateToken(String token) {
        return Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    public UUID extractUserId(String token) {
        return UUID.fromString(validateToken(token).getSubject());
    }

    public String extractUsername(String token) {
        return validateToken(token).getSubject();
    }

    public List<String> extractRoles(String token) {
        Claims claims = validateToken(token);
        List<String> roles = new ArrayList<>();

        Object plural = claims.get("roles");
        if (plural instanceof Collection<?> lista) {
            for (Object r : lista) {
                if (r != null) {
                    roles.add(r.toString());
                }
            }
        } else if (plural instanceof String unico) {
            roles.add(unico);
        }

        if (roles.isEmpty()) {
            String singular = claims.get("role", String.class);
            if (singular != null) {
                roles.add(singular);
            }
        }
        return roles;
    }

    public String extractRole(String token) {
        List<String> roles = extractRoles(token);
        return roles.isEmpty() ? null : roles.get(0);
    }

    public boolean isTokenValid(String token) {
        try {
            validateToken(token);
            return true;
        } catch (Exception e) {
            return false;
        }
    }
}