package com.plantia.data

/** Cuidados que se pueden aplicar a una planta; `name` es el valor que espera el backend. */
enum class AccionCuidado(val etiqueta: String, val pista: String) {
    REGAR("Regar", "+20%"),
    SOL("Sol", "+15%"),
    ABONAR("Abonar", "+10%")
}
