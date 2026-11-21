package com.plantia.dto;

import java.util.List;

public record LogrosResponseDto(
        int total,
        int desbloqueados,
        List<LogroDto> logros
) {
}
