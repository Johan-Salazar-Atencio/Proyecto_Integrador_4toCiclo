"""Proveedor simulado: permite probar todo el flujo sin claves ni modelos reales."""
import json

from .base import IaProvider, normalizar_cuidados, normalizar_identificacion

_CUIDADOS_DEMO = [
    {"tipo": "RIEGO", "titulo": "Riego", "contenido": "Riega cuando los primeros 3 cm de sustrato esten secos, 1 vez por semana."},
    {"tipo": "LUZ", "titulo": "Luz", "contenido": "Luz indirecta brillante; evita el sol directo del mediodia."},
    {"tipo": "SUSTRATO", "titulo": "Sustrato", "contenido": "Sustrato suelto y drenado con perlita y turba."},
    {"tipo": "TEMPERATURA", "titulo": "Temperatura", "contenido": "Entre 18 y 27 grados; protege de corrientes de aire frio."},
    {"tipo": "TOXICIDAD", "titulo": "Toxicidad", "contenido": "Toxica para mascotas si se ingiere; mantenla fuera de su alcance."},
]


class MockProvider(IaProvider):
    name = "mock"

    def identificar(self, image_bytes: bytes, filename: str = "scan.jpg") -> dict:
        data = {
            "nombre_comun": "Monstera",
            "nombre_cientifico": "Monstera deliciosa",
            "confianza": 92.5,
            "cuidados": _CUIDADOS_DEMO,
        }
        return normalizar_identificacion(data, json.dumps(data, ensure_ascii=False))

    def cuidados_de(self, nombre_comun: str, nombre_cientifico: str = "") -> dict:
        data = {
            "nombre_comun": nombre_comun or "Monstera",
            "nombre_cientifico": nombre_cientifico or "Monstera deliciosa",
            "es_toxica_mascotas": True,
            "frecuencia_riego_dias": 7,
            "frecuencia_sol_horas": 4,
            "cuidados": _CUIDADOS_DEMO,
        }
        return normalizar_cuidados(data, json.dumps(data, ensure_ascii=False))
