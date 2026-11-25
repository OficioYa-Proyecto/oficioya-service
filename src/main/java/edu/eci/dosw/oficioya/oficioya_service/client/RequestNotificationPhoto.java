package edu.eci.dosw.oficioya.oficioya_service.client;

/**
 * Foto reducida que viaja a Notification para incrustarse en el correo.
 * Se mantienen hasta tres para el carrusel; el contenido ya viene en JPEG.
 */
public record RequestNotificationPhoto(String base64, String contentType) {
}
