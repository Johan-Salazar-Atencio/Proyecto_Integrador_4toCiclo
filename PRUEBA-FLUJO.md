# Prueba E2E: Escaneo → Guardar en colección → Virtualización

Rama: `feat/flujo-escaneo-guardado-virtualizacion`

## Precondiciones
- PostgreSQL `plantia` corriendo (puerto 5432).
- Django en 8000 con IA configurada (Gemini o mock).
- Spring Boot en 8080.
- Web: `npm run dev` (http://localhost:5173).
- (Móvil) app instalada con `adb reverse tcp:8080 tcp:8080`.

## Pasos (Web)
1. Abrir http://localhost:5173 → login/acceso con usuario de prueba.
2. Ir a "Escanear" → elegir/subir foto de planta → "Identificar planta".
3. Verificar que aparece la ficha con nombre científico, cuidados y diagnóstico.
4. En la ficha, verificar botones: "Digitalizar", "Guardar en colección", "Cancelar".
5. Pulsar "Guardar en colección":
   - Debe mostrar "Guardando…" mientras espera.
   - Al éxito, navega a `/coleccion`.
   - Si falla, muestra mensaje de error y permanece.
6. En `/coleccion`, verificar que la planta aparece en la lista.
   - La lista usa virtualización (`@tanstack/react-virtual`): solo se renderizan los items visibles (inspeccionar DOM: número de nodos < total).
7. Volver a escanear la misma planta → "Guardar en colección" → debe ser idempotente (no duplica).
8. Pulsar "Cancelar" en la ficha → vuelve a `/escanear`.

## Pasos (Móvil)
1. Abrir app → Escanear → capturar foto.
2. Al tener resultado, se navega al detalle.
3. Pulsar "Guardar" → llama a `POST /api/v1/coleccion` y navega a "Mi colección".
4. Verificar que la planta aparece en la grilla (LazyVerticalGrid virtualiza automáticamente).
5. Pulsar "Digitalizar" → navega al jardín virtual.

## Criterios de aceptación
- [ ] Build web (`npm run build`) pasa sin errores.
- [ ] Compilación móvil (`gradlew :app:compileDebugSources`) pasa.
- [ ] Flujo escaneo→guardar→colección funciona end-to-end.
- [ ] Virtualización activa en web (DOM acotado) y móvil (LazyGrid).
- [ ] Idempotencia: guardar la misma planta dos veces no duplica.
- [ ] Cancelar no persiste cambios.

## Smoke test backend (PowerShell)
```powershell
# 1. Escanear
$resp = curl -s -X POST http://127.0.0.1:8080/api/v1/escaneos -H "X-User-Id: 1" -F "foto=@foto.jpg" | ConvertFrom-Json
$escaneoId = $resp.idEscaneo
# 2. Guardar en coleccion
curl -s -X POST http://127.0.0.1:8080/api/v1/coleccion -H "X-User-Id: 1" -H "Content-Type: application/json" -d "{\"idEscaneo\": $escaneoId}"
# 3. Listar
curl -s http://127.0.0.1:8080/api/v1/coleccion -H "X-User-Id: 1"
```

## Notas
- El prototipo de animación vectorial (`PlantaVectorAnimada`) vive en la rama
  `feat/prototipo-animacion-vectores` y no está integrado aquí; fase posterior.