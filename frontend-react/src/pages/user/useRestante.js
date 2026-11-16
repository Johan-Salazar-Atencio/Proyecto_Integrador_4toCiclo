import { useEffect, useState } from 'react'

const dos = (n) => String(n).padStart(2, '0')

// 3725 -> "01:02:05"
export const formatoCuenta = (s) => `${dos(Math.floor(s / 3600))}:${dos(Math.floor(s / 60) % 60)}:${dos(s % 60)}`

// Cuenta regresiva en vivo a partir de los segundos que manda el servidor (null si no manda ninguno).
// Se vuelve a armar cuando el servidor manda otro valor (por ejemplo, tras abonar).
export default function useRestante(segundos) {
  const [fin, setFin] = useState(() => (segundos != null ? Date.now() + segundos * 1000 : null))
  const [ahora, setAhora] = useState(Date.now())
  useEffect(() => { setFin(segundos != null ? Date.now() + segundos * 1000 : null) }, [segundos])
  useEffect(() => {
    if (fin == null) return undefined
    const id = setInterval(() => setAhora(Date.now()), 1000)
    return () => clearInterval(id)
  }, [fin])
  return fin == null ? null : Math.max(0, Math.round((fin - ahora) / 1000))
}
