package com.plantia.controllers;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;

import com.plantia.dto.LogroDesbloqueadoDto;
import com.plantia.dto.LogrosVistosRequest;
import com.plantia.services.LogroService;

class LogroControllerTest {

    @Test
    void pendientesDevuelveLista() {
        LogroService service = mock(LogroService.class);
        when(service.pendientes(eq(1), org.mockito.ArgumentMatchers.any(LocalDateTime.class)))
                .thenReturn(List.of(new LogroDesbloqueadoDto(1, "C", "T", "D", "i", "COMUN", 10, 5)));
        LogroController controller = new LogroController(service);

        List<LogroDesbloqueadoDto> res = controller.pendientes(1);

        assertThat(res).hasSize(1);
        assertThat(res.get(0).codigo()).isEqualTo("C");
    }

    @Test
    void vistosMarcaYResponde204() {
        LogroService service = mock(LogroService.class);
        LogroController controller = new LogroController(service);

        ResponseEntity<Void> res = controller.vistos(7, new LogrosVistosRequest(List.of(3, 4)));

        assertThat(res.getStatusCode().value()).isEqualTo(204);
        verify(service).marcarVistos(7, List.of(3, 4));
    }
}
