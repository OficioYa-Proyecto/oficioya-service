package edu.eci.dosw.oficioya.oficioya_service.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1")
@Tag(name="Health", description = "API to check the health of the service")
public class HealthController {

    @GetMapping("/service/health")
    @Operation(summary="Check service health", description = "Returns OK if the service is running")
    public ResponseEntity<String> health() {
        return ResponseEntity.ok("OK");
    }
}