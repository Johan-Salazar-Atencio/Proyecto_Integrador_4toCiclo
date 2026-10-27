package com.plantia.dto;

/** Coincide con RecentScan del movil. statusColor es un hex (#RRGGBB). */
public record RecentScanDto(
        String id,
        String plantName,
        String confidence,
        String statusColor
) {
}
