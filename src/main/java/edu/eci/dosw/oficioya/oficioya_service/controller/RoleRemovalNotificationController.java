package edu.eci.dosw.oficioya.oficioya_service.controller;

import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import edu.eci.dosw.oficioya.oficioya_service.dto.RoleRemovalNotificationRequest;
import edu.eci.dosw.oficioya.oficioya_service.service.RoleRemovalNotificationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

/**
 * RF-13. Users resuelve la petición de quita de rol y Service construye los
 * correos: uno al administrador cuando se solicita y uno al contratante o
 * trabajador cuando se aprueba o se rechaza.
 */
@RestController
@RequestMapping("/api/v1/role-removals")
@Tag(name = "Role removals", description = "Correos de peticiones para dejar de ser contratante o trabajador")
public class RoleRemovalNotificationController {

    private final RoleRemovalNotificationService roleRemovalNotificationService;

    public RoleRemovalNotificationController(RoleRemovalNotificationService roleRemovalNotificationService) {
        this.roleRemovalNotificationService = roleRemovalNotificationService;
    }

    @PostMapping("/notifications")
    @ResponseStatus(HttpStatus.ACCEPTED)
    @PreAuthorize("hasAnyRole('SERVICE', 'SISTEMA', 'ADMIN')")
    @Operation(summary = "Enviar correos de quita de rol",
            description = "Construye y envía el correo al administrador (REQUESTED) o al usuario (APPROVED/REJECTED)")
    public void notify(@Valid @RequestBody RoleRemovalNotificationRequest request) {
        roleRemovalNotificationService.notify(request);
    }
}
