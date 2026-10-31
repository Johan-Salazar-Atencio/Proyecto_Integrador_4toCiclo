"""Pobla el catalogo y sus cuidados usando IA.

Uso:
    python manage.py poblar_catalogo
    python manage.py poblar_catalogo "Monstera:Monstera deliciosa" "Ficus lyrata"
    python manage.py poblar_catalogo --provider mock

Cada argumento tiene la forma "Nombre comun:Nombre cientifico" (la parte
cientifica es opcional). Si no se pasan argumentos se usa un listado base.
"""
from django.core.management.base import BaseCommand

from ia_engine.providers.factory import get_provider
from ia_engine.services import persistir_catalogo

CATALOGO_BASE = [
    "Monstera:Monstera deliciosa",
    "Ficus lyrata:Ficus lyrata",
    "Pothos:Epipremnum aureum",
    "Sansevieria:Dracaena trifasciata",
    "Aloe vera:Aloe barbadensis miller",
    "Helecho de Boston:Nephrolepis exaltata",
]


class Command(BaseCommand):
    help = "Genera catalogo + cuidados con IA y los guarda en PostgreSQL."

    def add_arguments(self, parser):
        parser.add_argument("especies", nargs="*", help='"Nombre comun:Nombre cientifico"')
        parser.add_argument("--provider", default=None, help="openai|gemini|local|mock|auto")

    def handle(self, *args, **options):
        especies = options["especies"] or CATALOGO_BASE
        provider = get_provider(options["provider"])
        self.stdout.write(self.style.NOTICE(f"Proveedor de IA: {provider.name}"))

        for entrada in especies:
            comun, _, cientifico = entrada.partition(":")
            comun = comun.strip()
            cientifico = cientifico.strip()
            self.stdout.write(f"Generando ficha de {comun}...")
            try:
                resultado = provider.cuidados_de(comun, cientifico)
                id_catalogo = persistir_catalogo(resultado)
                self.stdout.write(
                    self.style.SUCCESS(
                        f"  OK id={id_catalogo} ({resultado['nombreCientifico']}) "
                        f"con {len(resultado['cuidados'])} secciones"
                    )
                )
            except Exception as exc:  # noqa: BLE001
                self.stdout.write(self.style.ERROR(f"  ERROR {comun}: {exc}"))

        self.stdout.write(self.style.SUCCESS("Catalogo poblado."))
