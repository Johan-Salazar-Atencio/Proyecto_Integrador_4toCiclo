// Iconos de trazo (los mismos de la referencia del nuevo diseño). Se pintan con el color del texto (currentColor).
const TRAZOS = {
  cam: 'M4 8h3l2-3h6l2 3h3v11H4zM12 17a3.5 3.5 0 1 0 0-7 3.5 3.5 0 0 0 0 7z',
  sun: 'M12 16a4 4 0 1 0 0-8 4 4 0 0 0 0 8zM12 2v2M12 20v2M2 12h2M20 12h2M5 5l1.5 1.5M17.5 17.5L19 19M5 19l1.5-1.5M17.5 6.5L19 5',
  leaf: 'M5 19c0-8 5-14 14-14 0 9-6 14-14 14zM5 19l7-7',
  target: 'M12 21a9 9 0 1 0 0-18 9 9 0 0 0 0 18zM12 16a4 4 0 1 0 0-8 4 4 0 0 0 0 8z',
  search: 'M11 18a7 7 0 1 0 0-14 7 7 0 0 0 0 14zM20 20l-4-4',
  check: 'M5 12l5 5 9-10',
  chest: 'M4 10h16v10H4zM4 10a8 3.5 0 0 1 16 0M12 13v3',
  plus: 'M12 5v14M5 12h14',
  right: 'M9 6l6 6-6 6',
  drop: 'M12 3c4 5 6 8 6 11a6 6 0 0 1-12 0c0-3 2-6 6-11z',
  bag: 'M6 8h12l-1 12H7zM9 8a3 3 0 0 1 6 0',
  clock: 'M12 21a9 9 0 1 0 0-18 9 9 0 0 0 0 18zM12 7v5l3 2',
  warn: 'M12 4l9 16H3zM12 10v4M12 17.5v.01',
  close: 'M6 6l12 12M18 6L6 18',
}

export default function Icono({ nombre, tam = 20, className = '' }) {
  return (
    <svg className={`icono ${className}`} width={tam} height={tam} viewBox="0 0 24 24" fill="none" stroke="currentColor"
      strokeWidth="2" strokeLinecap="round" strokeLinejoin="round" aria-hidden="true" style={{ flex: 'none' }}>
      <path d={TRAZOS[nombre]} />
    </svg>
  )
}
