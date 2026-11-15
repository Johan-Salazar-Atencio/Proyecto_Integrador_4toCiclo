"""Endpoints de solo lectura del catalogo y eventos (para React / enciclopedia)."""
from django.db.models import Q
from django.utils import timezone
from rest_framework import generics
from rest_framework.response import Response

from .models import EventoDesastre, PlantaCatalogo, Region
from .serializers import EventoDesastreSerializer, PlantaCatalogoSerializer


class CatalogoListView(generics.ListAPIView):
    serializer_class = PlantaCatalogoSerializer

    def get_queryset(self):
        qs = PlantaCatalogo.objects.all().prefetch_related("cuidadoseccion_set")
        q = self.request.query_params.get("q")
        if q:
            qs = qs.filter(nombre_comun__icontains=q) | qs.filter(
                nombre_cientifico__icontains=q
            )
        toxica = self.request.query_params.get("toxica")
        if toxica is not None and toxica != "":
            qs = qs.filter(es_toxica_mascotas=toxica.strip().lower() in {"1", "true", "si"})
        return qs


class CatalogoDetailView(generics.RetrieveAPIView):
    queryset = PlantaCatalogo.objects.all().prefetch_related("cuidadoseccion_set")
    serializer_class = PlantaCatalogoSerializer


class EventoListView(generics.ListAPIView):
    serializer_class = EventoDesastreSerializer

    def get_queryset(self):
        qs = EventoDesastre.objects.all()
        estado = self.request.query_params.get("estado")
        if estado:
            qs = qs.filter(estado=estado.upper())
        return qs


class EventoActivoView(generics.GenericAPIView):
    """Evento vigente: ACTIVO y dentro de su ventana de fechas (mismo criterio que Spring).

    Con ?region=X solo cuentan los eventos GLOBAL o de esa region.
    """

    def get(self, request):
        ahora = timezone.now()
        qs = EventoDesastre.objects.filter(
            estado="ACTIVO", fecha_inicio__lte=ahora, fecha_fin__gt=ahora
        )
        region = (request.query_params.get("region") or "").strip()
        if region:
            qs = qs.filter(Q(region="GLOBAL") | Q(region=region))
        evento = qs.order_by("-fecha_inicio").first()
        if not evento:
            return Response(status=204)
        return Response(EventoDesastreSerializer(evento).data)


class RegionListView(generics.GenericAPIView):
    """Regiones activas (codigo y nombre): las mismas que el usuario elige en su perfil."""

    def get(self, request):
        regiones = Region.objects.filter(activa=True).order_by("nombre")
        return Response([{"codigo": r.codigo, "nombre": r.nombre} for r in regiones])
