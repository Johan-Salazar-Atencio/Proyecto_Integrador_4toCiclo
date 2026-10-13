"""Servicios del motor de IA: inferencia + persistencia en el catalogo."""
import json
import logging

from django.db import transaction

from catalog.models import CuidadoSeccion, PlantaCatalogo

logger = logging.getLogger(__name__)

_SPRITE_DEFECTO = "/sprites/default.png"
_RIEGO_DEFECTO = 7
_SOL_DEFECTO = 4


def _json_obj(valor):
    """Convierte `datosRaw` (texto JSON) en objeto para el JSONField de Postgres."""
    if valor is None or isinstance(valor, (dict, list)):
        return valor
    try:
        return json.loads(valor)
    except (TypeError, ValueError):
        return {"raw": str(valor)}


@transaction.atomic
def persistir_catalogo(resultado: dict) -> int:
    """Crea o actualiza la planta del catalogo a partir del resultado de la IA.

    Devuelve `id_catalogo`. Las secciones de cuidado se reemplazan por las
    generadas en esta corrida (idempotente por especie).
    """
    cientifico = (resultado.get("nombreCientifico") or "").strip()
    comun = (resultado.get("nombreComun") or "Planta").strip()
    cuidados = resultado.get("cuidados") or []

    planta, _creada = PlantaCatalogo.objects.update_or_create(
        nombre_cientifico=cientifico or comun,
        defaults={
            "nombre_comun": comun,
            "frecuencia_riego_dias": int(resultado.get("frecuenciaRiegoDias") or _RIEGO_DEFECTO),
            "frecuencia_sol_horas": int(resultado.get("frecuenciaSolHoras") or _SOL_DEFECTO),
            "es_toxica_mascotas": bool(resultado.get("esToxicaMascotas", False)),
            "datos_ia_raw": _json_obj(resultado.get("datosRaw")),
            "sprite_base_url": _SPRITE_DEFECTO,
        },
    )

    CuidadoSeccion.objects.filter(id_catalogo=planta).delete()
    filas = [
        CuidadoSeccion(
            id_catalogo=planta,
            tipo=c.get("tipo", "OTRO"),
            titulo=c.get("titulo", "")[:100],
            contenido=c.get("contenido", ""),
            orden=i,
            generado_por_ia=True,
        )
        for i, c in enumerate(cuidados)
        if c.get("contenido")
    ]
    CuidadoSeccion.objects.bulk_create(filas)

    logger.info(
        "Catalogo persistido id=%s (%s) con %s secciones",
        planta.id_catalogo,
        planta.nombre_cientifico,
        len(filas),
    )
    return planta.id_catalogo


@transaction.atomic
def reemplazar_cuidados(id_catalogo: int, resultado: dict) -> int:
    """Regenera las secciones de cuidado de una planta existente."""
    planta = PlantaCatalogo.objects.get(pk=id_catalogo)
    planta.datos_ia_raw = _json_obj(resultado.get("datosRaw"))
    if resultado.get("esToxicaMascotas") is not None:
        planta.es_toxica_mascotas = resultado["esToxicaMascotas"]
    if resultado.get("frecuenciaRiegoDias"):
        planta.frecuencia_riego_dias = resultado["frecuenciaRiegoDias"]
    if resultado.get("frecuenciaSolHoras"):
        planta.frecuencia_sol_horas = resultado["frecuenciaSolHoras"]
    planta.save()

    CuidadoSeccion.objects.filter(id_catalogo=planta).delete()
    filas = [
        CuidadoSeccion(
            id_catalogo=planta,
            tipo=c.get("tipo", "OTRO"),
            titulo=c.get("titulo", "")[:100],
            contenido=c.get("contenido", ""),
            orden=i,
            generado_por_ia=True,
        )
        for i, c in enumerate(resultado.get("cuidados") or [])
        if c.get("contenido")
    ]
    CuidadoSeccion.objects.bulk_create(filas)
    return len(filas)
