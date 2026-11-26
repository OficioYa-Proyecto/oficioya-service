package edu.eci.dosw.oficioya.oficioya_service.dto;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import org.springframework.web.multipart.MultipartFile;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record RequestRequest (
    @NotEmpty List<UUID> workersId,
    @NotBlank String description,
    @NotNull Long localityId,
    @NotBlank @Size(max = 120) String locality,
    @NotBlank @Size(max = 120) String neighborhood,
    @NotBlank String exactAddress,
    @NotNull LocalDateTime scheduledDateTime,
    List<MultipartFile> images
    ){
    
}