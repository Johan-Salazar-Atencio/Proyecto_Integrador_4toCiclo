package com.plantia.dto;

import jakarta.validation.constraints.NotBlank;

public record CambiarRegionRequest(@NotBlank(message = "La region es obligatoria") String region) {
}
