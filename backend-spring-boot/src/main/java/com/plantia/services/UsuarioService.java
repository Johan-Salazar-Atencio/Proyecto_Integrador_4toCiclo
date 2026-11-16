package com.plantia.services;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.plantia.dto.RegionDto;
import com.plantia.dto.UserSummaryDto;
import com.plantia.models.InventarioUsuario;
import com.plantia.models.ItemTienda;
import com.plantia.models.Region;
import org.springframework.web.multipart.MultipartFile;
import com.plantia.models.Usuario;
import com.plantia.repositories.InventarioUsuarioRepository;
import com.plantia.repositories.ItemTiendaRepository;
import com.plantia.repositories.NivelRepository;
import com.plantia.repositories.RegionRepository;
import com.plantia.repositories.UsuarioRepository;

@Service
public class UsuarioService {

    private final UsuarioRepository usuarioRepo;
    private final NivelRepository nivelRepo;
    private final RegionRepository regionRepo;
    private final ItemTiendaRepository itemRepo;
    private final InventarioUsuarioRepository inventarioRepo;
    private final StorageService storage;

    public UsuarioService(UsuarioRepository usuarioRepo, NivelRepository nivelRepo, RegionRepository regionRepo,
                          ItemTiendaRepository itemRepo, InventarioUsuarioRepository inventarioRepo,
                          StorageService storage) {
        this.usuarioRepo = usuarioRepo;
        this.nivelRepo = nivelRepo;
        this.regionRepo = regionRepo;
        this.itemRepo = itemRepo;
        this.inventarioRepo = inventarioRepo;
        this.storage = storage;
    }

    @Transactional(readOnly = true)
    public UserSummaryDto me(Integer userId) {
        Usuario u = usuarioRepo.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Usuario no encontrado"));
        return resumen(u);
    }

    /** Regiones que el usuario puede elegir en su perfil. */
    @Transactional(readOnly = true)
    public List<RegionDto> regiones() {
        return regionRepo.findByActivaTrueOrderByNombreAsc().stream()
                .map(r -> new RegionDto(r.getCodigo(), r.getNombre()))
                .toList();
    }

    /**
     * Cambia la region del usuario, que decide que desastres le llegan. Solo acepta regiones activas
     * de la tabla regiones (las crea o desactiva el admin). Guarda el codigo y la llave a la vez.
     */
    @Transactional
    public UserSummaryDto cambiarNombre(Integer userId, String nombre) {
        String limpio = nombre == null ? "" : nombre.trim();
        if (limpio.length() < 2 || limpio.length() > 100) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El nombre debe tener entre 2 y 100 caracteres");
        }
        Usuario u = usuarioRepo.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Usuario no encontrado"));
        u.setNombre(limpio);
        usuarioRepo.save(u);
        return resumen(u);
    }

    @Transactional
    public UserSummaryDto cambiarRegion(Integer userId, String codigo) {
        Usuario u = usuarioRepo.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Usuario no encontrado"));
        Region region = regionRepo.findByCodigoAndActivaTrue(codigo == null ? "" : codigo.trim())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Region no valida"));
        u.setRegion(region.getCodigo());
        u.setIdRegion(region.getIdRegion());
        usuarioRepo.save(u);
        return resumen(u);
    }

    /**
     * Elige el invernadero de la coleccion virtual entre los que el usuario tiene (el predeterminado y los comprados).
     * 404 si no existe o esta desactivado; 409 si todavia no lo compro.
     */
    @Transactional
    public UserSummaryDto cambiarInvernadero(Integer userId, Integer idItem) {
        Usuario u = usuarioRepo.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Usuario no encontrado"));
        ItemTienda item = itemRepo.findById(idItem == null ? -1 : idItem)
                .filter(i -> Boolean.TRUE.equals(i.getActivo()) && TiendaService.INVERNADERO.equals(i.getTipo()))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Invernadero no encontrado"));
        List<Integer> comprados = inventarioRepo.findByIdUsuario(userId).stream().map(InventarioUsuario::getIdItem).toList();
        if (!TiendaService.esSuyo(item, comprados)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Primero compra este invernadero en la tienda");
        }
        u.setIdInvernadero(item.getIdItem());
        usuarioRepo.save(u);
        return resumen(u);
    }

    /**
     * Cambia la foto de perfil. El StorageService solo acepta JPG, PNG o WEBP (por el contenido real, no por el
     * nombre); la foto anterior se borra del disco una vez guardada la nueva.
     */
    @Transactional
    public UserSummaryDto cambiarFoto(Integer userId, MultipartFile foto) {
        Usuario u = usuarioRepo.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Usuario no encontrado"));
        String anterior = u.getFotoUrl();
        u.setFotoUrl(storage.save(foto));
        usuarioRepo.save(u);
        storage.delete(anterior);
        return resumen(u);
    }

    /** Codigo del invernadero elegido; SELVA (el predeterminado) si no eligio otro o ya no existe. */
    private String invernadero(Usuario u) {
        if (u.getIdInvernadero() == null) {
            return "SELVA";
        }
        return itemRepo.findById(u.getIdInvernadero()).map(ItemTienda::getCodigo).orElse("SELVA");
    }

    private UserSummaryDto resumen(Usuario u) {
        int nivel = u.getNivel() != null ? u.getNivel() : 1;
        int xp = u.getExperienciaXp() != null ? u.getExperienciaXp() : 0;
        // XP total que marca el inicio del nivel actual (0 si el nivel no esta en la tabla)
        int base = nivelRepo.findById(nivel).map(n -> n.getXpRequerido()).orElse(0);
        // En el ultimo nivel no hay siguiente: ambos limites quedan en el XP y la barra se ve completa
        int siguiente = nivelRepo.findById(nivel + 1).map(n -> n.getXpRequerido()).orElse(Math.max(xp, base));

        String email = u.getEmail();
        int streakDays = u.getRachaDias() != null ? u.getRachaDias() : 0;
        String region = u.getRegion() != null && !u.getRegion().isBlank() ? u.getRegion() : "GLOBAL";

        return new UserSummaryDto(
                u.getNombre(),
                nivel,
                badge(nivel),
                u.getMonedas() != null ? u.getMonedas() : 0,
                xp,
                siguiente,
                u.getFotoUrl(),
                email,
                streakDays,
                base,
                region,
                invernadero(u));
    }

    private static String badge(int nivel) {
        if (nivel >= 10) {
            return "Maestro Botanico";
        }
        if (nivel >= 5) {
            return "Cuidador";
        }
        return "Aprendiz";
    }
}
