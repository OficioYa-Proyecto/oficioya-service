package edu.eci.dosw.oficioya.oficioya_service.dto;

import jakarta.validation.constraints.NotBlank;

public record ActiveResponse(
        @NotBlank Long active) {
}
