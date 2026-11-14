package com.plantia.data

import com.plantia.data.remote.CuidadoSeccionDto
import com.plantia.data.remote.ScanResultDto

/** Datos de ejemplo para las Previews de la ficha de planta. */
object MockPlantData {

    /** Respuesta de ejemplo con la misma forma que POST /api/v1/escaneos. */
    val scanResult = ScanResultDto(
        idEscaneo = 12,
        idCatalogo = 1,
        nombreComun = "Monstera",
        nombreCientifico = "Monstera deliciosa",
        confianza = 94.0,
        imagenUrl = null,
        nuevoEnColeccion = true,
        cuidados = listOf(
            CuidadoSeccionDto(1, "RIEGO", "Riego moderado", "Riega cuando los primeros 3 cm de sustrato esten secos; evita encharcamientos.", 0, true),
            CuidadoSeccionDto(2, "LUZ", "Luz indirecta brillante", "Prefiere luz indirecta abundante; el sol directo quema sus hojas.", 1, true),
            CuidadoSeccionDto(3, "TEMPERATURA", "Clima calido", "Mantenla entre 15 y 30 C, lejos de corrientes frias.", 2, true),
            CuidadoSeccionDto(4, "SUSTRATO", "Sustrato aireado", "Mezcla con perlita para buen drenaje.", 3, true),
            CuidadoSeccionDto(5, "TOXICIDAD", "Toxica para mascotas", "Contiene oxalato de calcio; puede irritar a perros y gatos.", 4, true)
        )
    )

    /** Escaneo que la IA no pudo identificar. */
    val scanResultFallo = ScanResultDto(
        idEscaneo = 13,
        nombreComun = "Planta desconocida",
        confianza = 0.0,
        cuidados = emptyList()
    )
}
