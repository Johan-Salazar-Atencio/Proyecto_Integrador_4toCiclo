"""Endpoints de solo lectura del catalogo y eventos (para React / enciclopedia)."""
from rest_framework import generics
from rest_framework.response import Response

from .models import EventoDesastre, PlantaCatalogo
from .serializers import EventoDesastreSerializer, PlantaCatalogoSerializer


class CatalogoListView(generics.ListAPIView):
    serializer_class = PlantaCatalogoSerializer

    def get_queryset(self):
        qs = PlantaCatalogo.objects.all().prefetch_related("cuidadosseccion_set")
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
    queryset = PlantaCatalogo.objects.all().prefetch_related("cuidadosseccion_set")
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
    def get(self, request):
        evento = EventoDesastre.objects.filter(estado="ACTIVO").order_by("-fecha_inicio").first()
        if not evento:
            return Response(status=204)
        return Response(EventoDesastreSerializer(evento).data)
