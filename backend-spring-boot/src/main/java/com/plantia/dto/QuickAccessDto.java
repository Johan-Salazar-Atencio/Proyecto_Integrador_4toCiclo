package com.plantia.dto;

/** Coincide con QuickAccessItem del movil. Colores en hex (#RRGGBB). */
public record QuickAccessDto(
        String id,
        String title,
        String subtitle,
        String icon,
        String backgroundColor,
        String iconColor
) {
}
