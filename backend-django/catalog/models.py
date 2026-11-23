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
    """Tipo de desastre que el admin puede crear (Flyway V13); reemplaza la lista fija de 4.
    `descripcion` es el texto informativo de la tarjeta del evento."""

    id_tipo_desastre = models.AutoField(primary_key=True)
    codigo = models.CharField(max_length=50, unique=True)
    nombre = models.CharField(max_length=100)
    descripcion = models.TextField(null=True, blank=True)
    icono_url = models.CharField(max_length=255, null=True, blank=True)
    color_hex = models.CharField(max_length=7, default="#888888")
    # Ya no se usan desde V23: ahora importa la salud con la que queda la planta (salud_final_*)
    dano_bajo = models.IntegerField(default=10)
    dano_moderado = models.IntegerField(default=25)
    dano_critico = models.IntegerField(default=50)
    # Tarjeta del evento en el Inicio: imagen, y un consejo de campo que se muestra al prevenir
    imagen_url = models.CharField(max_length=255, null=True, blank=True)
    consejo = models.CharField(max_length=250, null=True, blank=True)
    # Salud con la que queda una planta que no se previno, segun la severidad (Flyway V23)
    salud_final_bajo = models.IntegerField(default=40)
    salud_final_moderado = models.IntegerField(default=20)
    salud_final_critico = models.IntegerField(default=10)
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
    # Region del usuario (tabla regiones); si va vacia, la base la deja en GLOBAL (V25)
    id_region = models.ForeignKey(
        Region, on_delete=models.SET_NULL, db_column="id_region", null=True, blank=True
    )
    ultima_actividad = models.DateField(null=True, blank=True)
    foto_url = models.CharField(max_length=500, null=True, blank=True)
    # SHA-256 del token de la sesion activa (Flyway V7/V11; la comparte con Spring)
    session_token = models.CharField(max_length=64, null=True, blank=True)
    # Sesion del panel admin, aparte de la de la app (V31, QA-19)
    admin_session_token = models.CharField(max_length=64, null=True, blank=True)
    admin_session_expira = models.DateTimeField(null=True, blank=True)
    session_expira = models.DateTimeField(null=True, blank=True)
    fecha_registro = models.DateTimeField(auto_now_add=True)
    updated_at = models.DateTimeField(auto_now=True)

    class Meta:
        managed = False
        db_table = "usuarios"
        verbose_name = "Usuario"
        verbose_name_plural = "Usuarios"

    @property
    def region(self):
        """Codigo de su region (GLOBAL si no tiene)."""
        return self.id_region.codigo if self.id_region_id else "GLOBAL"

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
    # Tipo y zona del evento: solo las llaves (V25 quito las columnas de texto)
    id_tipo_desastre = models.ForeignKey(
        TipoDesastre, on_delete=models.PROTECT, db_column="id_tipo_desastre", null=True, blank=True
    )
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

    @property
    def tipo_evento(self):
        """Codigo del tipo (HUAYCO, HELADA...); vacio si el evento no tiene tipo."""
        return self.id_tipo_desastre.codigo if self.id_tipo_desastre_id else ""

    @property
    def region(self):
        """Codigo de la region a la que afecta (GLOBAL si no tiene)."""
        return self.id_region.codigo if self.id_region_id else "GLOBAL"

    def __str__(self):
        return f"{self.tipo_evento} / {self.region} [{self.estado}]"


class ItemTienda(models.Model):
    TIPOS = [("MACETA", "Maceta"), ("FONDO", "Fondo"), ("ABONO", "Abono"), ("INVERNADERO", "Invernadero")]

    id_item = models.AutoField(primary_key=True)
    nombre = models.CharField(max_length=100)
    tipo = models.CharField(max_length=30, choices=TIPOS)
    precio_monedas = models.IntegerField()
    nivel_requerido = models.IntegerField(default=1)
    asset_url = models.CharField(max_length=255)
    id_fondo = models.IntegerField(null=True, blank=True)
    id_tema = models.IntegerField(null=True, blank=True)
    descripcion = models.CharField(max_length=200, null=True, blank=True)
    activo = models.BooleanField(default=True)
    updated_at = models.DateTimeField(auto_now=True)

    class Meta:
        managed = False
        db_table = "items_tienda"
        verbose_name = "Item de tienda"
        verbose_name_plural = "Items de tienda"

    def __str__(self):
        return f"{self.nombre} ({self.precio_monedas} monedas)"


class TemaFondo(models.Model):
    """Tema de fondo del jardin: trae una escena para el dia, otra para la tarde y otra para la noche."""

    id_tema = models.AutoField(primary_key=True)
    nombre = models.CharField(max_length=100)
    descripcion = models.CharField(max_length=200, null=True, blank=True)
    es_predeterminado = models.BooleanField(default=False)
    activo = models.BooleanField(default=True)

    class Meta:
        managed = False
        db_table = "temas_fondo"

    def __str__(self):
        return self.nombre


class FondoEscena(models.Model):
    """Una escena animada (tira de cuadros) de un tema, para un momento del dia."""

    id_fondo = models.AutoField(primary_key=True)
    momento = models.CharField(max_length=10)
    nombre = models.CharField(max_length=100)
    imagen_url = models.CharField(max_length=255)
    vista_url = models.CharField(max_length=255, null=True, blank=True)
    total_frames = models.IntegerField(default=1)
    ancho_px = models.IntegerField(null=True, blank=True)
    alto_px = models.IntegerField(null=True, blank=True)
    ms_por_frame = models.IntegerField(default=200)
    es_predeterminado = models.BooleanField(default=False)
    activo = models.BooleanField(default=True)
    id_tema = models.IntegerField(null=True, blank=True)

    class Meta:
        managed = False
        db_table = "fondos_escena"

    def __str__(self):
        return f"{self.nombre} ({self.momento})"


class Logro(models.Model):
    """Logro configurable (Flyway V29, el esquema es de Spring). El admin lo edita o lo desactiva
    (activo=False); nunca se borra para conservar el progreso de los usuarios."""

    RAREZAS = ["COMUN", "RARA", "EPICA", "LEGENDARIA"]
    # condicion -> (explicacion en español, requiere parametro). ESCANEO_HORA lo exige; en
    # DESASTRE_AFRONTADO es opcional (vacio = cualquier desastre); en las demas esta prohibido.
    CONDICIONES = {
        "ESCANEOS_TOTAL": "Plantas escaneadas en total",
        "ESPECIES_DISTINTAS": "Especies distintas escaneadas",
        "DESASTRE_AFRONTADO": "Desastres afrontados (de un tipo o de cualquiera)",
        "DESASTRES_TIPOS_DISTINTOS": "Tipos de desastre distintos afrontados",
        "PLANTAS_REFUGIADAS": "Plantas refugiadas antes de un evento",
        "EVENTOS_SIN_PERDIDAS": "Eventos terminados sin perder ninguna planta",
        "RIEGOS_TOTAL": "Riegos realizados en total",
        "ABONOS_TOTAL": "Abonos realizados en total",
        "LUZ_TOTAL": "Veces que se dio luz a una planta",
        "RACHA_DIAS": "Dias seguidos de racha",
        "NIVEL": "Nivel alcanzado",
        "COMPRAS_TIENDA": "Compras hechas en la tienda",
        "MACETAS_COMPRADAS": "Macetas compradas",
        "INVERNADEROS_COMPRADOS": "Invernaderos comprados",
        "FONDOS_COMPRADOS": "Fondos de escena comprados",
        "MISIONES_COMPLETADAS": "Misiones completadas",
        "ESCANEO_HORA": "Escaneos hechos en una franja horaria",
    }
    PARAMETRO_OBLIGATORIO = "ESCANEO_HORA"
    PARAMETRO_OPCIONAL = "DESASTRE_AFRONTADO"
    FRANJAS_HORARIAS = ["MADRUGADA", "NOCHE"]

    id_logro = models.AutoField(primary_key=True)
    codigo = models.CharField(max_length=50, unique=True)
    titulo = models.CharField(max_length=80)
    descripcion = models.CharField(max_length=250)
    icono = models.CharField(max_length=40)
    rareza = models.CharField(max_length=15, default="COMUN")
    condicion = models.CharField(max_length=40)
    meta = models.IntegerField(default=1)
    parametro = models.CharField(max_length=60, null=True, blank=True)
    recompensa_xp = models.IntegerField(default=0)
    recompensa_monedas = models.IntegerField(default=0)
    secreto = models.BooleanField(default=False)
    activo = models.BooleanField(default=True)
    orden = models.IntegerField(default=0)

    class Meta:
        managed = False
        db_table = "logros"
        verbose_name = "Logro"
        verbose_name_plural = "Logros"
        ordering = ["orden", "id_logro"]

    def __str__(self):
        return self.titulo

    def desbloqueos(self):
        """Usuarios que ya lo tienen desbloqueado."""
        return LogroUsuario.objects.filter(id_logro=self.pk, desbloqueado_en__isnull=False).count()


class LogroUsuario(models.Model):
    """Progreso de un usuario en un logro (Flyway V29). Solo lectura: lo escribe Spring."""

    id_logro_usuario = models.AutoField(primary_key=True)
    id_usuario = models.IntegerField()
    id_logro = models.IntegerField()
    progreso = models.IntegerField(default=0)
    desbloqueado_en = models.DateTimeField(null=True, blank=True)
    notificado = models.BooleanField(default=False)

    class Meta:
        managed = False
        db_table = "logros_usuario"
        verbose_name = "Logro de usuario"
        verbose_name_plural = "Logros de usuarios"

    def __str__(self):
        return f"usuario {self.id_usuario} / logro {self.id_logro}"


class Mision(models.Model):
    """Mision diaria que ve cada usuario. Las predeterminadas vienen de Flyway V16; el admin las
    edita, las desactiva (activa=False) o crea mas."""

    # CUIDADO cuenta cualquier cuidado (riego, luz o abono); REFUGIO es "prevenir el evento" (V23)
    TIPOS = ["RIEGO", "ESCANEO", "SOL", "ABONO", "COLECCION", "REFUGIO", "DIGITALIZAR", "CUIDADO"]

    id_mision = models.AutoField(primary_key=True)
    titulo = models.CharField(max_length=100)
    descripcion = models.TextField(blank=True, null=True)
    tipo = models.CharField(max_length=30)
    meta = models.IntegerField()
    recompensa_monedas = models.IntegerField(default=0)
    recompensa_xp = models.IntegerField(default=0)
    activa = models.BooleanField(default=True)
    predeterminada = models.BooleanField(default=False)
    orden = models.IntegerField(default=0)

    class Meta:
        managed = False
        db_table = "misiones"
        verbose_name = "Mision"
        verbose_name_plural = "Misiones"

    def __str__(self):
        return self.titulo


class ParametroJuego(models.Model):
    """Valor del juego que el admin ajusta sin tocar el codigo (Flyway V23): costo de revivir, horas de
    recuperacion, etc. Spring los lee al momento."""

    clave = models.CharField(max_length=50, primary_key=True)
    valor = models.IntegerField()
    descripcion = models.CharField(max_length=200, null=True, blank=True)

    class Meta:
        managed = False
        db_table = "parametros_juego"
        verbose_name = "Parametro de juego"
        verbose_name_plural = "Parametros de juego"

    def __str__(self):
        return f"{self.clave} = {self.valor}"


class ReglaProgreso(models.Model):
    """Hojas (monedas) y XP que paga cada accion del juego (Flyway V15 y V23). El motivo identifica la
    accion y no se cambia; el admin ajusta lo que paga, su tope diario y si esta activa."""

    id_regla = models.AutoField(primary_key=True)
    motivo = models.CharField(max_length=50, unique=True)
    xp = models.IntegerField(default=0)
    monedas = models.IntegerField(default=0)
    tope_diario = models.IntegerField(null=True, blank=True)
    activa = models.BooleanField(default=True)

    class Meta:
        managed = False
        db_table = "reglas_progreso"
        verbose_name = "Regla de progreso"
        verbose_name_plural = "Reglas de progreso"

    def __str__(self):
        return f"{self.motivo}: {self.xp} XP, {self.monedas} hojas"


class PlantaUsuario(models.Model):
    """Planta digital de un usuario (solo los campos que lee el panel admin)."""

    id_planta_usuario = models.AutoField(primary_key=True)
    id_usuario = models.ForeignKey(Usuario, on_delete=models.CASCADE, db_column="id_usuario")
    apodo = models.CharField(max_length=50, null=True, blank=True)
    etapa_crecimiento = models.CharField(max_length=20, default="SEMILLA")
    esta_refugiada = models.BooleanField(default=False)
    refugiada_desde = models.DateTimeField(null=True, blank=True)

    class Meta:
        managed = False
        db_table = "plantas_usuario"
        verbose_name = "Planta de usuario"
        verbose_name_plural = "Plantas de usuarios"

    def __str__(self):
        return self.apodo or f"Planta {self.id_planta_usuario}"


class RegistroDesastreUsuario(models.Model):
    """Resultado de un desastre para una planta (lo escribe Spring al terminar el evento)."""

    id_registro = models.AutoField(primary_key=True)
    # PROTECT como el RESTRICT de V31 (QA-21): borrar un evento no se lleva los resultados de los usuarios
    id_desastre = models.ForeignKey(EventoDesastre, on_delete=models.PROTECT, db_column="id_desastre")
    id_planta_usuario = models.ForeignKey(PlantaUsuario, on_delete=models.CASCADE, db_column="id_planta_usuario")
    estado_final = models.CharField(max_length=30)
    dano_salud_aplicado = models.IntegerField(default=0)
    fecha_procesado = models.DateTimeField(null=True, blank=True)
    estaba_refugiada = models.BooleanField(default=False)
    xp_otorgado = models.IntegerField(default=0)

    class Meta:
        managed = False
        db_table = "registros_desastre_usuario"
        verbose_name = "Registro de desastre"
        verbose_name_plural = "Registros de desastre"

    def __str__(self):
        return f"{self.id_desastre_id} / {self.id_planta_usuario_id}: {self.estado_final}"
