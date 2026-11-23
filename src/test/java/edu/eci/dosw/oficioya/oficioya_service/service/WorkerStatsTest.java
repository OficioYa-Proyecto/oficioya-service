package edu.eci.dosw.oficioya.oficioya_service.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import edu.eci.dosw.oficioya.oficioya_service.entity.Request;
import edu.eci.dosw.oficioya.oficioya_service.model.RequestState;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import edu.eci.dosw.oficioya.oficioya_service.dto.WorkerStatsResponse;
import edu.eci.dosw.oficioya.oficioya_service.repository.RequestRepository;

@ExtendWith(MockitoExtension.class)
class WorkerStatsTest {

    @Mock
    private RequestRepository requestRepository;

    @Test
    void statsWithNoRequests() {
        UUID workerId = UUID.randomUUID();
        when(requestRepository.findAllRequestsForWorker(workerId)).thenReturn(List.of());

        RequestService service = new RequestService(requestRepository,
                org.mapstruct.factory.Mappers.getMapper(edu.eci.dosw.oficioya.oficioya_service.mapper.RequestMapper.class),
                new edu.eci.dosw.oficioya.oficioya_service.mapper.ResponseMapper(),
                new edu.eci.dosw.oficioya.oficioya_service.client.LoggingNotificationService(),
                null, null, null);

        WorkerStatsResponse stats = service.getWorkerStats(workerId);

        assertEquals(0, stats.completedJobs());
        assertEquals(0.0, stats.monthlyEarnings());
    }

    @Test
    void statsWithCompletedRequest() {
        UUID workerId = UUID.randomUUID();
        Request req = new Request();
        req.setId(UUID.randomUUID());
        req.setWorkerId(workerId);
        req.setContractorId(UUID.randomUUID());
        req.setState(RequestState.COMPLETED);
        req.setDescription("desc");
        req.setNeighborhood("zone");
        req.setExactAddress("addr");
        req.setScheduledDateTime(LocalDateTime.now());
        req.setDateCreated(LocalDateTime.now());
        when(requestRepository.findAllRequestsForWorker(workerId)).thenReturn(List.of(req));

        RequestService service = new RequestService(requestRepository,
                org.mapstruct.factory.Mappers.getMapper(edu.eci.dosw.oficioya.oficioya_service.mapper.RequestMapper.class),
                new edu.eci.dosw.oficioya.oficioya_service.mapper.ResponseMapper(),
                new edu.eci.dosw.oficioya.oficioya_service.client.LoggingNotificationService(),
                null, null, null);

        WorkerStatsResponse stats = service.getWorkerStats(workerId);

        assertEquals(1, stats.completedJobs());
    }
}