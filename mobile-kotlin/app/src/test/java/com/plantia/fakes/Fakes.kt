package com.plantia.fakes

import com.plantia.data.model.AccionCuidado
import com.plantia.data.model.AlertSeverity
import com.plantia.data.model.DailyMission
import com.plantia.data.model.PlantSummary
import com.plantia.data.model.UserSummary
import com.plantia.data.model.WeatherAlert
import com.plantia.data.remote.dto.ScanResultDto
import com.plantia.data.repository.AlertaResultado
import com.plantia.data.repository.CollectionRepository
import com.plantia.data.repository.ColeccionResultado
import com.plantia.data.repository.DetalleColeccionResultado
import com.plantia.data.repository.GardenRepository
import com.plantia.data.repository.HomeDatos
import com.plantia.data.repository.HomeRepository
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

fun usuarioEjemplo(nivel: Int = 1, xp: Int = 0) = UserSummary(
    name = "Johan",
    level = nivel,
    badge = "Aprendiz",
    leaves = 100,
    experience = xp,
    experienceToNextLevel = 500
)

fun alerta() = WeatherAlert(
    title = "Helada Inminente",
    description = "Resguarda tus plantas",
    severity = AlertSeverity.HIGH,
    timeRemaining = "01h 00m",
    actionText = "Prevenir evento"
)

/** Home falso: la carga inicial y cada consulta del sondeo se configuran por separado. */
class FakeHomeRepository : HomeRepository {
    var datos = HomeDatos(
        usuario = usuarioEjemplo(), alerta = null, planta = planta(), accesos = emptyList(),
        misiones = emptyList(), fallidas = 0, sinConexion = false
    )
    var alertaResultado: AlertaResultado = AlertaResultado.Sin
    var usuarioResultado: UserSummary? = usuarioEjemplo()
    var misionesResultado: List<DailyMission>? = null
    var consultasAlerta = 0

    override suspend fun cargar() = datos

    override suspend fun alerta(): AlertaResultado {
        consultasAlerta++
        return alertaResultado
    }

    override suspend fun usuario() = usuarioResultado

    override suspend fun misiones() = misionesResultado
}

/** Jardin falso: devuelve lo que se configure y recuerda las llamadas. */
class FakeGardenRepository : GardenRepository {
    var activa: PlantaActivaResultado = PlantaActivaResultado.ConPlanta(planta())
    var lista: PlantasResultado = PlantasResultado.Con(emptyList())
    var alDigitalizar: PlantaResultado = PlantaResultado.Con(planta())
    var alSeleccionar: PlantaResultado = PlantaResultado.Con(planta())
    var alCuidar: PlantaResultado = PlantaResultado.Con(planta())
    var alPrevenir: PlantaResultado = PlantaResultado.Con(planta().copy(refugiada = true))
    var alRevivir: PlantaResultado = PlantaResultado.Con(planta())
    var alGolpeVisto: PlantaResultado = PlantaResultado.Con(planta())

    val cuidados = mutableListOf<Pair<Int, AccionCuidado>>()
    val prevenidos = mutableListOf<Int>()
    val revividos = mutableListOf<Int>()
    val golpesVistos = mutableListOf<Int>()
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

    override suspend fun prevenir(idPlanta: Int): PlantaResultado {
        prevenidos += idPlanta
        return alPrevenir
    }

    override suspend fun revivir(idPlanta: Int): PlantaResultado {
        revividos += idPlanta
        return alRevivir
    }

    override suspend fun golpeVisto(idPlanta: Int): PlantaResultado {
        golpesVistos += idPlanta
        return alGolpeVisto
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

fun mision(id: String = "mission_1", progreso: Int = 0, meta: Int = 2) = DailyMission(
    id = id,
    title = "Riego Consciente",
    reward = "+20 XP · +50 gotas de rocio",
    progress = progreso.toFloat() / meta,
    currentCount = progreso,
    targetCount = meta,
    isCompleted = progreso >= meta
)

/** Arte falso: devuelve lo que se configure. */
class FakeArteRepository(var arte: com.plantia.data.model.Arte? = null) : com.plantia.data.repository.ArteRepository {
    var consultas = 0

    override suspend fun cargar(): com.plantia.data.model.Arte? {
        consultas++
        return arte
    }
}
