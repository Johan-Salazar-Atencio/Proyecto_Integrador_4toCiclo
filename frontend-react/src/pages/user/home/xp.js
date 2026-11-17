// Porcentaje de XP hacia el siguiente nivel (0-100) a partir de los datos de /usuarios/me
export function porcentajeXp(usuario) {
  if (!usuario) return 0
  const desde = usuario.experienceFromLevel || 0
  const tramo = Math.max((usuario.experienceToNextLevel || 0) - desde, 0)
  return tramo > 0 ? Math.min(100, Math.max(0, Math.round(((usuario.experience - desde) / tramo) * 100))) : 100
}

// Iniciales para el avatar cuando no hay foto: "Johan Demo" -> "JD"
export const iniciales = (nombre) =>
  (nombre || '').trim().split(/\s+/).filter(Boolean).slice(0, 2).map((p) => p[0].toUpperCase()).join('') || '?'
