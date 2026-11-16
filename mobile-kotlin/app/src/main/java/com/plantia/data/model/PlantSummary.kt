package com.plantia.data.model

data class PlantSummary(
    val name: String,
    val species: String,
    val healthPercent: Int,
    val humidityPercent: Int,
    val lightPercent: Int,
    val lastWatered: String,
    val imageUrl: String? = null,
    /** Id de la planta digital en el backend; null en datos de ejemplo. */
    val id: Int? = null,
    /** Planta que se muestra en el jardin. */
    val activa: Boolean = false,
    /** true mientras la planta esta a resguardo de un desastre. */
    val refugiada: Boolean = false,
    /** Rasgos de la planta adulta que dio la IA (forma y colores); null = dibujo por defecto. */
    val apariencia: Apariencia? = null,
    /** Dias con cuidado contados y dias necesarios para crecer del todo (0 si el servidor no los manda). */
    val diasCuidados: Int = 0,
    val diasTotales: Int = 0,
    /** Fase de la planta: SEMILLA, BROTE, ADULTA o MUERTA (vacia si el servidor no la manda). */
    val fase: String = "",
    /** Golpeada por un evento: solo el abono la salva antes de que se acabe el plazo. */
    val enRecuperacion: Boolean = false,
    /** Segundos que quedan para abonarla; null si no esta en recuperacion. */
    val recuperacionSegundos: Long? = null,
    /** Evento que la golpeo y cuya animacion aun no se vio (null = ninguna pendiente). */
    val golpeTipo: String? = null
) {
    /** Murio por no abonarla a tiempo: se puede revivir a cambio de hojas. */
    val muerta: Boolean get() = fase == "MUERTA"

    /** El evento ya esta prevenido (a resguardo) o la planta no se puede proteger. */
    val protegida: Boolean get() = refugiada
}

/** Apariencia que da la IA al escanear; cada campo ausente toma el valor por defecto del sprite. */
data class Apariencia(
    val forma: String? = null,
    val colorHoja: String? = null,
    val tieneFlor: Boolean? = null,
    val colorFlor: String? = null,
    val tieneFruto: Boolean? = null,
    val colorFruto: String? = null
)
