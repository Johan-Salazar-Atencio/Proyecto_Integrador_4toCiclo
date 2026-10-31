// Acceso del administrador. La autenticacion corresponde a Django (ver arquitectura).
//
// SIMULADO: Django aun no tiene un endpoint de login para la API (solo su /admin/ de sesion),
// y en la BD no hay un usuario con rol ADMIN. Mientras tanto se acepta cualquier correo con
// contrasena y se crea una sesion de prueba.
//
// TODO(django): reemplazar por
//   POST /django/api/admin/login  { email, password }  ->  { id, nombre, email, rol: "ADMIN" }
//   (200 si es ADMIN, 401 credenciales invalidas, 403 si el usuario no es ADMIN)
// y mandar el token o la cookie de sesion en cada llamada del panel.

const PAUSA_MS = 600

export async function iniciarSesionAdmin(email, password) {
  await new Promise((ok) => setTimeout(ok, PAUSA_MS))
  if (!email.trim() || !password) {
    const error = new Error('Escribe tu correo y contraseña.')
    error.estado = 401
    throw error
  }
  return { id: 0, nombre: 'Admin Tecsup', email: email.trim(), rol: 'ADMIN' }
}

// Estado del motor de IA / Django para el indicador de la barra superior
export async function estadoDjango() {
  try {
    const respuesta = await fetch('/django/api/health')
    return respuesta.ok
  } catch {
    return false
  }
}
