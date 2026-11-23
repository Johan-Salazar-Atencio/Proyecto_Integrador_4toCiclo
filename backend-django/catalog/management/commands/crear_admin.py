"""Crea el administrador real o da rol ADMIN a una cuenta existente (QA-40).

En produccion (PLANTIA_PRODUCCION=true) Spring deshabilita las cuentas de demostracion, incluido admin@plantia.test:
este comando es la forma explicita de tener un admin. La clave NUNCA va fija ni en la linea de comandos: se lee de la
variable PLANTIA_ADMIN_PASSWORD (para un despliegue automatizado) o se pide dos veces por teclado.

    python manage.py crear_admin --email admin@mi-dominio.pe --nombre "Admin Plantia"
"""
import getpass
import os

import bcrypt
from django.core.management.base import BaseCommand, CommandError

from catalog.models import Usuario

MIN_CLAVE = 12
CUENTAS_DEMO = {"admin@plantia.test", "johan@plantia.test", "lucia@plantia.test"}


def validar_clave(clave):
    if len(clave) < MIN_CLAVE:
        raise CommandError(f"La clave debe tener al menos {MIN_CLAVE} caracteres")
    if len(clave.encode("utf-8")) > 72:
        raise CommandError("La clave no puede pasar de 72 bytes (limite de BCrypt)")


class Command(BaseCommand):
    help = "Crea el administrador o promueve una cuenta existente a ADMIN (clave por PLANTIA_ADMIN_PASSWORD o por teclado)."

    def add_arguments(self, parser):
        parser.add_argument("--email", required=True)
        parser.add_argument("--nombre", default="Administrador")

    def handle(self, *args, **opciones):
        email = opciones["email"].strip().lower()
        if "@" not in email or len(email) > 150:
            raise CommandError("Correo no valido")
        if email in CUENTAS_DEMO:
            raise CommandError("Usa un correo propio: las cuentas de demostracion se deshabilitan en produccion")
        clave = os.environ.get("PLANTIA_ADMIN_PASSWORD") or ""
        if not clave:
            clave = getpass.getpass("Clave del admin: ")
            if clave != getpass.getpass("Repite la clave: "):
                raise CommandError("Las claves no coinciden")
        validar_clave(clave)
        hash_clave = bcrypt.hashpw(clave.encode("utf-8"), bcrypt.gensalt(10)).decode()

        usuario = Usuario.objects.filter(email=email).first()
        if usuario is None:
            usuario = Usuario(email=email, nombre=opciones["nombre"].strip()[:100] or "Administrador", rol="ADMIN",
                              password_hash=hash_clave, monedas=0)
            usuario.save()
            self.stdout.write(self.style.SUCCESS(f"Admin creado: {email}"))
        else:
            usuario.rol = "ADMIN"
            usuario.password_hash = hash_clave
            # Cierra sesiones abiertas con la clave anterior
            usuario.session_token = None
            usuario.admin_session_token = None
            usuario.save(update_fields=["rol", "password_hash", "session_token", "admin_session_token"])
            self.stdout.write(self.style.SUCCESS(f"{email} ahora es ADMIN (clave cambiada)"))
