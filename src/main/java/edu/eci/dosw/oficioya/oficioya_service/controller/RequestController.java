package edu.eci.dosw.oficioya.oficioya_service.controller;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.net.URI;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.UUID;
import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.stream.ImageOutputStream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import edu.eci.dosw.oficioya.oficioya_service.client.RequestNotificationPhoto;
import edu.eci.dosw.oficioya.oficioya_service.dto.ActiveResponse;
import edu.eci.dosw.oficioya.oficioya_service.dto.RequestRequest;
import edu.eci.dosw.oficioya.oficioya_service.dto.RequestResponse;
import edu.eci.dosw.oficioya.oficioya_service.model.RequestModel;
import edu.eci.dosw.oficioya.oficioya_service.model.document.ImageDocument;
import edu.eci.dosw.oficioya.oficioya_service.security.CurrentUser;
import edu.eci.dosw.oficioya.oficioya_service.security.JwtUtil;
import edu.eci.dosw.oficioya.oficioya_service.security.JwtUtil.EmailAction;
import edu.eci.dosw.oficioya.oficioya_service.service.ImageService;
import edu.eci.dosw.oficioya.oficioya_service.service.RequestCancellationService;
import edu.eci.dosw.oficioya.oficioya_service.service.RequestCompletionService;
import edu.eci.dosw.oficioya.oficioya_service.service.RequestService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/v1/requests")
@Tag(name="Requests", description = "API to control the life of the requests")
public class RequestController {

    private static final Logger log = LoggerFactory.getLogger(RequestController.class);

    private final RequestService requestService;
    private final ImageService imageService;
    private final RequestCancellationService requestCancellationService;
    private final RequestCompletionService requestCompletionService;
    private final JwtUtil jwtUtil;
    private final String frontUrl;

    public RequestController(RequestService requestService, ImageService imageService,
                             RequestCancellationService requestCancellationService,
                             RequestCompletionService requestCompletionService,
                             JwtUtil jwtUtil,
                             @Value("${app.front-url:http://localhost:5173}") String frontUrl) {
        this.requestService = requestService;
        this.imageService = imageService;
        this.requestCancellationService = requestCancellationService;
        this.requestCompletionService = requestCompletionService;
        this.jwtUtil = jwtUtil;
        String base = frontUrl == null || frontUrl.isBlank() ? "http://localhost:5173" : frontUrl;
        this.frontUrl = base.endsWith("/") ? base.substring(0, base.length() - 1) : base;
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasRole('CONTRATANTE')")
    @Operation(summary="Create a request", description = "Creates a request and saves it with its images")
    public RequestResponse create(
        @Valid @RequestPart("data") RequestRequest req,
        @RequestPart(value = "images", required = false) List<MultipartFile> images,
        @RequestHeader("Authorization") String authHeader,
        Authentication authentication) {
        // El rootId se reserva antes de persistir para poder subir las imágenes
        // primero: así el correo del trabajador ya puede mostrar la foto real.
        UUID rootId = UUID.randomUUID();
        imageService.addImages(images, rootId, null, authHeader);

        String imageBase64 = null;
        String imageContentType = null;
        if (images != null && !images.isEmpty()) {
            MultipartFile first = images.get(0);
            if (first != null && !first.isEmpty()) {
                try {
                    imageBase64 = Base64.getEncoder().encodeToString(first.getBytes());
                    imageContentType = first.getContentType();
                } catch (Exception e) {
                    log.warn("No se pudo adjuntar la primera foto al correo: {}", e.getMessage());
                }
            }
        }

        List<RequestNotificationPhoto> photos = emailPhotos(images);
        if (photos.isEmpty()) {
            return requestService.create(req, CurrentUser.id(authentication), rootId, authHeader,
                    imageBase64, imageContentType);
        }
        return requestService.create(req, CurrentUser.id(authentication), rootId, authHeader,
                imageBase64, imageContentType, photos);
    }

    /**
     * Prepara hasta tres fotos pequeñas en JPEG para el carrusel del correo.
     * Recortar y reducir aquí evita enviar varios megabytes por notificación.
     */
    private List<RequestNotificationPhoto> emailPhotos(List<MultipartFile> images) {
        if (images == null || images.isEmpty()) {
            return List.of();
        }
        List<RequestNotificationPhoto> photos = new ArrayList<>();
        for (MultipartFile image : images) {
            if (photos.size() >= 3) {
                break;
            }
            if (image == null || image.isEmpty()) {
                continue;
            }
            try {
                BufferedImage source = ImageIO.read(image.getInputStream());
                if (source == null) {
                    continue;
                }
                BufferedImage small = new BufferedImage(720, 420, BufferedImage.TYPE_INT_RGB);
                Graphics2D graphics = small.createGraphics();
                graphics.setColor(Color.WHITE);
                graphics.fillRect(0, 0, small.getWidth(), small.getHeight());
                double scale = Math.max(720.0 / source.getWidth(), 420.0 / source.getHeight());
                int cropWidth = Math.max(1, (int) Math.round(720 / scale));
                int cropHeight = Math.max(1, (int) Math.round(420 / scale));
                int x = Math.max(0, (source.getWidth() - cropWidth) / 2);
                int y = Math.max(0, (source.getHeight() - cropHeight) / 2);
                graphics.drawImage(source, 0, 0, 720, 420, x, y, x + cropWidth, y + cropHeight, null);
                graphics.dispose();

                ByteArrayOutputStream output = new ByteArrayOutputStream();
                ImageWriter writer = ImageIO.getImageWritersByFormatName("jpg").next();
                ImageWriteParam params = writer.getDefaultWriteParam();
                params.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
                params.setCompressionQuality(0.72f);
                try (ImageOutputStream stream = ImageIO.createImageOutputStream(output)) {
                    writer.setOutput(stream);
                    writer.write(null, new IIOImage(small, null, null), params);
                } finally {
                    writer.dispose();
                }
                photos.add(new RequestNotificationPhoto(
                        Base64.getEncoder().encodeToString(output.toByteArray()), "image/jpeg"));
            } catch (Exception e) {
                log.warn("No se pudo preparar una foto para el correo: {}", e.getMessage());
            }
        }
        return photos;
    }

    /*
    @PreAuthorize("hasAnyRole('CONTRATANTE', 'TRABAJADOR')")
    @GetMapping
    public List<RequestModel> listMine(Authentication authentication) {
        return requestService.listRequestsForUser(CurrentUser.id(authentication));
    }
    */

    @PreAuthorize("hasRole('TRABAJADOR')")
    @GetMapping("/worker")
    @Operation(summary="List requests for a worker", description = "Lists all the requests of a worker, excluding the ones he made as a contractor and the eliminated ones")
    public List<RequestModel> listForWorker(Authentication authentication){
        return requestService.listRequestsForWorker(CurrentUser.id(authentication));
    }

    @PreAuthorize("hasRole('CONTRATANTE')")
    @GetMapping ("/contractor")
    @Operation(summary="List requests for a contractor", description = "Lists all the requests made as a contractor, if the user is a worker too, it only shows the ones that were not received and excludes too the eliminated ones")
    public List<RequestModel> listForContractor(Authentication authentication){
        return requestService.listRequestsForContractor(CurrentUser.id(authentication));
    }

    @PreAuthorize("hasRole('TRABAJADOR')")
    @PatchMapping("/{id}/accept")
    @Operation(summary="Accept a request", description = "Lets the worker accept a request")
    public RequestModel accept(@PathVariable UUID id, Authentication authentication) {
        return requestService.acceptRequest(id, CurrentUser.id(authentication));
    }

    @PreAuthorize("hasRole('TRABAJADOR')")
    @PatchMapping("/{id}/reject")
    @Operation(summary="Reject a request", description = "Lets the worker reject a request")
    public RequestModel reject(@PathVariable UUID id, Authentication authentication) {
        return requestService.rejectRequest(id, CurrentUser.id(authentication));
    }

    @PreAuthorize("hasRole('CONTRATANTE')")
    @PatchMapping("/{id}/accept-conflict")
    @Operation(summary="Resolve conflict", description = "Lets the contractor resolve a conflict between the conflicted requests")
    public RequestModel acceptConflict(@PathVariable UUID id, Authentication authentication) {
        return requestService.acceptConflict(id, CurrentUser.id(authentication));
    }

    @PatchMapping("/{id}/cancel")
    @PreAuthorize("hasAnyRole('CONTRATANTE', 'TRABAJADOR')")
    @Operation(summary="Cancel a request", description = "Lets a worker or a contractor cancel a request")
    public RequestModel cancelRequest(@PathVariable UUID id, Authentication authentication){
        return requestCancellationService.cancelRequest(id,CurrentUser.id(authentication));
    }

    @PatchMapping("/{id}/complete")
    @PreAuthorize("hasRole('TRABAJADOR')")
    @Operation(summary="Complete a request", description = "Lets the worker complete a request")
    public RequestModel completeRequest(@PathVariable UUID id, Authentication authentication){
        return requestCompletionService.completeRequest(id, CurrentUser.id(authentication));
    }
    
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary="Delete a request", description = "Lets only an admin delete a request")
    public ResponseEntity<String> deleteRequest(@PathVariable UUID id){
        if(requestService.deleteRequest(id)){
            return ResponseEntity.status(HttpStatus.OK).body("Request deleted");
        }
        else{
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Request not deleted");
        }
    }

    @GetMapping("/email/accept")
    @Operation(summary = "Accept a request from the email button",
            description = "Valida el enlace firmado, acepta la solicitud y redirige al front")
    public ResponseEntity<Void> acceptFromEmail(@RequestParam UUID requestId, @RequestParam String token) {
        return applyEmailAction(requestId, token, "ACCEPT", "accepted");
    }

    @GetMapping("/email/reject")
    @Operation(summary = "Reject a request from the email button",
            description = "Valida el enlace firmado, rechaza la solicitud y redirige al front")
    public ResponseEntity<Void> rejectFromEmail(@RequestParam UUID requestId, @RequestParam String token) {
        return applyEmailAction(requestId, token, "REJECT", "rejected");
    }

    private ResponseEntity<Void> applyEmailAction(UUID requestId, String token,
                                                   String expectedAction, String result) {
        try {
            EmailAction action = jwtUtil.validateEmailActionToken(token);
            if (!expectedAction.equals(action.action()) || !requestId.equals(action.requestId())) {
                return redirectAfterAction(requestId, "invalid");
            }
            if ("ACCEPT".equals(expectedAction)) {
                requestService.acceptRequest(requestId, action.workerId());
            } else {
                requestService.rejectRequest(requestId, action.workerId());
            }
            return redirectAfterAction(requestId, result);
        } catch (Exception e) {
            log.warn("No se pudo ejecutar la acción {} del correo para {}: {}",
                    expectedAction, requestId, e.getMessage());
            return redirectAfterAction(requestId, "invalid");
        }
    }

    private ResponseEntity<Void> redirectAfterAction(UUID requestId, String result) {
        String location = frontUrl + "/mis-servicios?request=" + requestId + "&result=" + result;
        return ResponseEntity.status(HttpStatus.FOUND).location(URI.create(location)).build();
    }

    @GetMapping("/active")
    @PreAuthorize("hasRole('SERVICE')")
    public ActiveResponse active(
            @RequestParam(required = true) UUID userId,
            @RequestParam(required = true) String role) {

        return requestService.active(userId, role);
    }
}