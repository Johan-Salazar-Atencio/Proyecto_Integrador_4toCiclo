import { StrictMode } from 'react'
import { createRoot } from 'react-dom/client'
import { BrowserRouter } from 'react-router-dom'
import App from './App'
import { SesionProvider } from './store/Sesion'
import { SesionAdminProvider } from './store/SesionAdmin'
import { aplicarTema } from './store/preferencias'
import './styles/tema.css'
import './styles/global.css'

aplicarTema()

createRoot(document.getElementById('root')).render(
  <StrictMode>
    <BrowserRouter>
      <SesionProvider>
        <SesionAdminProvider>
          <App />
        </SesionAdminProvider>
      </SesionProvider>
    </BrowserRouter>
  </StrictMode>,
)
