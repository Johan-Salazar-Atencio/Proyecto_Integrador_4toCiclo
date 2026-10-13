"""Pruebas del motor de IA. No requieren base de datos (SimpleTestCase)."""
from django.test import SimpleTestCase

from ia_engine.providers import base
from ia_engine.providers.factory import get_provider
from ia_engine.providers.mock_provider import MockProvider

CONTRATO_IDENT = {"nombreComun", "nombreCientifico", "confianza", "cuidados", "datosRaw"}


class PromptTests(SimpleTestCase):
    def test_prompt_cuidados_conserva_llaves_json(self):
        prompt = base.prompt_cuidados("Monstera", "Monstera deliciosa")
        self.assertIn("Monstera deliciosa", prompt)
        self.assertIn('"cuidados"', prompt)

    def test_prompt_con_nombre_vacio(self):
        self.assertTrue(base.prompt_cuidados(""))


class ParseJsonTests(SimpleTestCase):
    def test_con_fences(self):
        texto = '```json\n{"a": 1}\n```'
        self.assertEqual(base.parse_json(texto), {"a": 1})

    def test_json_plano(self):
        self.assertEqual(base.parse_json('{"b": 2}'), {"b": 2})


class NormalizacionTests(SimpleTestCase):
    def test_identificacion_contrato(self):
        r = base.normalizar_identificacion(
            {"nombre_comun": "X", "confianza": 90,
             "cuidados": [{"tipo": "riego", "titulo": "t", "contenido": "c"}]},
            "{}",
        )
        self.assertEqual(set(r.keys()), CONTRATO_IDENT)
        self.assertEqual(r["cuidados"][0]["tipo"], "RIEGO")

    def test_tipo_invalido_es_otro(self):
        r = base.normalizar_identificacion(
            {"nombre_comun": "X", "cuidados": [{"tipo": "xyz", "titulo": "t", "contenido": "c"}]},
            "{}",
        )
        self.assertEqual(r["cuidados"][0]["tipo"], "OTRO")

    def test_cuidado_sin_contenido_se_descarta(self):
        r = base.normalizar_identificacion(
            {"nombre_comun": "X", "cuidados": [{"tipo": "RIEGO", "titulo": "t", "contenido": ""}]},
            "{}",
        )
        self.assertEqual(r["cuidados"], [])

    def test_cuidados_metadatos(self):
        r = base.normalizar_cuidados(
            {"nombre_comun": "Y", "es_toxica_mascotas": True,
             "frecuencia_riego_dias": 5, "frecuencia_sol_horas": 6, "cuidados": []},
            "{}",
        )
        self.assertTrue(r["esToxicaMascotas"])
        self.assertEqual(r["frecuenciaRiegoDias"], 5)
        self.assertEqual(r["frecuenciaSolHoras"], 6)


class MockProviderTests(SimpleTestCase):
    def test_identificar(self):
        r = MockProvider().identificar(b"bytes", "foto.jpg")
        self.assertEqual(r["nombreComun"], "Monstera")
        self.assertGreaterEqual(len(r["cuidados"]), 3)

    def test_cuidados_de(self):
        r = MockProvider().cuidados_de("Pothos", "Epipremnum aureum")
        self.assertEqual(r["nombreCientifico"], "Epipremnum aureum")
        self.assertIn("esToxicaMascotas", r)


class FactoryTests(SimpleTestCase):
    def test_proveedor_explicito_mock(self):
        self.assertEqual(get_provider("mock").name, "mock")

    def test_proveedor_desconocido(self):
        with self.assertRaises(ValueError):
            get_provider("inexistente")
