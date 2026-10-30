import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'

// El proxy evita problemas de CORS: la web llama a /api y /files en su mismo origen
// y Vite lo reenvia a Spring Boot.
const SPRING = process.env.VITE_SPRING_URL || 'http://localhost:8080'

export default defineConfig({
  plugins: [react()],
  server: {
    port: 5173,
    proxy: {
      '/api': SPRING,
      '/files': SPRING,
    },
  },
})
