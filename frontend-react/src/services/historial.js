import { llamar } from './api'

// GET /escaneos/recientes -> [{ id, plantName, confidence ("98%"), statusColor ("#10B981") }]
export const escaneosRecientes = () => llamar('/escaneos/recientes').then((r) => r || [])
