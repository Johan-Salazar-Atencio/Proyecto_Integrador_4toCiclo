// DATOS SIMULADOS de logros. Solo se usan si se arranca con VITE_MOCK_LOGROS=1 (ver services/logros.js y
// services/admin/logros.js). Para quitarlos: borrar este archivo y las lineas "MOCK" de esos servicios.

const ESPERA = (ms = 250) => new Promise((r) => setTimeout(r, ms))

export const OPCIONES = {
  condiciones: [
    { codigo: 'PLANTAS_DIGITALIZADAS', texto: 'Digitalizar plantas', explicacion: 'Cuenta las plantas que el usuario digitaliza.', requiere_parametro: false, valores_permitidos: [] },
    { codigo: 'EVENTOS_PREVENIDOS', texto: 'Prevenir eventos', explicacion: 'Cuenta los eventos climáticos prevenidos.', requiere_parametro: false, valores_permitidos: [] },
    { codigo: 'EVENTO_TIPO_PREVENIDO', texto: 'Prevenir un tipo de evento', explicacion: 'Cuenta los eventos prevenidos de un tipo concreto.', requiere_parametro: true, valores_permitidos: ['HUAYCO', 'HELADA', 'SEQUIA'] },
    { codigo: 'RACHA_DIAS', texto: 'Racha de días', explicacion: 'Días seguidos cuidando las plantas.', requiere_parametro: false, valores_permitidos: [] },
  ],
  rarezas: ['COMUN', 'RARA', 'EPICA', 'LEGENDARIA'],
  iconos: ['semilla', 'hoja', 'huayco', 'copo', 'sol', 'escudo', 'racha', 'nivel', 'secreto'],
}

let logros = [
  { id_logro: 1, codigo: 'PRIMERA_SEMILLA', titulo: 'Primera semilla', descripcion: 'Digitaliza tu primera planta.', icono: 'semilla', rareza: 'COMUN', condicion: 'PLANTAS_DIGITALIZADAS', meta: 1, parametro: null, recompensa_xp: 20, recompensa_monedas: 10, secreto: false, activo: true, orden: 1, desbloqueos: 12 },
  { id_logro: 2, codigo: 'JARDIN_FELIZ', titulo: 'Jardín feliz', descripcion: 'Ten 5 plantas digitalizadas.', icono: 'hoja', rareza: 'RARA', condicion: 'PLANTAS_DIGITALIZADAS', meta: 5, parametro: null, recompensa_xp: 60, recompensa_monedas: 30, secreto: false, activo: true, orden: 2, desbloqueos: 5 },
  { id_logro: 3, codigo: 'FRENTE_AL_HUAYCO', titulo: 'Frente al huayco', descripcion: 'Previene 3 huaycos.', icono: 'huayco', rareza: 'EPICA', condicion: 'EVENTO_TIPO_PREVENIDO', meta: 3, parametro: 'HUAYCO', recompensa_xp: 150, recompensa_monedas: 80, secreto: false, activo: true, orden: 3, desbloqueos: 2 },
  { id_logro: 4, codigo: 'MAESTRO_JARDINERO', titulo: 'Maestro jardinero', descripcion: 'Mantén una racha de 30 días.', icono: 'racha', rareza: 'LEGENDARIA', condicion: 'RACHA_DIAS', meta: 30, parametro: null, recompensa_xp: 500, recompensa_monedas: 300, secreto: false, activo: true, orden: 4, desbloqueos: 0 },
  { id_logro: 5, codigo: 'LUNA_LLENA', titulo: 'Luna llena', descripcion: 'Un logro secreto.', icono: 'secreto', rareza: 'EPICA', condicion: 'RACHA_DIAS', meta: 7, parametro: null, recompensa_xp: 100, recompensa_monedas: 50, secreto: true, activo: false, orden: 5, desbloqueos: 1 },
]

export async function listarLogrosMock() { await ESPERA(); return logros.map((l) => ({ ...l })) }
export async function opcionesLogrosMock() { await ESPERA(100); return OPCIONES }
export async function crearLogroMock(d) {
  await ESPERA()
  if (logros.some((l) => l.codigo === d.codigo)) throw new Error('codigo: Ya existe un logro con ese código.')
  const nuevo = { ...d, id_logro: Math.max(0, ...logros.map((l) => l.id_logro)) + 1, desbloqueos: 0 }
  logros = [...logros, nuevo]
  return { ...nuevo, advertencia: d.meta > 100 ? 'La meta es muy alta; casi nadie lo desbloqueará.' : undefined }
}
export async function editarLogroMock(id, cambios) {
  await ESPERA()
  logros = logros.map((l) => (l.id_logro === id ? { ...l, ...cambios } : l))
  return logros.find((l) => l.id_logro === id)
}

const ahora = Date.now()
const dia = 86400000
const fila = (idLogro, codigo, titulo, descripcion, icono, rareza, meta, progreso, extra = {}) => ({
  idLogro, codigo, titulo, descripcion, icono, rareza, meta, progreso,
  desbloqueado: false, desbloqueadoEn: null, secreto: false, recompensaXp: 20, recompensaMonedas: 10, ...extra,
})
const hecho = (dias) => ({ desbloqueado: true, desbloqueadoEn: new Date(ahora - dias * dia).toISOString() })

export const MOCK_USUARIO = {
  total: 8,
  desbloqueados: 3,
  logros: [
    fila(1, 'PRIMERA_SEMILLA', 'Primera semilla', 'Digitaliza tu primera planta.', 'semilla', 'COMUN', 1, 1, hecho(5)),
    fila(2, 'JARDIN_FELIZ', 'Jardín feliz', 'Ten 5 plantas digitalizadas.', 'hoja', 'RARA', 5, 5, { ...hecho(2), recompensaXp: 60, recompensaMonedas: 30 }),
    fila(3, 'FRENTE_AL_HUAYCO', 'Frente al huayco', 'Previene 3 huaycos.', 'huayco', 'EPICA', 3, 3, { ...hecho(1), recompensaXp: 150, recompensaMonedas: 80 }),
    fila(4, 'MAESTRO_JARDINERO', 'Maestro jardinero', 'Mantén una racha de 30 días.', 'racha', 'LEGENDARIA', 30, 9, { recompensaXp: 500, recompensaMonedas: 300 }),
    fila(5, 'LUNA_LLENA', '???', 'Logro secreto', 'secreto', 'EPICA', 7, 0, { secreto: true, recompensaXp: 0, recompensaMonedas: 0 }),
    fila(6, 'AMANECER', 'Madrugador', 'Cuida una planta al amanecer.', 'amanecer', 'COMUN', 1, 0),
    fila(7, 'NUEVO', 'Clave nueva', 'Con un ícono que la web no conoce.', 'inventado', 'RARA', 10, 4, { recompensaXp: 40, recompensaMonedas: 20 }),
    fila(8, 'TIENDERO', 'Cliente fiel', 'Compra 3 temas en la tienda.', 'tienda', 'RARA', 3, 1, { recompensaXp: 40, recompensaMonedas: 20 }),
  ],
}
export const MOCK_PENDIENTES = [
  { idLogro: 3, codigo: 'FRENTE_AL_HUAYCO', titulo: 'Frente al huayco', descripcion: 'Previene 3 huaycos.', icono: 'huayco', rareza: 'EPICA', recompensaXp: 150, recompensaMonedas: 80 },
]
export async function usuarioMock() { await ESPERA(); return MOCK_USUARIO }
export async function pendientesMock() { await ESPERA(100); return MOCK_PENDIENTES }
