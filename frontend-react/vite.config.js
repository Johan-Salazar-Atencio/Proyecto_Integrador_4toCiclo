import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'

// El proxy evita problemas de CORS: la web llama a rutas de su mismo origen y Vite las reenvia.
//   /api y /files  -> Spring Boot (usuario)
//   /django/...    -> Django (administracion); se quita el prefijo /django
const SPRING = process.env.VITE_SPRING_URL || 'http://localhost:8080'
const DJANGO = process.env.VITE_DJANGO_URL || 'http://127.0.0.1:8000'

export default defineConfig({
  plugins: [react()],
  server: {
    port: 5173,
    proxy: {
      '/api': SPRING,
      '/files': SPRING,
      '/django': { target: DJANGO, rewrite: (ruta) => ruta.replace(/^\/django/, '') },
    },
  },
})
