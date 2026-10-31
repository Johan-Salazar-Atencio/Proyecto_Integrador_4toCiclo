package com.plantia.services;

import java.util.List;

import org.springframework.stereotype.Service;

import com.plantia.dto.QuickAccessDto;

@Service
public class AccesoService {

    /** Accesos rapidos que muestra el HomeScreen del movil (colores hex). */
    public List<QuickAccessDto> listar() {
        return List.of(
                new QuickAccessDto("scan", "Escanear", "Identificar y cuidar", "camera", "#E8F8F0", "#22C55E"),
                new QuickAccessDto("collection", "Mi Coleccion", "Plantas que has visto", "leaf", "#E8F8F0", "#22C55E"),
                new QuickAccessDto("store", "Tienda Botanica", "Macetas & abonos", "store", "#FFF3E0", "#F59E0B"),
                new QuickAccessDto("encyclopedia", "Enciclopedia", "Guias botanicas", "book", "#E8F4FD", "#3B82F6"));
    }
}
