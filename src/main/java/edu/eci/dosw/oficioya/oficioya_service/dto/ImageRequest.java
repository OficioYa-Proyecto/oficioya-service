package edu.eci.dosw.oficioya.oficioya_service.dto;

/**
 * Imagen adjunta a una solicitud. Los bytes viajan en Base64 dentro del JSON
 * y Service los reenvía como multipart al microservicio de imágenes.
 */
public record ImageRequest(
    String nombre,
    String tipoContenido,
    byte[] datos,
    String referenciaExterna
) {
}
