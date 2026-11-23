package edu.eci.dosw.oficioya.oficioya_service.dto;

import java.time.LocalDateTime;
import java.util.UUID;

public record ImageResponse(
        UUID id,
        UUID rootId,
        //ImageType tipo,
        String nombre,
        String tipoContenido,
        Long tamano,
        LocalDateTime fechaCarga,
        String referenciaExterna,
        String url,
        UUID subidoPor
) {}