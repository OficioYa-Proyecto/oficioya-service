package edu.eci.dosw.oficioya.oficioya_service.service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import edu.eci.dosw.oficioya.oficioya_service.client.HttpWorkerClient;
import edu.eci.dosw.oficioya.oficioya_service.client.NotificationService;
import edu.eci.dosw.oficioya.oficioya_service.client.ReputationClient;
import edu.eci.dosw.oficioya.oficioya_service.client.UserClient;
import edu.eci.dosw.oficioya.oficioya_service.dto.UserResponse;
import edu.eci.dosw.oficioya.oficioya_service.entity.Request;
import edu.eci.dosw.oficioya.oficioya_service.exception.CredencialesInvalidasException;
import edu.eci.dosw.oficioya.oficioya_service.exception.RecursoNoEncontradoException;
import edu.eci.dosw.oficioya.oficioya_service.exception.RecursosAjenosException;
import edu.eci.dosw.oficioya.oficioya_service.mapper.RequestMapper;
import edu.eci.dosw.oficioya.oficioya_service.model.CancellationState;
import edu.eci.dosw.oficioya.oficioya_service.model.RequestModel;
import edu.eci.dosw.oficioya.oficioya_service.model.RequestState;
import edu.eci.dosw.oficioya.oficioya_service.repository.RequestRepository;

@Service
public class RequestCancellationService {

    private final RequestRepository requestRepository;

    private final RequestMapper requestMapper;

    private final NotificationService notificationService;

    private final UserClient userClient;

    private final ReputationClient reputationClient;

    private static final Logger log = LoggerFactory.getLogger(RequestCancellationService.class);

    public RequestCancellationService(RequestRepository requestRepository, RequestMapper requestMapper, NotificationService notificationService, UserClient userClient, ReputationClient reputationClient) {
        this.requestRepository = requestRepository;
        this.requestMapper = requestMapper;
        this.notificationService = notificationService;
        this.userClient = userClient;
        this.reputationClient = reputationClient;
    }

    public RequestModel cancelRequest(UUID requestId, UUID userId){
        log.info("User: {} cancelando request: {}",userId, requestId);
        if(requestRepository.findById(requestId).isEmpty()){
            log.error("Request no encontrada");
            throw new RecursoNoEncontradoException("NO_ENCONTRADO");
        }

        
        if (userClient != null) {
            UserResponse user = userClient.getUser(userId);
            if (user == null || user.roles() == null) {
                throw new RecursoNoEncontradoException("Usuario no válido o inactivo");
            }
        }

        Request found = requestRepository.findById(requestId).orElse(null);

        if(!userId.equals(found.getContractorId()) && !userId.equals(found.getWorkerId())){
            log.warn("Usuario intentó acceder a un recurso ajeno");
            throw new RecursosAjenosException();
        }

        if(userId.equals(found.getContractorId())){
            cancelByClient(requestId, userId);
        }
        else if(userId.equals(found.getWorkerId())){
            cancelByWorker(requestId, userId);
        }

        return requestMapper.toModel(requestRepository.findById(requestId).orElse(null));
    }


    public void cancelByClient(UUID requestId, UUID contractorId) {
        log.info("Request: {} Cancelada por contractor: {}", requestId,contractorId);

        Request request = requestRepository.findById(requestId).orElseThrow();
        if (!request.getContractorId().equals(contractorId)) {
            throw new RecursosAjenosException();
        }
        if (request.getState() == RequestState.COMPLETED) {
            //TODO: CAmbiar tipo de exception
            throw new RuntimeException("No se puede cancelar a solicitud ya completada");
        }
        if (request.getState() == RequestState.CANCELED || request.getState() == RequestState.DELETED) {
            //TODO: CAmbiar tipo de exception
            throw new RuntimeException("Esta solicitud ya se encuentra cancelada");
        }
        if (request.getState() == RequestState.ACCEPTED && request.getScheduledDateTime() != null) {
            LocalDateTime limit = request.getScheduledDateTime().minusHours(24);
            if (LocalDateTime.now().isAfter(limit)) {
                log.info("Contratante canceló con menos de 24 horas");
                request.setCancellationState(CancellationState.IN_PROGRESS);
            }
            else{
                request.setCancellationState(CancellationState.COMPLETED);
            }
        }

        request.setState(RequestState.CANCELED);
        if (request.getCancellationState() == null) {
            request.setCancellationState(CancellationState.COMPLETED);
        }
        request.setCancellationDateTime(LocalDateTime.now());
        requestRepository.save(request);
        // Notificar al trabajador que la solicitud fue cancelada (microservicio de notificaciones)
        notificationService.notifyCancelled(request.getWorkerId(), requestId,
                emailOf(request.getWorkerId()), "CANCELLED_BY_CONTRACTOR");

        //TODO: ELIMINAR FRANJA HORARIA DEL WORKER

    }

    public void cancelByWorker(UUID requestId, UUID workerId) {
        log.info("Request: {} Cancelada por worker: {}", requestId, workerId);

        Request request = requestRepository.findById(requestId).orElseThrow();
        if (!request.getWorkerId().equals(workerId)) {
            throw new RecursosAjenosException();
        }

        if (request.getState() == RequestState.COMPLETED) {
            log.warn("La solicitud ya estaba completada");
            //TODO: CAmbiar tipo de exception
            throw new RuntimeException("No se puede cancelar a solicitud ya completada");
        }
        if (request.getState() == RequestState.CANCELED || request.getState() == RequestState.DELETED) {
            //TODO: CAmbiar tipo de exception
            throw new RuntimeException("Esta solicitud ya se encuentra cancelada");
        }
        if (request.getState() == RequestState.ACCEPTED && request.getScheduledDateTime() != null) {
            LocalDateTime limit = request.getScheduledDateTime().minusHours(24);
            if (LocalDateTime.now().isAfter(limit)) {
                log.info("Trabajador canceló con menos de 24 horas");
                // Notificar a Reputation: cancelación tardía
                if (reputationClient != null) {
                    log.info("Notificando a reputation");
                    reputationClient.notifyLateCancellation(requestId, workerId, request.getScheduledDateTime(), LocalDateTime.now());
                }
            }
        }
        request.setState(RequestState.CANCELED);
        request.setCancellationState(CancellationState.COMPLETED);
        request.setCancellationDateTime(LocalDateTime.now());
        requestRepository.save(request);
        // Notificar al contratante que la solicitud fue cancelada (microservicio de notificaciones)
        notificationService.notifyCancelled(request.getContractorId(), requestId,
                emailOf(request.getContractorId()), "CANCELLED_BY_WORKER");

        //TODO: ELIMINAR FRANJA HORARIA DEL WORKER
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

    private void penalizarReputacion(UUID workerId) {
        if (reputationClient != null) {
            reputationClient.penalize(workerId, 0.5);
        } else {
            // TODO: invocar microservicio de reputacion para restar 0.5 puntos
            System.out.println("Penalizacion de reputacion -0.5 para trabajador: " + workerId);
        }
    }

}