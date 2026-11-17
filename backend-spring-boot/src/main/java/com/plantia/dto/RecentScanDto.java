package com.plantia.dto;

/** Coincide con RecentScan del movil. statusColor es un hex (#RRGGBB); fotoUrl es la foto que se tomo o subio. */
public record RecentScanDto(
        String id,
        String plantName,
        String confidence,
        Integer confianza,
        String statusColor,
        String fotoUrl
) {
}
