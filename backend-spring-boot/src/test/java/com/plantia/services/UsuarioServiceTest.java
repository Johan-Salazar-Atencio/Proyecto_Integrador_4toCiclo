package com.plantia.services;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.web.server.ResponseStatusException;

import com.plantia.dto.RegionDto;
import com.plantia.dto.UserSummaryDto;
import com.plantia.models.Nivel;
import com.plantia.models.Region;
import com.plantia.models.Usuario;
import com.plantia.repositories.NivelRepository;
import com.plantia.repositories.RegionRepository;
import com.plantia.repositories.UsuarioRepository;

/** El perfil trae los limites del nivel actual para dibujar la barra de XP. */
class UsuarioServiceTest {

    private NivelRepository nivelRepo;
    private UsuarioRepository usuarioRepo;
    private RegionRepository regionRepo;
    private com.plantia.repositories.ItemTiendaRepository itemRepo;
    private com.plantia.repositories.InventarioUsuarioRepository inventarioRepo;
    private UsuarioService service;
    private StorageService storage;
    private final List<String> borradas = new java.util.ArrayList<>();

    @BeforeEach
    void preparar() {
        nivelRepo = mock(NivelRepository.class);
        usuarioRepo = mock(UsuarioRepository.class);
        when(nivelRepo.findById(2)).thenReturn(Optional.of(nivel(2, 500)));
        when(nivelRepo.findById(3)).thenReturn(Optional.of(nivel(3, 1000)));

        regionRepo = mock(RegionRepository.class);
        itemRepo = mock(com.plantia.repositories.ItemTiendaRepository.class);
        inventarioRepo = mock(com.plantia.repositories.InventarioUsuarioRepository.class);
        storage = new StorageService() {
            @Override public String save(org.springframework.web.multipart.MultipartFile f) {
                if (f.isEmpty()) {
                    throw new ResponseStatusException(org.springframework.http.HttpStatus.BAD_REQUEST, "vacio");
                }
                return "http://x/files/nueva.jpg";
            }
            @Override public void delete(String url) { borradas.add(url); }
        };
        service = new UsuarioService(usuarioRepo, nivelRepo, regionRepo, itemRepo, inventarioRepo, storage);
    }

    private static Nivel nivel(int n, int xp) {
        Nivel nv = new Nivel();
        nv.setNivel(n);
        nv.setXpRequerido(xp);
        return nv;
    }

    private void usuario(int nivel, int xp) {
        Usuario u = new Usuario();
        u.setIdUsuario(1);
        u.setNombre("Johan");
        u.setNivel(nivel);
        u.setExperienciaXp(xp);
        u.setRachaDias(3);
        when(usuarioRepo.findById(1)).thenReturn(Optional.of(u));
    }

    @Test
    void enMitadDeNivelDaLosDosLimites() {
        usuario(2, 620);

        UserSummaryDto dto = service.me(1);

        assertThat(dto.experienceFromLevel()).isEqualTo(500);
        assertThat(dto.experienceToNextLevel()).isEqualTo(1000);
        assertThat(dto.experience()).isEqualTo(620);
        assertThat(dto.streakDays()).isEqualTo(3);
    }

    @Test
    void enElUltimoNivelLosDosLimitesSeIgualanAlXpYLaBarraQuedaCompleta() {
        when(nivelRepo.findById(4)).thenReturn(Optional.empty());
        when(nivelRepo.findById(3)).thenReturn(Optional.of(nivel(3, 1000)));
        usuario(3, 1250);

        UserSummaryDto dto = service.me(1);

        assertThat(dto.experienceToNextLevel()).isEqualTo(1250);
        assertThat(dto.experienceFromLevel()).isEqualTo(1000);
    }

    @Test
    void conUnNivelQueNoEstaEnLaTablaLaBaseEsCero() {
        usuario(1, 120);

        UserSummaryDto dto = service.me(1);

        assertThat(dto.experienceFromLevel()).isZero();
    }

    private static Region region(int id, String codigo, String nombre) {
        Region r = new Region();
        r.setIdRegion(id);
        r.setCodigo(codigo);
        r.setNombre(nombre);
        return r;
    }

    @Test
    void sinRegionGuardadaElResumenDiceGlobal() {
        usuario(1, 0);

        assertThat(service.me(1).regionCode()).isEqualTo("GLOBAL");
    }

    @Test
    void cambiarRegionGuardaElCodigoYLaLlave() {
        usuario(1, 0);
        when(regionRepo.findByCodigoAndActivaTrue("AREQUIPA")).thenReturn(Optional.of(region(4, "AREQUIPA", "Arequipa")));

        UserSummaryDto dto = service.cambiarRegion(1, "AREQUIPA");

        ArgumentCaptor<Usuario> guardado = ArgumentCaptor.forClass(Usuario.class);
        verify(usuarioRepo).save(guardado.capture());
        assertThat(guardado.getValue().getRegion()).isEqualTo("AREQUIPA");
        assertThat(guardado.getValue().getIdRegion()).isEqualTo(4);
        assertThat(dto.regionCode()).isEqualTo("AREQUIPA");
    }

    @Test
    void unaRegionInexistenteOInactivaDa400YNoGuarda() {
        usuario(1, 0);
        when(regionRepo.findByCodigoAndActivaTrue("NOEXISTE")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.cambiarRegion(1, "NOEXISTE"))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("Region no valida");
        verify(usuarioRepo, never()).save(any());
    }

    @Test
    void cambiarRegionDeUnUsuarioInexistenteDa404() {
        when(usuarioRepo.findById(9)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.cambiarRegion(9, "LIMA")).isInstanceOf(ResponseStatusException.class);
    }

    @Test
    void cambiarNombreGuardaElNombreSinEspaciosSobrantes() {
        usuario(1, 0);

        UserSummaryDto dto = service.cambiarNombre(1, "  Johan Salazar  ");

        ArgumentCaptor<Usuario> guardado = ArgumentCaptor.forClass(Usuario.class);
        verify(usuarioRepo).save(guardado.capture());
        assertThat(guardado.getValue().getNombre()).isEqualTo("Johan Salazar");
        assertThat(dto.name()).isEqualTo("Johan Salazar");
    }

    @Test
    void unNombreMuyCortoDa400YNoGuarda() {
        usuario(1, 0);

        assertThatThrownBy(() -> service.cambiarNombre(1, " J "))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("entre 2 y 100");
        verify(usuarioRepo, never()).save(any());
    }

    @Test
    void cambiarNombreDeUnUsuarioInexistenteDa404() {
        when(usuarioRepo.findById(9)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.cambiarNombre(9, "Ana")).isInstanceOf(ResponseStatusException.class);
    }

    @Test
    void lasRegionesSalenConCodigoYNombre() {
        when(regionRepo.findByActivaTrueOrderByNombreAsc())
                .thenReturn(List.of(region(1, "GLOBAL", "Global"), region(3, "LIMA", "Lima Metropolitana")));

        assertThat(service.regiones()).extracting(RegionDto::code).containsExactly("GLOBAL", "LIMA");
        assertThat(service.regiones().get(1).name()).isEqualTo("Lima Metropolitana");
    }

    // --- Invernadero de la coleccion ---

    private static com.plantia.models.ItemTienda invernadero(int id, int precio, String codigo, boolean activo) {
        com.plantia.models.ItemTienda i = new com.plantia.models.ItemTienda();
        i.setIdItem(id);
        i.setTipo("INVERNADERO");
        i.setPrecioMonedas(precio);
        i.setCodigo(codigo);
        i.setActivo(activo);
        return i;
    }

    private Usuario usuarioSimple() {
        Usuario u = new Usuario();
        u.setIdUsuario(1);
        u.setNombre("Johan");
        when(usuarioRepo.findById(1)).thenReturn(Optional.of(u));
        when(inventarioRepo.findByIdUsuario(1)).thenReturn(List.of());
        return u;
    }

    @Test
    void sinEleccionElInvernaderoEsLaSelvaTropical() {
        usuarioSimple();

        assertThat(service.me(1).greenhouseCode()).isEqualTo("SELVA");
    }

    @Test
    void sePuedeElegirUnInvernaderoGratisOComprado() {
        Usuario u = usuarioSimple();
        when(itemRepo.findById(30)).thenReturn(Optional.of(invernadero(30, 0, "SELVA", true)));
        when(itemRepo.findById(31)).thenReturn(Optional.of(invernadero(31, 150, "VICTORIANO", true)));
        com.plantia.models.InventarioUsuario compra = new com.plantia.models.InventarioUsuario();
        compra.setIdItem(31);
        when(inventarioRepo.findByIdUsuario(1)).thenReturn(List.of(compra));

        assertThat(service.cambiarInvernadero(1, 30).greenhouseCode()).isEqualTo("SELVA");
        assertThat(service.cambiarInvernadero(1, 31).greenhouseCode()).isEqualTo("VICTORIANO");
        assertThat(u.getIdInvernadero()).isEqualTo(31);
    }

    @Test
    void unInvernaderoQueNoSeCompro409YUnoQueNoExisteOEstaApagado404() {
        Usuario u = usuarioSimple();
        when(itemRepo.findById(32)).thenReturn(Optional.of(invernadero(32, 200, "LUCES", true)));
        when(itemRepo.findById(33)).thenReturn(Optional.of(invernadero(33, 0, "X", false)));

        assertThatThrownBy(() -> service.cambiarInvernadero(1, 32))
                .isInstanceOfSatisfying(ResponseStatusException.class, e -> assertThat(e.getStatusCode().value()).isEqualTo(409));
        assertThatThrownBy(() -> service.cambiarInvernadero(1, 33))
                .isInstanceOfSatisfying(ResponseStatusException.class, e -> assertThat(e.getStatusCode().value()).isEqualTo(404));
        assertThatThrownBy(() -> service.cambiarInvernadero(1, 99))
                .isInstanceOfSatisfying(ResponseStatusException.class, e -> assertThat(e.getStatusCode().value()).isEqualTo(404));
        assertThat(u.getIdInvernadero()).isNull();
    }

    // --- Foto de perfil ---

    @Test
    void laFotoNuevaReemplazaALaAnteriorYBorraLaVieja() {
        Usuario u = usuarioSimple();
        u.setFotoUrl("http://x/files/vieja.jpg");
        var foto = new org.springframework.mock.web.MockMultipartFile("foto", "yo.jpg", "image/jpeg", new byte[] {1});

        UserSummaryDto dto = service.cambiarFoto(1, foto);

        assertThat(dto.profileImageUrl()).isEqualTo("http://x/files/nueva.jpg");
        assertThat(u.getFotoUrl()).isEqualTo("http://x/files/nueva.jpg");
        assertThat(borradas).containsExactly("http://x/files/vieja.jpg");
    }

    @Test
    void unaFotoVaciaSeRechazaYNoTocaLaAnterior() {
        Usuario u = usuarioSimple();
        u.setFotoUrl("http://x/files/vieja.jpg");
        var vacia = new org.springframework.mock.web.MockMultipartFile("foto", "yo.jpg", "image/jpeg", new byte[0]);

        assertThatThrownBy(() -> service.cambiarFoto(1, vacia)).isInstanceOf(ResponseStatusException.class);

        assertThat(u.getFotoUrl()).isEqualTo("http://x/files/vieja.jpg");
        assertThat(borradas).isEmpty();
    }
}
