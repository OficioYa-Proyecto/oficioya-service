package edu.eci.dosw.oficioya.oficioya_service.dto;

import java.time.LocalDateTime;
import java.util.UUID;

/** Metadatos devueltos por el microservicio de imágenes (sin bytes binarios). */
public record ImageResponse(
    UUID id,
    UUID rootId,
    String tipo,
    String nombre,
    String tipoContenido,
    Long tamano,
    LocalDateTime fechaCarga,
    String referenciaExterna,
    String url,
    UUID subidoPor
) {
}
