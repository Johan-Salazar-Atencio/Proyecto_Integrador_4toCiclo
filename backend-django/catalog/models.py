"""Modelos SOLO LECTURA/ESCRITURA sobre el esquema creado por Spring Boot + Flyway.

Todos usan `managed = False`: Django nunca crea ni altera estas tablas.
El DDL vive en `db/migration` (Flyway, lado Spring Boot).
"""
from django.db import models


class Region(models.Model):
    """Region a la que pertenecen usuarios y eventos (Flyway V13)."""

    id_region = models.AutoField(primary_key=True)
    codigo = models.CharField(max_length=100, unique=True)
    nombre = models.CharField(max_length=100)
    activa = models.BooleanField(default=True)

    class Meta:
        managed = False
        db_table = "regiones"
        verbose_name = "Region"
        verbose_name_plural = "Regiones"

    def __str__(self):
        return self.nombre


class TipoDesastre(models.Model):
    """Tipo de desastre que el admin puede crear (Flyway V13); reemplaza la lista fija de 4."""

    id_tipo_desastre = models.AutoField(primary_key=True)
    codigo = models.CharField(max_length=50, unique=True)
    nombre = models.CharField(max_length=100)
    descripcion = models.TextField(null=True, blank=True)
    icono_url = models.CharField(max_length=255, null=True, blank=True)
    color_hex = models.CharField(max_length=7, default="#888888")
    # Salud que pierde una planta sin refugio segun la severidad
    dano_bajo = models.IntegerField(default=10)
    dano_moderado = models.IntegerField(default=25)
    dano_critico = models.IntegerField(default=50)
    activo = models.BooleanField(default=True)
    id_creador = models.IntegerField(null=True, blank=True)
    fecha_creacion = models.DateTimeField(auto_now_add=True)

    class Meta:
        managed = False
        db_table = "tipos_desastre"
        verbose_name = "Tipo de desastre"
        verbose_name_plural = "Tipos de desastre"

    def __str__(self):
        return self.nombre


class Usuario(models.Model):
    id_usuario = models.AutoField(primary_key=True)
    nombre = models.CharField(max_length=100)
    email = models.CharField(max_length=150, unique=True)
    password_hash = models.CharField(max_length=255)
    rol = models.CharField(max_length=20, default="USER")
    nivel = models.IntegerField(default=1)
    experiencia_xp = models.IntegerField(default=0)
    monedas = models.IntegerField(default=100)
    racha_dias = models.IntegerField(default=0)
    region = models.CharField(max_length=100, default="GLOBAL")
    # Llave a regiones; si va vacia, la base la completa desde `region` (V13)
    id_region = models.ForeignKey(
        Region, on_delete=models.SET_NULL, db_column="id_region", null=True, blank=True
    )
    ultima_actividad = models.DateField(null=True, blank=True)
    foto_url = models.CharField(max_length=500, null=True, blank=True)
    # SHA-256 del token de la sesion activa (Flyway V7/V11; la comparte con Spring)
    session_token = models.CharField(max_length=64, null=True, blank=True)
    session_expira = models.DateTimeField(null=True, blank=True)
    fecha_registro = models.DateTimeField(auto_now_add=True)
    updated_at = models.DateTimeField(auto_now=True)

    class Meta:
        managed = False
        db_table = "usuarios"
        verbose_name = "Usuario"
        verbose_name_plural = "Usuarios"

    def __str__(self):
        return f"{self.nombre} ({self.email})"


class PlantaCatalogo(models.Model):
    id_catalogo = models.AutoField(primary_key=True)
    nombre_comun = models.CharField(max_length=100)
    nombre_cientifico = models.CharField(max_length=150, unique=True)
    frecuencia_riego_dias = models.IntegerField()
    frecuencia_sol_horas = models.IntegerField()
    temperatura_min_c = models.IntegerField(default=10)
    temperatura_max_c = models.IntegerField(default=30)
    es_toxica_mascotas = models.BooleanField(default=False)
    sprite_base_url = models.CharField(max_length=255, default="/assets/plantas/default.png")
    publicada = models.BooleanField(default=True)
    etapa_semilla_url = models.CharField(max_length=255, null=True, blank=True)
    etapa_brote_url = models.CharField(max_length=255, null=True, blank=True)
    etapa_adulta_url = models.CharField(max_length=255, null=True, blank=True)
    datos_ia_raw = models.JSONField(null=True, blank=True)
    fecha_creacion = models.DateTimeField(auto_now_add=True)
    updated_at = models.DateTimeField(auto_now=True)

    class Meta:
        managed = False
        db_table = "plantas_catalogo"
        verbose_name = "Planta del catalogo"
        verbose_name_plural = "Plantas del catalogo"
        ordering = ["nombre_comun"]

    def __str__(self):
        return f"{self.nombre_comun} ({self.nombre_cientifico})"


class CuidadoSeccion(models.Model):
    TIPOS = [
        ("RIEGO", "Riego"),
        ("LUZ", "Luz"),
        ("SUSTRATO", "Sustrato"),
        ("TEMPERATURA", "Temperatura"),
        ("PODA", "Poda"),
        ("PLAGAS", "Plagas"),
        ("TOXICIDAD", "Toxicidad"),
        ("ABONO", "Abono"),
        ("OTRO", "Otro"),
    ]

    id_seccion = models.AutoField(primary_key=True)
    id_catalogo = models.ForeignKey(
        PlantaCatalogo, on_delete=models.CASCADE, db_column="id_catalogo"
    )
    tipo = models.CharField(max_length=30, choices=TIPOS)
    titulo = models.CharField(max_length=100)
    contenido = models.TextField()
    orden = models.IntegerField(default=0)
    generado_por_ia = models.BooleanField(default=True)
    fecha_creacion = models.DateTimeField(auto_now_add=True)

    class Meta:
        managed = False
        db_table = "cuidados_secciones"
        verbose_name = "Seccion de cuidado"
        verbose_name_plural = "Secciones de cuidado"
        ordering = ["orden", "id_seccion"]

    def __str__(self):
        return f"[{self.tipo}] {self.titulo}"


class EscaneoHistorial(models.Model):
    id_escaneo = models.AutoField(primary_key=True)
    id_usuario = models.ForeignKey(
        Usuario, on_delete=models.CASCADE, db_column="id_usuario", null=True
    )
    id_catalogo = models.ForeignKey(
        PlantaCatalogo, on_delete=models.SET_NULL, db_column="id_catalogo", null=True, blank=True
    )
    imagen_foto_url = models.CharField(max_length=255)
    confianza_ia = models.DecimalField(max_digits=5, decimal_places=2, null=True, blank=True)
    respuesta_ia_raw = models.JSONField()
    fecha_escaneo = models.DateTimeField(auto_now_add=True)

    class Meta:
        managed = False
        db_table = "escaneos_historial"
        verbose_name = "Escaneo"
        verbose_name_plural = "Escaneos"
        ordering = ["-fecha_escaneo"]


class EventoDesastre(models.Model):
    ESTADOS = [
        ("PROGRAMADO", "Programado"),
        ("ACTIVO", "Activo"),
        ("FINALIZADO", "Finalizado"),
    ]
    SEVERIDADES = [("BAJO", "Bajo"), ("MODERADO", "Moderado"), ("CRITICO", "Critico")]

    id_desastre = models.AutoField(primary_key=True)
    id_creador = models.ForeignKey(
        Usuario, on_delete=models.SET_NULL, db_column="id_creador", null=True, blank=True
    )
    # Texto y llave conviven hasta la migracion que elimine el texto; se guardan los dos
    tipo_evento = models.CharField(max_length=50)
    id_tipo_desastre = models.ForeignKey(
        TipoDesastre, on_delete=models.PROTECT, db_column="id_tipo_desastre", null=True, blank=True
    )
    region = models.CharField(max_length=100, default="GLOBAL")
    id_region = models.ForeignKey(
        Region, on_delete=models.SET_NULL, db_column="id_region", null=True, blank=True
    )
    # True cuando Spring ya registro el resultado en las plantas
    procesado = models.BooleanField(default=False)
    mensaje_notificacion = models.TextField()
    fecha_inicio = models.DateTimeField()
    fecha_fin = models.DateTimeField()
    estado = models.CharField(max_length=20, choices=ESTADOS, default="PROGRAMADO")
    severidad = models.CharField(max_length=20, choices=SEVERIDADES, default="MODERADO")
    updated_at = models.DateTimeField(auto_now=True)

    class Meta:
        managed = False
        db_table = "eventos_desastre"
        verbose_name = "Evento de desastre"
        verbose_name_plural = "Eventos de desastre"
        ordering = ["-fecha_inicio"]

    def __str__(self):
        return f"{self.tipo_evento} / {self.region} [{self.estado}]"


class ItemTienda(models.Model):
    TIPOS = [("MACETA", "Maceta"), ("FONDO", "Fondo"), ("ABONO", "Abono")]

    id_item = models.AutoField(primary_key=True)
    nombre = models.CharField(max_length=100)
    tipo = models.CharField(max_length=30, choices=TIPOS)
    precio_monedas = models.IntegerField()
    nivel_requerido = models.IntegerField(default=1)
    asset_url = models.CharField(max_length=255)
    updated_at = models.DateTimeField(auto_now=True)

    class Meta:
        managed = False
        db_table = "items_tienda"
        verbose_name = "Item de tienda"
        verbose_name_plural = "Items de tienda"

    def __str__(self):
        return f"{self.nombre} ({self.precio_monedas} monedas)"


class Logro(models.Model):
    id_logro = models.AutoField(primary_key=True)
    nombre = models.CharField(max_length=100)
    descripcion = models.TextField()
    icono_url = models.CharField(max_length=255)
    recompensa_monedas = models.IntegerField(default=50)

    class Meta:
        managed = False
        db_table = "logros"
        verbose_name = "Logro"
        verbose_name_plural = "Logros"

    def __str__(self):
        return self.nombre
