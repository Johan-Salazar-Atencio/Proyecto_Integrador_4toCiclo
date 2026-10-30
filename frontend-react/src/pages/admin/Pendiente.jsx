// Pagina de relleno para las secciones del panel que se construyen en los siguientes commits
export default function Pendiente({ titulo }) {
  return (
    <div className="aparece" style={{ padding: '64px 0', textAlign: 'center', color: 'var(--texto-suave)' }}>
      <div style={{ fontSize: '3rem' }}>🛠️</div>
      <h2 style={{ color: 'var(--texto)', margin: '8px 0' }}>{titulo}</h2>
      <p>Esta sección llega pronto.</p>
    </div>
  )
}
