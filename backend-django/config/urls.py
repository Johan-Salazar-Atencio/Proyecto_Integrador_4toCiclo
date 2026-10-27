"""Rutas raiz del backend de administracion e IA de Plantia."""
from django.contrib import admin
from django.http import JsonResponse
from django.urls import include, path


def health(_request):
    return JsonResponse({"status": "ok", "servicio": "plantia-ia-admin"})


urlpatterns = [
    path("admin/", admin.site.urls),
    path("api/health", health),
    path("api/ia/", include("ia_engine.urls")),
    path("api/catalog/", include("catalog.urls")),
]
