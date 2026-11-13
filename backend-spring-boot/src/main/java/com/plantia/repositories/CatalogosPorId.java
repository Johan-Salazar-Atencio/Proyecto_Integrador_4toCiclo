package com.plantia.repositories;

import java.util.Collection;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

import com.plantia.models.PlantaCatalogo;

/** Carga en UNA consulta las especies de una lista (evita un findById por fila: N+1). */
public final class CatalogosPorId {

    private CatalogosPorId() {
    }

    public static <T> Map<Integer, PlantaCatalogo> de(PlantaCatalogoRepository repo, Collection<T> filas,
                                                      Function<T, Integer> idCatalogo) {
        var ids = filas.stream().map(idCatalogo).filter(Objects::nonNull).collect(Collectors.toSet());
        if (ids.isEmpty()) {
            return Map.of();
        }
        return repo.findAllById(ids).stream()
                .collect(Collectors.toMap(PlantaCatalogo::getIdCatalogo, Function.identity()));
    }
}
