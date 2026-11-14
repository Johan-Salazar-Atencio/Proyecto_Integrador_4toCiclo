package com.plantia.fakes

import com.plantia.data.model.AccionCuidado
import com.plantia.data.model.PlantSummary
import com.plantia.data.remote.dto.ScanResultDto
import com.plantia.data.repository.CollectionRepository
import com.plantia.data.repository.ColeccionResultado
import com.plantia.data.repository.DetalleColeccionResultado
import com.plantia.data.repository.GardenRepository
import com.plantia.data.repository.PlantaActivaResultado
import com.plantia.data.repository.PlantaResultado
import com.plantia.data.repository.PlantasResultado
import com.plantia.data.repository.ScanRepository
import com.plantia.data.model.RecentScan
import java.io.File
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

fun planta(
    id: Int? = 1,
    salud: Int = 50,
    agua: Int = 50,
    luz: Int = 50,
    activa: Boolean = true
) = PlantSummary(
    name = "Monstera",
    species = "Monstera deliciosa",
    healthPercent = salud,
    humidityPercent = agua,
    lightPercent = luz,
    lastWatered = "Hace 1 hora",
    id = id,
    activa = activa
)

/** Jardin falso: devuelve lo que se configure y recuerda las llamadas. */
class FakeGardenRepository : GardenRepository {
    var activa: PlantaActivaResultado = PlantaActivaResultado.ConPlanta(planta())
    var lista: PlantasResultado = PlantasResultado.Con(emptyList())
    var alDigitalizar: PlantaResultado = PlantaResultado.Con(planta())
    var alSeleccionar: PlantaResultado = PlantaResultado.Con(planta())
    var alCuidar: PlantaResultado = PlantaResultado.Con(planta())

    val cuidados = mutableListOf<Pair<Int, AccionCuidado>>()
    val digitalizados = mutableListOf<Pair<Int, Int?>>()

    override suspend fun plantaActiva() = activa
    override suspend fun listar() = lista

    override suspend fun digitalizar(idEscaneo: Int, idCatalogo: Int?): PlantaResultado {
        digitalizados += idEscaneo to idCatalogo
        return alDigitalizar
    }

    override suspend fun seleccionar(idPlanta: Int) = alSeleccionar

    override suspend fun cuidar(idPlanta: Int, accion: AccionCuidado): PlantaResultado {
        cuidados += idPlanta to accion
        return alCuidar
    }
}

class FakeCollectionRepository : CollectionRepository {
    var lista: ColeccionResultado = ColeccionResultado.Con(emptyList())
    var detalle: DetalleColeccionResultado = DetalleColeccionResultado.Fallo("sin datos")
    val agregados = mutableListOf<Int>()

    override suspend fun listar() = lista

    override suspend fun agregar(idEscaneo: Int): ColeccionResultado {
        agregados += idEscaneo
        return lista
    }

    override suspend fun detalle(id: Int) = detalle
}

class FakeScanRepository : ScanRepository {
    private val ultimo = MutableStateFlow<ScanResultDto?>(null)
    override val ultimoResultado: StateFlow<ScanResultDto?> = ultimo

    var resultado: ScanResultDto = ScanResultDto(idEscaneo = 7, nombreComun = "Monstera")
    var recientes: List<RecentScan> = emptyList()
    var falla: Exception? = null

    override suspend fun escanear(foto: File): ScanResultDto {
        falla?.let { throw it }
        ultimo.value = resultado
        return resultado
    }

    override suspend fun recientes(): List<RecentScan> {
        falla?.let { throw it }
        return recientes
    }

    override fun limpiarUltimo() {
        ultimo.value = null
    }

    fun fijarUltimo(valor: ScanResultDto?) {
        ultimo.value = valor
    }
}
