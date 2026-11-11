# Prototipo: Animación vectorial (Monstera)

Rama: `feat/prototipo-animacion-vectores`

## Objetivo
Prototipo de animación vectorial que se parezca **>= 70%** a la planta
original (`plant-monstera.png`), sin imágenes raster, usando paths vectoriales
+ animación de balanceo. Base para la integración de "virtualización con
vectores" en Plantia.

## Archivos
- `frontend-react/src/components/PlantaVectorAnimada.jsx` + `planta-vector.css`
  Componente React/SVG animado (balanceo de hojas, crecimiento, maceta).
- `mobile-kotlin/.../ui/components/PlantaVectorAnimada.kt`
  Composable Jetpack Compose con Canvas + paths + `animateFloat` (sway).

## Características de la Monstera representadas (>=70%)
- Hojas grandes en forma de corazón con fenestraciones (agujeros) y hendiduras.
- Tallo central verde oscuro.
- Maceta terracota.
- Varias hojas a distintas alturas (joven + maduras).

## Virtualización
- Web: renderizar este componente solo para items visibles usando
  `@tanstack/react-virtual` (ver informe de investigación).
- Móvil: usar dentro de `LazyColumn`/`LazyGrid` con `key` estable.

## Accesibilidad
- Web: `prefers-reduced-motion: reduce` desactiva animaciones.
- Móvil: parámetro `animar` para desactivar (conectar a ajustes del sistema).

## Próximos pasos
- Ajustar paths para mayor fidelidad a `plant-monstera.png`.
- Conectar `salud` a datos reales (colección/escaneo).
- Evaluar Lottie/Rive para state machines avanzadas.