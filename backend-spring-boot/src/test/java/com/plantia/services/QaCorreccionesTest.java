package com.plantia.services;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.server.ResponseStatusException;

import com.plantia.dto.IaResultDto;
import com.plantia.repositories.EscaneoHistorialRepository;
import com.plantia.repositories.PlantaCatalogoRepository;
import com.plantia.util.AparienciaUtil;

/** Pruebas unitarias de las correcciones del QA del 22/11 que no necesitan base de datos. */
class QaCorreccionesTest {

    // ---------------------------------------------------------------- QA-12

    @Test
    void qa12LaAparienciaSoloGuardaCamposConocidosYCortos() {
        Map<String, Object> cruda = new LinkedHashMap<>();
        cruda.put("forma", "suculenta");
        cruda.put("colorHoja", "#2f7d4a");
        cruda.put("tieneFlor", true);
        cruda.put("relleno", "x".repeat(5_000_000));
        cruda.put("colorFlor", "x".repeat(41));
        cruda.put("tieneFruto", "si");

        Map<String, Object> limpia = AparienciaUtil.limpiar(cruda);

        assertThat(limpia).containsOnlyKeys("forma", "colorHoja", "tieneFlor");
        assertThat(AparienciaUtil.aJson(cruda).length()).isLessThan(100);
        assertThat(AparienciaUtil.limpiar(Map.of("basura", 1))).isNull();
        assertThat(AparienciaUtil.limpiar(null)).isNull();
    }

    // ---------------------------------------------------------------- QA-15

    @Test
    void qa15SpringNoMandaProviderAutoNiVacio() {
        assertThat(IaClient.debeEnviarProvider("auto")).isFalse();
        assertThat(IaClient.debeEnviarProvider(" AUTO ")).isFalse();
        assertThat(IaClient.debeEnviarProvider("")).isFalse();
        assertThat(IaClient.debeEnviarProvider(null)).isFalse();
        assertThat(IaClient.debeEnviarProvider("gemini")).isTrue();
    }

    // ---------------------------------------------------------------- QA-08

    private EscaneoService escaneoCon(IaClient ia, StorageService storage) {
        PlatformTransactionManager tx = mock(PlatformTransactionManager.class);
        return new EscaneoService(mock(EscaneoHistorialRepository.class), mock(PlantaCatalogoRepository.class), storage, ia,
                null, null, tx);
    }

    @Test
    void qa08UnUsuarioNoPasaDeSuTopePorMinutoYLosDemasSiguen() {
        EscaneoService s = escaneoCon(mock(IaClient.class), mock(StorageService.class));
        s.maxPorMinuto = 3;
        for (int i = 0; i < 3; i++) {
            s.limitarPorUsuario(1, 1_000 + i);
        }
        assertThatThrownBy(() -> s.limitarPorUsuario(1, 2_000))
                .isInstanceOfSatisfying(ResponseStatusException.class,
                        e -> assertThat(e.getStatusCode()).isEqualTo(HttpStatus.TOO_MANY_REQUESTS));
        s.limitarPorUsuario(2, 2_000); // otro usuario no se ve afectado
        s.limitarPorUsuario(1, 61_500); // pasado el minuto vuelve a poder
    }

    @Test
    void qa08UnaIaSaturadaDa429YBorraLaFotoEnVezDePlantaDesconocida() {
        IaClient ia = mock(IaClient.class);
        when(ia.identificar(any())).thenThrow(HttpClientErrorException.create(HttpStatus.TOO_MANY_REQUESTS, "x", null, null, null));
        StorageService storage = mock(StorageService.class);
        when(storage.save(any())).thenReturn("/files/foto.png");
        EscaneoService s = escaneoCon(ia, storage);

        assertThatThrownBy(() -> s.escanear(1, new MockMultipartFile("foto", "f.png", "image/png", new byte[] {1})))
                .isInstanceOfSatisfying(ResponseStatusException.class,
                        e -> assertThat(e.getStatusCode()).isEqualTo(HttpStatus.TOO_MANY_REQUESTS));
        verify(storage).delete("/files/foto.png");
    }

    // ---------------------------------------------------------------- QA-01 / QA-13

    @Test
    void qa13UnaEspecieQueYaExisteNoSeReescribeNiPierdeSusCuidados() {
        PlantaCatalogoRepository catalogoRepo = mock(PlantaCatalogoRepository.class);
        com.plantia.repositories.CuidadoSeccionRepository cuidadoRepo = mock(com.plantia.repositories.CuidadoSeccionRepository.class);
        com.plantia.models.PlantaCatalogo existente = new com.plantia.models.PlantaCatalogo();
        existente.setIdCatalogo(4);
        existente.setNombreComun("Nombre del admin");
        when(catalogoRepo.findFirstByNombreCientificoIgnoreCaseOrderByIdCatalogoAsc("Rosa qaensis")).thenReturn(Optional.of(existente));
        when(cuidadoRepo.countByIdCatalogo(4)).thenReturn(3L);
        CatalogoService catalogo = new CatalogoService(catalogoRepo, cuidadoRepo);

        IaResultDto ia = new IaResultDto("Nueva1", " Rosa qaensis ", 90.0,
                List.of(new IaResultDto.IaCuidadoDto("RIEGO", "Riego", "Semanal")), "{}");
        assertThat(catalogo.upsertDesdeIa(ia)).isSameAs(existente);

        assertThat(existente.getNombreComun()).isEqualTo("Nombre del admin");
        verify(catalogoRepo).bloquearEspecie("rosa qaensis");
        verify(catalogoRepo, never()).save(any());
        verify(cuidadoRepo, never()).deleteByIdCatalogo(any());
        verify(cuidadoRepo, never()).save(any());
    }

    // ---------------------------------------------------------------- QA-35 y QA-39

    @Test
    void qa35LosPremiosSeRecortanYLaSumaNoDesborda() {
        assertThat(ProgresoService.premio(Integer.MAX_VALUE)).isEqualTo(ProgresoService.MAX_PREMIO);
        assertThat(ProgresoService.premio(-5)).isZero();
        assertThat(ProgresoService.premio(null)).isZero();
        assertThat(ProgresoService.saturar(Integer.MAX_VALUE - 10, 100_000)).isEqualTo(Integer.MAX_VALUE);
        assertThat(ProgresoService.saturar(5, 7)).isEqualTo(12);
    }

    @Test
    void qa39UnaFotoInvalidaNoGastaElTopeYUnEscaneoFallidoDevuelveSuCupo() {
        StorageService storage = mock(StorageService.class);
        when(storage.save(any())).thenThrow(new ResponseStatusException(HttpStatus.UNSUPPORTED_MEDIA_TYPE, "no es imagen"));
        EscaneoService invalidas = escaneoCon(mock(IaClient.class), storage);
        invalidas.maxPorMinuto = 2;
        for (int i = 0; i < 5; i++) {
            assertThatThrownBy(() -> invalidas.escanear(1, new MockMultipartFile("foto", "x.txt", "text/plain", new byte[] {1})))
                    .isInstanceOfSatisfying(ResponseStatusException.class,
                            e -> assertThat(e.getStatusCode()).isEqualTo(HttpStatus.UNSUPPORTED_MEDIA_TYPE));
        }
        invalidas.limitarPorUsuario(1, System.currentTimeMillis()); // el cupo sigue entero
        invalidas.limitarPorUsuario(1, System.currentTimeMillis());

        IaClient ia = mock(IaClient.class);
        when(ia.identificar(any())).thenThrow(HttpClientErrorException.create(HttpStatus.TOO_MANY_REQUESTS, "x", null, null, null));
        StorageService ok = mock(StorageService.class);
        when(ok.save(any())).thenReturn("/files/f.png");
        EscaneoService saturada = escaneoCon(ia, ok);
        saturada.maxPorMinuto = 2;
        for (int i = 0; i < 4; i++) {
            assertThatThrownBy(() -> saturada.escanear(1, new MockMultipartFile("foto", "f.png", "image/png", new byte[] {1})))
                    .isInstanceOf(ResponseStatusException.class);
        }
        saturada.limitarPorUsuario(1, System.currentTimeMillis()); // los fallidos devolvieron su cupo
        saturada.limitarPorUsuario(1, System.currentTimeMillis());
    }
}
