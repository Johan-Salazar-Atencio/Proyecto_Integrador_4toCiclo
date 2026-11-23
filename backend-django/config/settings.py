"""Configuracion de Django para el backend de administracion e IA de Plantia.

Este proyecto comparte la MISMA base de datos PostgreSQL que el backend de
Spring Boot. Spring Boot es el dueno del DDL (lo aplica con Flyway), por lo que
todos los modelos de negocio de Django usan `managed = False` y no generan
migraciones.
"""
import os
from pathlib import Path

from dotenv import load_dotenv

BASE_DIR = Path(__file__).resolve().parent.parent

load_dotenv(BASE_DIR / ".env")


def env_bool(name: str, default: bool = False) -> bool:
    return os.getenv(name, str(default)).strip().lower() in {"1", "true", "yes", "on"}


def env_list(name: str, default: str = "") -> list[str]:
    raw = os.getenv(name, default)
    return [item.strip() for item in raw.split(",") if item.strip()]


# Valores por defecto seguros: para desarrollo se activan en .env (ver .env.example)
DEBUG = env_bool("DJANGO_DEBUG", False)
SECRET_KEY = os.getenv("DJANGO_SECRET_KEY", "")
if not SECRET_KEY:
    if not DEBUG:
        from django.core.exceptions import ImproperlyConfigured

        raise ImproperlyConfigured("Falta DJANGO_SECRET_KEY (o usa DJANGO_DEBUG=true en desarrollo)")
    SECRET_KEY = "dev-inseguro-solo-desarrollo"
ALLOWED_HOSTS = env_list("DJANGO_ALLOWED_HOSTS", "127.0.0.1,localhost")

INSTALLED_APPS = [
    "django.contrib.admin",
    "django.contrib.auth",
    "django.contrib.contenttypes",
    "django.contrib.sessions",
    "django.contrib.messages",
    "django.contrib.staticfiles",
    "corsheaders",
    "rest_framework",
    "catalog",
    "ia_engine",
    "admin_panel",
]

# Django NO es dueno del DDL: Spring Boot lo aplica con Flyway.
# Desactivamos las migraciones de estas apps; sus modelos usan managed=False.
MIGRATION_MODULES = {
    "catalog": None,
    "ia_engine": None,
    "admin_panel": None,
}

# QA-44: sin paginas de depuracion (con la lista de rutas) aunque DJANGO_DEBUG este activo
PAGINAS_DEBUG = env_bool("DJANGO_PAGINAS_DEBUG", False)

MIDDLEWARE = [
    "config.errores.SinPaginasDeDepuracion",
    "corsheaders.middleware.CorsMiddleware",
    "django.middleware.security.SecurityMiddleware",
    "django.contrib.sessions.middleware.SessionMiddleware",
    "django.middleware.common.CommonMiddleware",
    "django.middleware.csrf.CsrfViewMiddleware",
    "django.contrib.auth.middleware.AuthenticationMiddleware",
    "django.contrib.messages.middleware.MessageMiddleware",
    "django.middleware.clickjacking.XFrameOptionsMiddleware",
]

ROOT_URLCONF = "config.urls"

TEMPLATES = [
    {
        "BACKEND": "django.template.backends.django.DjangoTemplates",
        "DIRS": [BASE_DIR / "templates"],
        "APP_DIRS": True,
        "OPTIONS": {
            "context_processors": [
                "django.template.context_processors.request",
                "django.contrib.auth.context_processors.auth",
                "django.contrib.messages.context_processors.messages",
            ],
        },
    },
]

WSGI_APPLICATION = "config.wsgi.application"
ASGI_APPLICATION = "config.asgi.application"

DB_ENGINE = os.getenv("DB_ENGINE", "django.db.backends.postgresql")

if DB_ENGINE.endswith("sqlite3"):
    # Util para correr tests sin PostgreSQL: DB_ENGINE=sqlite3 manage.py test
    DATABASES = {
        "default": {
            "ENGINE": "django.db.backends.sqlite3",
            "NAME": os.getenv("DB_NAME", ":memory:"),
        }
    }
else:
    DATABASES = {
        "default": {
            "ENGINE": "django.db.backends.postgresql",
            "NAME": os.getenv("DB_NAME", "plantia"),
            "USER": os.getenv("DB_USER", "plantia"),
            "PASSWORD": os.getenv("DB_PASSWORD", "plantia"),
            "HOST": os.getenv("DB_HOST", "localhost"),
            "PORT": os.getenv("DB_PORT", "5432"),
            "OPTIONS": {"connect_timeout": 5},
            # Las columnas de fecha de las tablas que comparte con Spring son "timestamp sin zona" y Spring guarda y
            # compara la hora de Lima: sin esto Django escribia y leia en UTC y los eventos aparecian 5 horas tarde.
            "TIME_ZONE": "America/Lima",
        }
    }

AUTH_PASSWORD_VALIDATORS = [
    {"NAME": "django.contrib.auth.password_validation.UserAttributeSimilarityValidator"},
    {"NAME": "django.contrib.auth.password_validation.MinimumLengthValidator"},
    {"NAME": "django.contrib.auth.password_validation.CommonPasswordValidator"},
    {"NAME": "django.contrib.auth.password_validation.NumericPasswordValidator"},
]

LANGUAGE_CODE = "es-pe"
TIME_ZONE = "America/Lima"
USE_I18N = True
USE_TZ = True

STATIC_URL = "static/"
STATIC_ROOT = BASE_DIR / "staticfiles"

DEFAULT_AUTO_FIELD = "django.db.models.BigAutoField"

REST_FRAMEWORK = {
    "DEFAULT_RENDERER_CLASSES": [
        "rest_framework.renderers.JSONRenderer",
        "rest_framework.renderers.BrowsableAPIRenderer",
    ],
    "DEFAULT_PARSER_CLASSES": [
        "rest_framework.parsers.JSONParser",
        "rest_framework.parsers.MultiPartParser",
        "rest_framework.parsers.FormParser",
    ],
    "UNAUTHENTICATED_USER": None,
    # Limite de uso del motor de IA (cada llamada gasta cuota del proveedor). Spring es el unico cliente, asi que el
    # limite es de TODA la app (QA-08): 30/min cortaba el escaneo 31 de un salon entero. Spring ademas limita por
    # usuario (app.escaneo.max-por-minuto) y avisa con 429 en vez de "Planta desconocida".
    "DEFAULT_THROTTLE_CLASSES": ["rest_framework.throttling.ScopedRateThrottle"],
    "DEFAULT_THROTTLE_RATES": {"ia": os.getenv("IA_LIMITE", "120/min")},
    "EXCEPTION_HANDLER": "config.errores.manejar_excepcion",
}

# La web llama a Django a traves del proxy de Vite (mismo origen): CORS no hace falta
CORS_ALLOW_ALL_ORIGINS = env_bool("CORS_ALLOW_ALL", False)

# Clave compartida con Spring para usar el motor de IA (vacia = solo desde esta maquina)
IA_CLAVE_INTERNA = os.getenv("IA_CLAVE_INTERNA", "")

# --- Motor de IA (hibrido e intercambiable) ---
IA_PROVIDER = os.getenv("IA_PROVIDER", "auto")

# Carpeta que Spring sirve en /files/**: aqui se guardan las imagenes de los fondos que sube el admin
# (en desarrollo, la misma carpeta de Spring; en un servidor, un volumen compartido).
# Direccion desde donde se ven las imagenes que Spring sirve en /files/** (el admin las muestra desde ahi)
SPRING_PUBLIC_URL = os.getenv("SPRING_PUBLIC_URL", "http://localhost:8080").rstrip("/")
STORAGE_IMAGES_DIR = os.getenv(
    "STORAGE_IMAGES_DIR", str(BASE_DIR.parent / "backend-spring-boot" / "storage" / "images"))
OPENAI_API_KEY = os.getenv("OPENAI_API_KEY", "")
OPENAI_MODEL = os.getenv("OPENAI_MODEL", "gpt-4o")
# IA institucional local: Open WebUI (proxy de vLLM) con API compatible OpenAI.
WEBUI_BASE_URL = os.getenv("WEBUI_BASE_URL", "")
WEBUI_API_KEY = os.getenv("WEBUI_API_KEY", "")
WEBUI_MODEL = os.getenv("WEBUI_MODEL", "Qwen/Qwen3.6-35B-A3B-FP8")
GEMINI_API_KEY = os.getenv("GEMINI_API_KEY", "")
GEMINI_MODEL = os.getenv("GEMINI_MODEL", "gemini-flash-latest")
GEMINI_FALLBACK_MODELS = os.getenv("GEMINI_FALLBACK_MODELS", "")
LOCAL_IA_URL = os.getenv("LOCAL_IA_URL", "http://localhost:11434")
LOCAL_IA_MODEL = os.getenv("LOCAL_IA_MODEL", "llava")

LOGGING = {
    "version": 1,
    "disable_existing_loggers": False,
    "handlers": {"console": {"class": "logging.StreamHandler"}},
    "root": {"handlers": ["console"], "level": "INFO"},
}
