"""Contrato comun de proveedores de IA y normalizacion de respuestas.

El formato de salida es la clave de la integracion: Spring Boot consume este
JSON con su `IaResultDto`, cuyos campos son camelCase:
    { nombreComun, nombreCientifico, confianza, cuidados[], datosRaw }

Cada proveedor (OpenAI, Gemini, local, mock) traduce la respuesta cruda del
modelo a este contrato unico.
"""
import json
import re
from abc import ABC, abstractmethod

TIPOS_VALIDOS = {
    "RIEGO",
    "LUZ",
    "SUSTRATO",
    "TEMPERATURA",
    "PODA",
    "PLAGAS",
    "TOXICIDAD",
    "ABONO",
    "OTRO",
}

PROMPT_IDENTIFICAR = (
    "Eres un botanico experto. Analiza la imagen de la planta y responde SOLO "
    "con un objeto JSON valido, sin texto adicional, con la forma: "
    '{"nombre_comun": "string", "nombre_cientifico": "string", '
    '"confianza": 0-100, "cuidados": [{"tipo": "RIEGO|LUZ|SUSTRATO|TEMPERATURA|'
    'PODA|PLAGAS|TOXICIDAD|ABONO|OTRO", "titulo": "string corto", '
    '"contenido": "instruccion concreta"}], '
    '"apariencia": {"forma": "arbusto|suculenta|cactus|roseta|monstera|trepadora|'
    'helecho|palmera|arbol", "color_hoja": "#RRGGBB", "tiene_flor": true|false, '
    '"color_flor": "#RRGGBB o null", "tiene_fruto": true|false, '
    '"color_fruto": "#RRGGBB o null"}}. '
    "Incluye al menos las secciones RIEGO, LUZ y TEMPERATURA. "
    "La apariencia describe a la planta adulta tipica de la especie (no solo la "
    "foto): su forma de crecimiento, el color dominante de sus hojas, y si da "
    "flores o frutos vistosos con su color al madurar."
)

# Formas de crecimiento que entiende el jardin virtual (la web elige el dibujo)
FORMAS = {
    "arbusto", "suculenta", "cactus", "roseta", "monstera",
    "trepadora", "helecho", "palmera", "arbol",
}
_COLOR_HOJA_DEFECTO = "#4f9a52"
_HEX = re.compile(r"#?([0-9a-fA-F]{6})")

PROMPT_CUIDADOS = (
    "Eres un botanico experto. Para la planta '__NOMBRE__' (nombre cientifico "
    "'__CIENTIFICO__') responde SOLO con un objeto JSON valido, sin texto "
    "adicional, con la forma: "
    '{"nombre_comun": "string", "nombre_cientifico": "string", '
    '"es_toxica_mascotas": true|false, "frecuencia_riego_dias": int, '
    '"frecuencia_sol_horas": int, "cuidados": [{"tipo": "RIEGO|LUZ|SUSTRATO|'
    'TEMPERATURA|PODA|PLAGAS|TOXICIDAD|ABONO|OTRO", "titulo": "string", '
    '"contenido": "string"}]}. '
    "Incluye al menos RIEGO, LUZ, SUSTRATO, TEMPERATURA y TOXICIDAD."
)


def prompt_cuidados(nombre_comun: str, nombre_cientifico: str = "") -> str:
    """Rellena el prompt de cuidados sin romper las llaves literales del JSON."""
    return (
        PROMPT_CUIDADOS
        .replace("__NOMBRE__", nombre_comun or "planta")
        .replace("__CIENTIFICO__", nombre_cientifico or "")
    )


def parse_json(texto):
    """Extrae el primer objeto JSON de un texto del modelo (tolera ```json)."""
    if isinstance(texto, dict):
        return texto
    texto = (texto or "").strip()
    if texto.startswith("```"):
        texto = texto.strip("`")
        if texto.lower().startswith("json"):
            texto = texto[4:]
    inicio = texto.find("{")
    fin = texto.rfind("}")
    if inicio == -1 or fin == -1:
        raise ValueError("La IA no devolvio un JSON valido")
    return json.loads(texto[inicio : fin + 1])


def _datos_raw(raw) -> str:
    """Garantiza que `datosRaw` sea un JSON valido.

    La columna `respuesta_ia_raw` / `datos_ia_raw` es JSONB: si el modelo
    responde con texto antes del JSON ("Aqui tienes: {...}"), insertar la
    respuesta cruda revienta la columna. En ese caso se envuelve el texto
    crudo en un objeto JSON, de modo que nunca se pierde la respuesta.
    """
    if raw is None:
        return "{}"
    if isinstance(raw, (dict, list)):
        return json.dumps(raw, ensure_ascii=False)
    texto = str(raw).strip()
    if not texto:
        return "{}"
    try:
        json.loads(texto)
        return texto
    except (TypeError, ValueError):
        return json.dumps({"respuesta_cruda": texto}, ensure_ascii=False)


def _normalizar_cuidados(items):
    salida = []
    for item in items or []:
        if not isinstance(item, dict):
            continue
        tipo = str(item.get("tipo", "OTRO")).upper()
        if tipo not in TIPOS_VALIDOS:
            tipo = "OTRO"
        titulo = str(item.get("titulo") or tipo.title())[:100]
        contenido = str(item.get("contenido") or "").strip()
        if not contenido:
            continue
        salida.append({"tipo": tipo, "titulo": titulo, "contenido": contenido})
    return salida


def _color(valor):
    """'#RRGGBB' en minusculas, o None si el modelo no dio un color valido."""
    coincide = _HEX.fullmatch(str(valor or "").strip())
    return f"#{coincide.group(1).lower()}" if coincide else None


def _booleano(valor) -> bool:
    if isinstance(valor, str):
        return valor.strip().lower() in {"true", "si", "sí", "yes", "1"}
    return bool(valor)


def normalizar_apariencia(data) -> dict | None:
    """Rasgos visuales para dibujar la planta en el jardin virtual.

    Devuelve None si el modelo no los envio (la web usa su dibujo por defecto).
    """
    a = data.get("apariencia")
    if not isinstance(a, dict):
        return None

    def campo(snake, camel):
        return a.get(snake, a.get(camel))

    forma = str(a.get("forma") or "").strip().lower()
    color_flor = _color(campo("color_flor", "colorFlor"))
    color_fruto = _color(campo("color_fruto", "colorFruto"))
    return {
        "forma": forma if forma in FORMAS else "arbusto",
        "colorHoja": _color(campo("color_hoja", "colorHoja")) or _COLOR_HOJA_DEFECTO,
        # Sin color no se puede dibujar la flor/el fruto: se trata como si no tuviera
        "tieneFlor": _booleano(campo("tiene_flor", "tieneFlor")) and color_flor is not None,
        "colorFlor": color_flor,
        "tieneFruto": _booleano(campo("tiene_fruto", "tieneFruto")) and color_fruto is not None,
        "colorFruto": color_fruto,
    }


def normalizar_identificacion(data, raw):
    return {
        "apariencia": normalizar_apariencia(data),
        "nombreComun": data.get("nombre_comun") or data.get("nombreComun") or "Planta desconocida",
        "nombreCientifico": data.get("nombre_cientifico")
        or data.get("nombreCientifico")
        or "Especie no identificada",
        "confianza": float(data.get("confianza") or 0),
        "cuidados": _normalizar_cuidados(data.get("cuidados")),
        "datosRaw": _datos_raw(raw),
    }


def normalizar_cuidados(data, raw):
    base = normalizar_identificacion(data, raw)
    base["esToxicaMascotas"] = bool(data.get("es_toxica_mascotas", False))
    base["frecuenciaRiegoDias"] = int(data.get("frecuencia_riego_dias") or 7)
    base["frecuenciaSolHoras"] = int(data.get("frecuencia_sol_horas") or 4)
    return base


class IaProvider(ABC):
    """Interfaz que todo proveedor de IA debe cumplir."""

    name = "base"

    @abstractmethod
    def identificar(self, image_bytes: bytes, filename: str = "scan.jpg") -> dict:
        """Identifica la planta de la foto y devuelve el contrato normalizado."""

    @abstractmethod
    def cuidados_de(self, nombre_comun: str, nombre_cientifico: str = "") -> dict:
        """Genera la ficha de cuidados de una especie sin necesidad de imagen."""
