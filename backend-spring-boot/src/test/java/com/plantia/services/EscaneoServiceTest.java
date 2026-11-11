package com.plantia.services;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.SimpleTransactionStatus;
import org.springframework.web.client.RestClient;
import org.springframework.web.multipart.MultipartFile;

import com.plantia.dto.IaResultDto;
import com.plantia.repositories.EscaneoHistorialRepository;
import com.plantia.repositories.PlantaCatalogoRepository;

/** La IA (lenta) se llama antes de abrir la transaccion, y un fallo no deja fotos huerfanas. */
class EscaneoServiceTest {

    private final MockMultipartFile foto = new MockMultipartFile("foto", "a.jpg", "image/jpeg", new byte[] {1});

    // StorageService e IaClient son clases concretas: Mockito no puede simularlas en Java 26,
    // asi que se usan dobles escritos a mano que registran lo que pasa.
    private final List<String> eventos = new ArrayList<>();
    private StorageService storage;
    private IaClient ia;
    private PlatformTransactionManager tx;
    private EscaneoHistorialRepository escaneoRepo;
    private EscaneoService service;

    @BeforeEach
    void preparar() {
        storage = new StorageService() {
            @Override public String save(MultipartFile f) { return "http://x/files/scan.jpg"; }
            @Override public void delete(String url) { eventos.add("borrar " + url); }
        };
        ia = new IaClient(RestClient.builder(), "http://ia", "auto", 1) {
            @Override public IaResultDto identificar(MultipartFile f) {
                eventos.add("ia");
                return new IaResultDto("Desconocido", "Desconocido", 0.0, List.of(), "{}");
            }
        };
        tx = mock(PlatformTransactionManager.class);
        when(tx.getTransaction(any())).thenAnswer(i -> {
            eventos.add("transaccion");
            return new SimpleTransactionStatus();
        });
        escaneoRepo = mock(EscaneoHistorialRepository.class);
        service = new EscaneoService(escaneoRepo, mock(PlantaCatalogoRepository.class), storage, ia, null, tx);
    }

    @Test
    void laIaSeLlamaAntesDeAbrirLaTransaccion() {
        when(escaneoRepo.save(any())).thenAnswer(i -> i.getArgument(0));
        service.escanear(1, foto);
        assertThat(eventos).containsExactly("ia", "transaccion");
    }

    @Test
    void siFallaElGuardadoSeBorraLaFoto() {
        when(escaneoRepo.save(any())).thenThrow(new IllegalStateException("BD caida"));
        assertThatThrownBy(() -> service.escanear(1, foto)).isInstanceOf(IllegalStateException.class);
        assertThat(eventos).contains("borrar http://x/files/scan.jpg");
    }
}
