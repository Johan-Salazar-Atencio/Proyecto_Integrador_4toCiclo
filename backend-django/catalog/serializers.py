"""Serializadores del catalogo (lectura para React / enciclopedia)."""
from rest_framework import serializers

from .models import CuidadoSeccion, EventoDesastre, PlantaCatalogo


class CuidadoSeccionSerializer(serializers.ModelSerializer):
    class Meta:
        model = CuidadoSeccion
        fields = ["id_seccion", "tipo", "titulo", "contenido", "orden", "generado_por_ia"]


class PlantaCatalogoSerializer(serializers.ModelSerializer):
    cuidados = CuidadoSeccionSerializer(source="cuidadosseccion_set", many=True, read_only=True)

    class Meta:
        model = PlantaCatalogo
        fields = [
            "id_catalogo",
            "nombre_comun",
            "nombre_cientifico",
            "frecuencia_riego_dias",
            "frecuencia_sol_horas",
            "temperatura_min_c",
            "temperatura_max_c",
            "es_toxica_mascotas",
            "sprite_base_url",
            "publicada",
            "cuidados",
        ]


class EventoDesastreSerializer(serializers.ModelSerializer):
    # Nombre y color del tipo para pintar la alerta sin una lista fija en el cliente
    tipo_nombre = serializers.CharField(source="id_tipo_desastre.nombre", read_only=True, default=None)
    tipo_color = serializers.CharField(source="id_tipo_desastre.color_hex", read_only=True, default=None)

    class Meta:
        model = EventoDesastre
        fields = [
            "id_desastre",
            "tipo_evento",
            "tipo_nombre",
            "tipo_color",
            "region",
            "mensaje_notificacion",
            "severidad",
            "estado",
            "fecha_inicio",
            "fecha_fin",
        ]
