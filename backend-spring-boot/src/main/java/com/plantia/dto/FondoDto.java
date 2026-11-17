package com.plantia.dto;

/**
 * Fondo de la escena del jardin. La imagen es una tira horizontal de `frames` cuadros de
 * widthPx x heightPx; imageUrl es relativa al servidor (/files/...).
 */
public record FondoDto(
        String id,
        String momento,
        String name,
        String imageUrl,
        int frames,
        int widthPx,
        int heightPx,
        int msPerFrame,
        boolean isDefault,
        /** true si el usuario compro este fondo y lo tiene puesto: reemplaza al predeterminado de su momento. */
        boolean equipado
) {
}
