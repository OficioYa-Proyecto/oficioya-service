package edu.eci.dosw.oficioya.oficioya_service.dto;

import java.util.UUID;

import jakarta.validation.constraints.NotBlank;

/**
 * Datos que Users envía para que Service construya y envíe los correos de RF-13.
 * action: REQUESTED, APPROVED o REJECTED.
 */
public record RoleRemovalNotificationRequest(
        @NotBlank String action,
        UUID roleRemovalRequestId,
        UUID userId,
        @NotBlank String role,
        String userName,
        String reason,
        String adminReason,
        String userEmail,
        String adminEmail
) {
}
