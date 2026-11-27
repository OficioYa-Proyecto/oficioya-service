package edu.eci.dosw.oficioya.oficioya_service.service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.Set;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import edu.eci.dosw.oficioya.oficioya_service.client.ImageClient;
import edu.eci.dosw.oficioya.oficioya_service.client.NotificationService;
import edu.eci.dosw.oficioya.oficioya_service.client.RequestNotificationDetails;
import edu.eci.dosw.oficioya.oficioya_service.client.RequestNotificationPhoto;
import edu.eci.dosw.oficioya.oficioya_service.client.ReputationClient;
import edu.eci.dosw.oficioya.oficioya_service.client.UserClient;
import edu.eci.dosw.oficioya.oficioya_service.client.WorkerClient;
import edu.eci.dosw.oficioya.oficioya_service.dto.ActiveResponse;
import edu.eci.dosw.oficioya.oficioya_service.dto.ImageResponse;
import edu.eci.dosw.oficioya.oficioya_service.dto.RequestRequest;
import edu.eci.dosw.oficioya.oficioya_service.dto.RequestResponse;
import edu.eci.dosw.oficioya.oficioya_service.dto.ReportItem;
import edu.eci.dosw.oficioya.oficioya_service.dto.ServiceReportResponse;
import edu.eci.dosw.oficioya.oficioya_service.dto.UserResponse;
import edu.eci.dosw.oficioya.oficioya_service.dto.WorkerStatsResponse;
import edu.eci.dosw.oficioya.oficioya_service.entity.Request;
import edu.eci.dosw.oficioya.oficioya_service.exception.RecursoNoEncontradoException;
import edu.eci.dosw.oficioya.oficioya_service.exception.RequestSinWorkersException;
import edu.eci.dosw.oficioya.oficioya_service.exception.RolNoPermitidoException;
import edu.eci.dosw.oficioya.oficioya_service.exception.TransicionInvalidaException;
import edu.eci.dosw.oficioya.oficioya_service.mapper.RequestMapper;
import edu.eci.dosw.oficioya.oficioya_service.mapper.ResponseMapper;
import edu.eci.dosw.oficioya.oficioya_service.model.CancellationState;
import edu.eci.dosw.oficioya.oficioya_service.model.RequestModel;
import edu.eci.dosw.oficioya.oficioya_service.model.RequestState;
import edu.eci.dosw.oficioya.oficioya_service.repository.RequestRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Service
public class RequestService {

    private static final long MINUTOS_PARA_RESPONDER = 30;
    private static final Set<RequestState> CANCELABLES_EN_CONFLICTO =
            EnumSet.of(RequestState.PENDING, RequestState.ACCEPTED, RequestState.CONFLICT);

    private final RequestRepository requestRepository;
    private final RequestMapper requestMapper;
    private final ResponseMapper responseMapper;
    private final NotificationService notificationService;
    private final UserClient userClient;
    private final WorkerClient workerClient;
    private final ReputationClient reputationClient;
    private final ImageClient imageClient;
    private static final Logger log = LoggerFactory.getLogger(RequestService.class);

    public RequestService(RequestRepository requestRepository, RequestMapper requestMapper, ResponseMapper responseMapper,
                          NotificationService notificationService, UserClient userClient, WorkerClient workerClient,
                          ReputationClient reputationClient) {
        this(requestRepository, requestMapper, responseMapper, notificationService, userClient, workerClient,
                reputationClient, null);
    }

    @Autowired
    public RequestService(RequestRepository requestRepository, RequestMapper requestMapper, ResponseMapper responseMapper,
                          NotificationService notificationService, UserClient userClient, WorkerClient workerClient,
                          ReputationClient reputationClient, ImageClient imageClient) {
        this.requestRepository = requestRepository;
        this.requestMapper = requestMapper;
        this.responseMapper = responseMapper;
        this.notificationService = notificationService;
        this.userClient = userClient;
        this.workerClient = workerClient;
        this.reputationClient = reputationClient;
        this.imageClient = imageClient;
    }

    public RequestResponse create(RequestRequest req, UUID contractorId, String authHeader){
        return create(req, contractorId, UUID.randomUUID(), authHeader, null, null, List.of());
    }

    public RequestResponse create(RequestRequest req, UUID contractorId, UUID rootId, String authHeader){
        return create(req, contractorId, rootId, authHeader, null, null, List.of());
    }

    public RequestResponse create(RequestRequest req, UUID contractorId, UUID rootId, String authHeader,
                                  String imageBase64, String imageContentType){
        return create(req, contractorId, rootId, authHeader, imageBase64, imageContentType, List.of());
    }

    public RequestResponse create(RequestRequest req, UUID contractorId, UUID rootId, String authHeader,
                                  String imageBase64, String imageContentType,
                                  List<RequestNotificationPhoto> photos){
        log.info("Creando una nueva request como usuario: {}", contractorId);
        List<UUID> workers = req.workersId();

        if(workers.size() <= 0){
            throw new RequestSinWorkersException();
        }

        String contractorEmail = null;
        if (userClient != null) {
            UserResponse contractor = userClient.getUser(contractorId);
            if (contractor != null) {
                contractorEmail = contractor.email();
            }
            else{
                log.warn("No se pudo encontrar el contratante {}", contractorId);
                throw new RecursoNoEncontradoException("Contratante no válido o inactivo");
            }
        }

        if(contractorEmail == null){
            log.warn("No se pudo obtener el correo del contratante {}", contractorId);
            throw new RecursoNoEncontradoException("Contratante no válido o inactivo");
        }

        List<UUID> verifiedWorkers = new ArrayList<>();
        UserResponse worker = null;

        for (UUID id : workers){
            if(contractorId.equals(id)){
                log.warn("EL usuario se trató de contratar a sí mismo");
                continue;
            }

            worker = userClient.getUser(id);

            if (worker == null || worker.roles() == null || !worker.roles().contains("TRABAJADOR")) {
                log.info("Usuario no encontrado, no tiene roles, o no era trabajador");
                continue;
            }

            // Users es la fuente de verdad del rol. Worker puede responder 404 cuando el
            // perfil está pausado o no existe; en ese caso se registran los dos resultados
            // pero no se descarta al trabajador, para no bloquear la creación de la solicitud.
            if (workerClient != null && !workerClient.exists(id, authHeader)) {
                log.warn("El worker {} no está disponible en Worker; se validó el rol en Users", id);
            }

            Optional<Request> contractorCancelledWorker = requestRepository.findContractorHasCancelledWorker(contractorId, id);

            if(!contractorCancelledWorker.isEmpty()){
                LocalDateTime cancellationDate = contractorCancelledWorker.orElse(null).getCancellationDateTime();
                if(LocalDateTime.now().isAfter(cancellationDate.plusWeeks(1))){
                    contractorCancelledWorker.orElse(null).setCancellationState(CancellationState.COMPLETED);
                }
                else{
                    continue;
                }
            }

            //TODO: AGREGAR FRANJA HORARIA DEL WORKER
            //Si está libre se agrega
            if(true){
                verifiedWorkers.add(id);
                if (workerClient != null) {
                    workerClient.reserveSlot(id, req.scheduledDateTime());
                }
            }
        }

        if (verifiedWorkers.size() <= 0){
            throw new RecursoNoEncontradoException(RecursoNoEncontradoException.TRABAJADOR_NO_ENCONTRADO);
        }
        
        UUID newRootId = rootId != null ? rootId : UUID.randomUUID();

        if (rootId == null) {
            while (requestRepository.findByRootId(newRootId).size() >= 1){
                newRootId = UUID.randomUUID();
            }
        }

        List<Request> createdRequests = responseMapper.toEntities(contractorId, verifiedWorkers, req, newRootId);
        List<Request> savedRequests = requestRepository.saveAll(createdRequests);

        // Las imágenes se suben antes de crear las solicitudes, usando el mismo rootId.
        RequestNotificationDetails details = new RequestNotificationDetails(
                req.scheduledDateTime(), req.neighborhood(), req.exactAddress(), req.description(),
                firstImageUrl(newRootId, authHeader), imageBase64, imageContentType,
                photos == null ? List.of() : photos);

        // Se notifica después de persistir para que los requestId ya estén asignados.
        savedRequests.forEach(w -> notificationService.notifyNewRequest(
                w.getWorkerId(), w.getId(), emailOf(w.getWorkerId()), details));
        // Una sola confirmación para el contratante, asociada al grupo de solicitudes (rootId).
        notificationService.notifyRequestCreated(contractorId, newRootId, contractorEmail, details);

        return responseMapper.toRespone(savedRequests);

    }

    private String firstImageUrl(UUID rootId, String authHeader) {
        if (imageClient == null) {
            return null;
        }
        try {
            List<ImageResponse> images = imageClient.getImages(rootId, authHeader);
            if (images == null || images.isEmpty()) {
                return null;
            }
            return images.get(0).url();
        } catch (Exception e) {
            log.warn("No se pudo obtener la imagen de la solicitud {}: {}", rootId, e.getMessage());
            return null;
        }
    }

    /** Contexto que el correo de aceptación, disputa o cancelación reutiliza del correo de entrada. */
    private RequestNotificationDetails detailsOf(Request request) {
        return new RequestNotificationDetails(
                request.getScheduledDateTime(), request.getNeighborhood(), request.getExactAddress(),
                request.getDescription(), firstImageUrl(request.getRootId(), null));
    }

    private String emailOf(UUID userId) {
        if (userClient == null) {
            return null;
        }
        try {
            UserResponse user = userClient.getUser(userId);
            return user != null ? user.email() : null;
        } catch (Exception e) {
            log.warn("No se pudo obtener el correo del trabajador {}: {}", userId, e.getMessage());
            return null;
        }
    }


    /** Todas las solicitudes (no eliminadas) donde el usuario es contratante o trabajador. */
    @Transactional(readOnly = true)
    public List<RequestModel> listRequestsForUser(UUID userId) {
        return requestRepository.findAllRequestsForUser(userId).stream()
                .map(requestMapper::toModel)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<RequestModel> listRequestsForWorker(UUID workerId) {
        return requestRepository.findAllRequestsForWorker(workerId).stream()
                .map(requestMapper::toModel)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<RequestModel> listRequestsForContractor(UUID contractorId) {
        return requestRepository.findAllRequestsForContractor(contractorId).stream()
                .map(requestMapper::toModel)
                .toList();
    }

    /** Estadísticas del trabajador para otros dominios (completedJobs, monthlyEarnings, topServices). */
    @Transactional(readOnly = true)
    public WorkerStatsResponse getWorkerStats(UUID workerId) {
        long completed = requestRepository.findAllRequestsForWorker(workerId).stream()
                .filter(r -> r.getState() == RequestState.COMPLETED)
                .count();
        return new WorkerStatsResponse(workerId, completed, 0.0, List.of());
    }

    /** Reporte de solicitudes por período: por estado, franja horaria y barrio. */
    @Transactional(readOnly = true)
    public ServiceReportResponse getReport(LocalDateTime from, LocalDateTime to) {
        List<Request> requests = requestRepository.findAll().stream()
                .filter(r -> r.getScheduledDateTime() != null
                        && !r.getScheduledDateTime().isBefore(from)
                        && !r.getScheduledDateTime().isAfter(to))
                .toList();

        Map<String, Long> byStatus = requests.stream()
                .collect(Collectors.groupingBy(r -> r.getState().name(), Collectors.counting()));

        Map<String, Long> byNeighborhood = requests.stream()
                .collect(Collectors.groupingBy(Request::getNeighborhood, Collectors.counting()));

        Map<Integer, Long> byHour = requests.stream()
                .collect(Collectors.groupingBy(r -> r.getScheduledDateTime().getHour(), Collectors.counting()));

        // Servicios cumplidos por día (solo COMPLETED)
        List<ReportItem> completedByDay = requests.stream()
                .filter(r -> r.getState() == RequestState.COMPLETED)
                .collect(Collectors.groupingBy(
                        r -> r.getScheduledDateTime().toLocalDate().toString(),
                        Collectors.counting()))
                .entrySet().stream()
                .map(e -> new ReportItem(e.getKey(), e.getValue()))
                .sorted((a, b) -> a.etiqueta().compareTo(b.etiqueta()))
                .toList();

        // Servicios cumplidos por oficio: requiere campo "trade/oficio" en Request (pendiente de agregar)
        List<ReportItem> completedByTrade = List.of();
        // Solicitudes por oficio (todas): requiere campo "trade/oficio" en Request (pendiente)
        List<ReportItem> requestsByTrade = List.of();

        return new ServiceReportResponse(requests.size(), byStatus, byNeighborhood, byHour,
                completedByDay, completedByTrade, requestsByTrade);
    }

    /**
     * El trabajador asignado acepta una solicitud PENDING dentro de los 30 minutos.
     * Si otra solicitud del mismo rootId ya estaba aceptada (o en conflicto), la solicitud
     * y esas otras pasan a CONFLICT para que el contratante elija una (acceptConflict).
     */
    @Transactional
    public RequestModel acceptRequest(UUID requestId, UUID workerId) {
        Request assigned = findAssignedToWorker(requestId, workerId);
        // Bloquea el grupo completo antes de decidir: dos aceptaciones simultáneas
        // se serializan y la segunda ve el estado que dejó la primera.
        List<Request> group = requestRepository.findByRootIdForUpdate(assigned.getRootId());
        Request request = group.stream()
                .filter(r -> r.getId().equals(requestId))
                .findFirst()
                .orElseThrow(() -> new RecursoNoEncontradoException(
                        RecursoNoEncontradoException.SOLICITUD_NO_ENCONTRADA));
        requireAnswerable(request);

        // Predisposición: si la publicación fue enviada a más de un trabajador y todavía hay
        // candidatos vivos (PENDING/ACCEPTED/CONFLICT), la primera aceptación pone la solicitud
        // en disputa (CONFLICT) para que el contratante elija, sin esperar una segunda aceptación.
        // Los PENDING se dejan intactos: así cada trabajador puede seguir aceptando o rechazando
        // y el grupo se resuelve solo cuando quede un único candidato. Si todos los demás ya
        // rechazaron/cancelaron (o solo había un trabajador), queda ACCEPTED como siempre.
        boolean multiWorker = group.size() > 1;

        List<Request> otherAccepted = group.stream()
                .filter(r -> !r.getId().equals(request.getId()))
                .filter(r -> r.getState() == RequestState.ACCEPTED)
                .toList();

        List<Request> otherPending = group.stream()
                .filter(r -> !r.getId().equals(request.getId()))
                .filter(r -> r.getState() == RequestState.PENDING)
                .toList();

        List<Request> otherConflict = group.stream()
                .filter(r -> !r.getId().equals(request.getId()))
                .filter(r -> r.getState() == RequestState.CONFLICT)
                .toList();

        boolean otherCandidatesAlive = !otherAccepted.isEmpty()
                || !otherPending.isEmpty() || !otherConflict.isEmpty();

        if (!multiWorker || !otherCandidatesAlive) {
            request.setState(RequestState.ACCEPTED);
            notificationService.notifyAccepted(request.getContractorId(), requestId,
                    emailOf(request.getContractorId()), detailsOf(request));
        } else {
            request.setState(RequestState.CONFLICT);
            // Solo las aceptadas previas se alinean en CONFLICT; las PENDING se respetan.
            otherAccepted.forEach(r -> r.setState(RequestState.CONFLICT));
            if (!otherAccepted.isEmpty()) {
                requestRepository.saveAll(otherAccepted);
            }
            notificationService.notifyConflict(request.getContractorId(), request.getId(),
                    request.getRootId(), emailOf(request.getContractorId()), detailsOf(request));
        }
        return requestMapper.toModel(requestRepository.save(request));
    }

    /**
     * El trabajador asignado rechaza una solicitud PENDING dentro de los 30 minutos.
     * En CONFLICT también se puede rechazar: ese candidato sale del grupo y, si solo
     * queda uno en CONFLICT, la disputa se resuelve sola como ACCEPTED.
     */
    @Transactional
    public RequestModel rejectRequest(UUID requestId, UUID workerId) {
        Request request = findAssignedToWorker(requestId, workerId);
        if (request.getState() != RequestState.PENDING && request.getState() != RequestState.CONFLICT) {
            throw new TransicionInvalidaException("Solo se pueden rechazar solicitudes PENDING o CONFLICT");
        }
        boolean wasConflict = request.getState() == RequestState.CONFLICT;
        request.setState(RequestState.REJECTED);
        notificationService.notifyRejected(request.getContractorId(), requestId,
                emailOf(request.getContractorId()), detailsOf(request));
        //TODO: ELIMINAR FRANJA HORARIA DEL WORKER
        RequestModel result = requestMapper.toModel(requestRepository.save(request));
        if (wasConflict && request.getRootId() != null) {
            resolveSingleConflict(request.getRootId());
        }
        return result;
    }

    /** Si tras un rechazo/cancelación solo queda un CONFLICT en el grupo, se acepta automáticamente. */
    private void resolveSingleConflict(UUID rootId) {
        List<Request> remaining = requestRepository.findByRootId(rootId).stream()
                .filter(r -> r.getState() == RequestState.CONFLICT)
                .toList();
        if (remaining.size() == 1) {
            Request sole = remaining.get(0);
            sole.setState(RequestState.ACCEPTED);
            requestRepository.save(sole);
            notificationService.notifyAccepted(sole.getWorkerId(), sole.getId(),
                    emailOf(sole.getWorkerId()), detailsOf(sole));
        }
    }

    /**
     * El contratante resuelve un conflicto eligiendo una solicitud (en estado CONFLICT):
     * esa pasa a ACCEPTED y las demás del mismo rootId se cancelan sin penalización.
     */
    @Transactional
    public RequestModel acceptConflict(UUID requestId, UUID contractorId) {
        Request owned = requestRepository.findById(requestId)
                .filter(r -> r.getContractorId().equals(contractorId))
                .orElseThrow(() -> new RecursoNoEncontradoException(RecursoNoEncontradoException.SOLICITUD_NO_ENCONTRADA));
        // El mismo bloqueo por rootId evita que dos resolver simultáneos elijan trabajadores distintos.
        List<Request> group = requestRepository.findByRootIdForUpdate(owned.getRootId());
        Request chosen = group.stream()
                .filter(r -> r.getId().equals(requestId))
                .findFirst()
                .orElseThrow(() -> new RecursoNoEncontradoException(
                        RecursoNoEncontradoException.SOLICITUD_NO_ENCONTRADA));
        if (chosen.getState() != RequestState.CONFLICT) {
            throw new TransicionInvalidaException("Solo se puede resolver un conflicto sobre una solicitud en estado CONFLICT");
        }

        LocalDateTime now = LocalDateTime.now();
        List<Request> others = group.stream()
                .filter(r -> !r.getId().equals(chosen.getId()))
                .filter(r -> CANCELABLES_EN_CONFLICTO.contains(r.getState()))
                .toList();
        for (Request other : others) {
            other.setState(RequestState.CANCELED);
            other.setCancellationState(CancellationState.COMPLETED);
            other.setCancellationDateTime(now);
        }
        requestRepository.saveAll(others);

        chosen.setState(RequestState.ACCEPTED);

        // Cada solicitud descartada pertenece a otro trabajador del mismo contratante.
        others.forEach(r -> notificationService.notifyCancelled(r.getWorkerId(), r.getId(),
                emailOf(r.getWorkerId()), "CANCELLED_BY_CONTRACTOR", detailsOf(r)));
        notificationService.notifyAccepted(chosen.getWorkerId(), chosen.getId(),
                emailOf(chosen.getWorkerId()), detailsOf(chosen));

        //TODO: ELIMINAR FRANJA HORARIA DEL WORKER
        return requestMapper.toModel(requestRepository.save(chosen));
    }

    public boolean deleteRequest(UUID requestId){
        Optional<Request> found = requestRepository.findById(requestId);

        if(found.isEmpty()){
            throw new RecursoNoEncontradoException(RecursoNoEncontradoException.SOLICITUD_NO_ENCONTRADA);
        }

        int changed = requestRepository.specialDeleteById(requestId);

        if(changed == 1){
            return true;
        }
        else{
            return false;
        }
    }

    public boolean existsById(UUID requestId){
        Optional<Request> found = requestRepository.findById(requestId);
        if(!found.isEmpty() && found.orElse(null).getState() != RequestState.DELETED){
            return true;
        }
        else{
            return false;
        }
    }

    public Optional<Request> getRequest(UUID requestId){
        return requestRepository.findById(requestId);
    }

    public Optional<UUID> getRootId(UUID requestId){
        Optional<Request> found = requestRepository.findById(requestId);
        if(!found.isEmpty()){
            return Optional.of(found.orElse(null).getRootId());
        }
        else{
            return Optional.empty();
        }
    }

    // Una solicitud ajena responde igual que una inexistente (404) para no revelar que existe.
    private Request findAssignedToWorker(UUID requestId, UUID workerId) {
        return requestRepository.findById(requestId)
                .filter(r -> r.getWorkerId().equals(workerId))
                .orElseThrow(() -> new RecursoNoEncontradoException(RecursoNoEncontradoException.SOLICITUD_NO_ENCONTRADA));
    }

    private void requireAnswerable(Request request) {
        if (request.getState() != RequestState.PENDING) {
            throw new TransicionInvalidaException("Solo se pueden responder solicitudes en estado PENDING");
        }
        /*
        if (request.getDateCreated() != null
                && request.getDateCreated().plusMinutes(MINUTOS_PARA_RESPONDER).isBefore(LocalDateTime.now())) {
            throw new TransicionInvalidaException("Tiempo de respuesta excedido (30 minutos)");
        }*/
    }

    public ActiveResponse active (UUID userId, String rol){
        int count = 0;
        if (userClient != null){
            UserResponse user = userClient.getUser(userId);

            if(user == null){
                throw new RecursoNoEncontradoException("Usuario no encontrado o no disponible");
            }
        }

        if ("CONTRATANTE".equals(rol)){
            count = requestRepository.countActiveContractor(userId);
        }
        else if ("TRABAJADOR".equals(rol)){
            count = requestRepository.countActiveWorker(userId);
        }
        else{
            throw new RolNoPermitidoException("Rol no permitido");
        }

        // Users usa este valor para impedir quitar un rol con solicitudes activas;
        // debe ser el conteo real, no un indicador 1/2.
        return new ActiveResponse(Long.valueOf(count));
    }
}
