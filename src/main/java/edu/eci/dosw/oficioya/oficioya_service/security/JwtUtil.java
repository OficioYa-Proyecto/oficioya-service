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
    /** Emisor exclusivo de los enlaces de acción enviados por correo. */
    public static final String EMAIL_ACTION_ISSUER = "oficioya-email-action";
    private static final long SERVICE_TOKEN_MILLIS = 5 * 60 * 1000L;
    private static final long EMAIL_ACTION_TTL_DAYS = 7;

    /** Acción firmada que viene en el enlace del correo del trabajador. */
    public record EmailAction(UUID workerId, UUID requestId, String action) { }

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

    /**
     * Token de un solo uso lógico para los botones Aceptar/Rechazar del correo.
     * Usa un emisor propio para que un token de login no pueda activar la acción.
     */
    public String generateEmailActionToken(UUID workerId, UUID requestId, String action) {
        java.time.Instant ahora = java.time.Instant.now();
        return Jwts.builder()
                .subject(workerId.toString())
                .issuer(EMAIL_ACTION_ISSUER)
                .claim("purpose", "request-action")
                .claim("requestId", requestId.toString())
                .claim("action", action)
                .issuedAt(Date.from(ahora))
                .expiration(Date.from(ahora.plus(EMAIL_ACTION_TTL_DAYS, java.time.temporal.ChronoUnit.DAYS)))
                .signWith(key, Jwts.SIG.HS256)
                .compact();
    }

    public EmailAction validateEmailActionToken(String token) {
        Claims claims = validateToken(token);
        if (!EMAIL_ACTION_ISSUER.equals(claims.getIssuer())
                || !"request-action".equals(claims.get("purpose", String.class))) {
            throw new IllegalArgumentException("El token no corresponde a una acción de correo");
        }
        return new EmailAction(
                UUID.fromString(claims.getSubject()),
                UUID.fromString(claims.get("requestId", String.class)),
                claims.get("action", String.class));
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