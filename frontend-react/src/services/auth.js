import { llamar } from './api'

// Devuelve { id, nombre, email }
export const iniciarSesion = (email, password) =>
  llamar('/auth/login', { metodo: 'POST', cuerpo: { email: email.trim(), password } })

export const registrarse = (nombre, email, password) =>
  llamar('/auth/registro', {
    metodo: 'POST',
    cuerpo: { nombre: nombre.trim(), email: email.trim(), password },
  })
