package edu.eci.dosw.oficioya.oficioya_service.service;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import edu.eci.dosw.oficioya.oficioya_service.client.NotificationService;
import edu.eci.dosw.oficioya.oficioya_service.dto.RoleRemovalNotificationRequest;

class RoleRemovalNotificationServiceTest {

    private NotificationService notificationService;
    private RoleRemovalNotificationService service;

    @BeforeEach
    void preparar() {
        notificationService = mock(NotificationService.class);
        service = new RoleRemovalNotificationService(notificationService);
    }

    @Test
    @DisplayName("REQUESTED envía el cuerpo al administrador con el tipo en el asunto")
    void requestedAlAdministrador() {
        UUID userId = UUID.randomUUID();
        UUID requestId = UUID.randomUUID();
        service.notify(new RoleRemovalNotificationRequest("REQUESTED", requestId, userId, "TRABAJADOR",
                "Juan", "Ya no quiero atender solicitudes", null, null, "admin@oficioya.com"));

        ArgumentCaptor<String> type = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> subject = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> body = ArgumentCaptor.forClass(String.class);
        verify(notificationService).notifyRoleRemovalEmail(type.capture(), org.mockito.ArgumentMatchers.eq(userId),
                org.mockito.ArgumentMatchers.eq("admin@oficioya.com"), org.mockito.ArgumentMatchers.eq(requestId),
                subject.capture(), body.capture());

        assertTrue(type.getValue().equals("ROLE_REMOVAL_REQUESTED"));
        assertTrue(subject.getValue().startsWith("ROLE_REMOVAL_REQUESTED - "));
        assertTrue(body.getValue().contains("TRABAJADOR"));
        assertTrue(body.getValue().contains("Ya no quiero atender solicitudes"));
    }

    @Test
    @DisplayName("APPROVED y REJECTED envían el cuerpo al contratante o trabajador")
    void resueltaAlUsuario() {
        UUID userId = UUID.randomUUID();
        service.notify(new RoleRemovalNotificationRequest("APPROVED", UUID.randomUUID(), userId, "CONTRATANTE",
                "Maria", "Motivo del usuario", "Todo en orden", "maria@example.com", null));
        service.notify(new RoleRemovalNotificationRequest("REJECTED", UUID.randomUUID(), userId, "TRABAJADOR",
                "Maria", "Motivo del usuario", "Tiene solicitudes activas", "maria@example.com", null));

        ArgumentCaptor<String> subjects = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> bodies = ArgumentCaptor.forClass(String.class);
        verify(notificationService, org.mockito.Mockito.times(2)).notifyRoleRemovalEmail(
                org.mockito.ArgumentMatchers.anyString(), org.mockito.ArgumentMatchers.eq(userId),
                org.mockito.ArgumentMatchers.eq("maria@example.com"), org.mockito.ArgumentMatchers.any(),
                subjects.capture(), bodies.capture());

        assertTrue(subjects.getAllValues().get(0).startsWith("ROLE_REMOVAL_APPROVED - "));
        assertTrue(bodies.getAllValues().get(0).contains("aprobada"));
        assertTrue(subjects.getAllValues().get(1).startsWith("ROLE_REMOVAL_REJECTED - "));
        assertTrue(bodies.getAllValues().get(1).contains("rechazada"));
        assertTrue(bodies.getAllValues().get(1).contains("Tiene solicitudes activas"));
    }
}
