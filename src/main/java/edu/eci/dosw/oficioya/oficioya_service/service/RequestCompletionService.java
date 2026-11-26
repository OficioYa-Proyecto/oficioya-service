package edu.eci.dosw.oficioya.oficioya_service.service;

import java.util.List;
import java.time.LocalDateTime;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import edu.eci.dosw.oficioya.oficioya_service.client.HttpWorkerClient;
import edu.eci.dosw.oficioya.oficioya_service.client.NotificationService;
import edu.eci.dosw.oficioya.oficioya_service.client.ReputationClient;
import edu.eci.dosw.oficioya.oficioya_service.client.UserClient;
import edu.eci.dosw.oficioya.oficioya_service.dto.UserResponse;
import edu.eci.dosw.oficioya.oficioya_service.entity.Request;
import edu.eci.dosw.oficioya.oficioya_service.exception.RecursoNoEncontradoException;
import edu.eci.dosw.oficioya.oficioya_service.exception.RecursosAjenosException;
import edu.eci.dosw.oficioya.oficioya_service.mapper.RequestMapper;
import edu.eci.dosw.oficioya.oficioya_service.model.RequestModel;
import edu.eci.dosw.oficioya.oficioya_service.model.RequestState;
import edu.eci.dosw.oficioya.oficioya_service.repository.RequestRepository;

@Service
public class RequestCompletionService {

    private final RequestRepository requestRepository;
    private final RequestMapper requestMapper;
    private final NotificationService notificationService;
    private final ReputationClient reputationClient;
    private final UserClient userClient;
    private static final Logger log = LoggerFactory.getLogger(RequestCompletionService.class);

    public RequestCompletionService(RequestRepository requestRepository, RequestMapper requestMapper, NotificationService notificationService, ReputationClient reputationClient) {
        this(requestRepository, requestMapper, notificationService, reputationClient, null);
    }

    @Autowired
    public RequestCompletionService(RequestRepository requestRepository, RequestMapper requestMapper, NotificationService notificationService,
                                    ReputationClient reputationClient, UserClient userClient) {
        this.requestRepository = requestRepository;
        this.requestMapper = requestMapper;
        this.notificationService = notificationService;
        this.reputationClient = reputationClient;
        this.userClient = userClient;
    }

    public RequestModel completeRequest(UUID requestId, UUID workerId) {
        Request request = requestRepository.findById(requestId)
            .orElseThrow(() -> new RecursoNoEncontradoException(RecursoNoEncontradoException.SOLICITUD_NO_ENCONTRADA));

        if (!request.getWorkerId().equals(workerId)) {
            throw new RecursosAjenosException();
        }

        if (request.getState() != RequestState.ACCEPTED) {
            //TODO: Debería de cambiarse la exception para hacer handling
            throw new RuntimeException("Solo se pueden completar solicitudes en estado ACCEPTED");
        }

        if(request.getScheduledDateTime().isAfter(LocalDateTime.now())){
            //TODO: tirar una nueva exception
        }

        request.setState(RequestState.COMPLETED);
        // Notificar a ambas partes: el contratante califica al trabajador y el trabajador al contratante.
        notificationService.notifyCompleted(request.getContractorId(), requestId,
                emailOf(request.getContractorId()));
        notificationService.notifyCompleted(request.getWorkerId(), requestId,
                emailOf(request.getWorkerId()));
        if (reputationClient != null) {
            log.info("Se notifica completado servicio: {} con worker: {}, contractor :{}",requestId,request.getWorkerId(),request.getContractorId());
            reputationClient.notifyServiceCompleted(requestId, request.getWorkerId(), request.getContractorId(), LocalDateTime.now());
        }

        return requestMapper.toModel(requestRepository.save(request));
    }

    private String emailOf(UUID userId) {
        if (userClient == null || userId == null) {
            return null;
        }
        try {
            UserResponse user = userClient.getUser(userId);
            return user != null ? user.email() : null;
        } catch (Exception e) {
            return null;
        }
    }
}
