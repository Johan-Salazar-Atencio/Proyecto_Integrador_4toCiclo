package com.plantia.services;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;

import com.plantia.dto.WeatherAlertDto;
import com.plantia.models.EventoDesastre;
import com.plantia.models.TipoDesastre;
import com.plantia.models.Usuario;
import com.plantia.repositories.EventoDesastreRepository;
import com.plantia.repositories.TipoDesastreRepository;
import com.plantia.repositories.UsuarioRepository;

/** La alerta del Inicio lleva la imagen, el texto y el consejo del tipo de evento. */
class EventoServiceTest {

    private EventoService servicio(EventoDesastre e, TipoDesastre tipo) {
        EventoDesastreRepository eventoRepo = mock(EventoDesastreRepository.class);
        UsuarioRepository usuarioRepo = mock(UsuarioRepository.class);
        Usuario u = new Usuario();
        u.setRegion("LIMA");
        when(usuarioRepo.findById(1)).thenReturn(Optional.of(u));
        when(eventoRepo.activosParaRegion("LIMA")).thenReturn(List.of(e));
        TipoDesastreRepository tipoRepo = mock(TipoDesastreRepository.class);
        when(tipoRepo.findById(3)).thenReturn(Optional.ofNullable(tipo));
        return new EventoService(eventoRepo, usuarioRepo, tipoRepo);
    }

    private static EventoDesastre evento() {
        EventoDesastre e = new EventoDesastre();
        e.setTipoEvento("OLA_DE_CALOR");
        e.setIdTipoDesastre(3);
        e.setSeveridad("MODERADO");
        e.setFechaInicio(LocalDateTime.now().minusHours(1));
        e.setFechaFin(LocalDateTime.now().plusHours(2));
        return e;
    }

    @Test
    void incluyeLosDatosDelTipoDeEvento() {
        TipoDesastre tipo = new TipoDesastre();
        tipo.setNombre("Ola de calor");
        tipo.setImagenUrl("/files/arte/eventos/ola_de_calor.jpg");
        tipo.setDescripcion("Texto informativo");
        tipo.setConsejo("Cubre el suelo");

        WeatherAlertDto a = servicio(evento(), tipo).activo(1).orElseThrow();

        assertThat(a.eventName()).isEqualTo("Ola de calor");
        assertThat(a.imageUrl()).isEqualTo("/files/arte/eventos/ola_de_calor.jpg");
        assertThat(a.info()).isEqualTo("Texto informativo");
        assertThat(a.tip()).isEqualTo("Cubre el suelo");
        assertThat(a.actionText()).isEqualTo("Prevenir evento");
        assertThat(a.endsInSeconds()).isBetween(2 * 3600L - 5, 2 * 3600L);
    }

    @Test
    void sinTipoRegistradoLosDatosExtraQuedanVacios() {
        EventoDesastre e = evento();
        e.setIdTipoDesastre(null);

        WeatherAlertDto a = servicio(e, null).activo(1).orElseThrow();

        assertThat(a.eventName()).isNull();
        assertThat(a.imageUrl()).isNull();
        assertThat(a.info()).isNull();
        assertThat(a.endsInSeconds()).isPositive();
    }
}
