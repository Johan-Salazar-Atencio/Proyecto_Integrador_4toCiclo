// Mismas reglas que AuthValidation.kt del movil
export const LARGO_MINIMO_PASSWORD = 8

const PATRON_CORREO = /^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\.[A-Za-z]{2,}$/

export const esCorreoValido = (email) => PATRON_CORREO.test(email.trim())
export const esNombreValido = (nombre) => nombre.trim().length >= 2
export const esPasswordValida = (password) => password.length >= LARGO_MINIMO_PASSWORD
export const coincidenPasswords = (password, confirmar) => password === confirmar
