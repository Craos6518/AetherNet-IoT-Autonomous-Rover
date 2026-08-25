# Backlog por Materia — Administración y Planeación de Proyectos

Backlog operativo detallado del Área 4. IDs PM-01..07 provienen de `docs/backlog.md`; nuevos continúan la serie (PM-08+). Tareas de gestión: el "hecho" es evidencia documental verificable, no código.

---

## Pre-Sprint / Sprint 1

### PM-01 — Product Backlog consolidado ✅ HECHO (mantener sincronizado)
| M · Origen entregable PDF |
**Hecho:** `docs/backlog.md` con MoSCoW × 5 áreas + criterios BDD en `requirements.md`.
**Mantenimiento exacto:** al crear los backlogs por materia (`docs/materias/backlog-*.md`), agregar en `docs/backlog.md` un encabezado que los declare extensión oficial por materia y regla de precedencia (el específico gana sobre el general si chocan).

**Criterios:**
- [ ] Referencia cruzada bidireccional entre backlog general y backlogs por materia

### PM-02 — Tablero Scrum visual
| M · Sprint 1 · Depende de PM-01 |
**Qué hacer exactamente:**
1. Crear GitHub Project (tablero kanban) en el repo: columnas Backlog / Este sprint / En curso / Bloqueado (con motivo) / Hecho.
2. Crear tarjetas por cada tarea abierta de los backlogs por materia, tituladas `ID — título corto` (ej. `DEVOPS-06 — Endpoints FastAPI`).
3. Automatización mínima: PR abierto mueve tarjeta a En curso; merge a main la mueve a Hecho.

**Alcance IN:** solo tareas del proyecto integrador.
**Criterios:**
- [ ] URL del tablero agregada a `docs/sprints.md` §Estado actual
- [ ] ≥90% de tareas abiertas tienen tarjeta

### PM-03 — Matriz de riesgos viva ✅ EXISTE (actualizar)
| M · Continuo · Origen entregable PDF |
**Qué hacer exactamente ahora:** incorporar a `docs/risk-register.md` los riesgos NUEVOS encontrados en esta revisión técnica: (a) CI silenciosamente roto por ausencia de `main.py` (prob. alta/impacto alto hasta cerrar DEVOPS-06); (b) conflicto pines MEGA relés↔LED (ya detectado, sin mitigación asignada); (c) credenciales versionadas en gateway.ino; (d) job Android desactivado oculta fallos de compilación de la app.
**Criterios:**
- [ ] Cada riesgo nuevo tiene probabilidad, impacto, mitigación y responsable
- [ ] Revisión documentada al cierre de cada sprint (fecha + cambios)

### PM-04 — Diagrama de Gantt
| M · Sprint 1 · Depende de PM-01 |
**Qué hacer exactamente:** generar `docs/gantt.md` con diagrama Mermaid Gantt usando las fechas reales (inicio Aug 2026, 4 sprints × 2 semanas) y SOLO las dependencias reales de `docs/sprints.md` ("Depende de"); exportar PNG para el entregable académico.
**Criterios:**
- [ ] Mermaid renderiza en GitHub (preview del propio repo)
- [ ] Incluye hitos: fin Sprint N + demo final

### PM-08 — Auditoría de secretos *(nuevo)*
| M · Sprint 1 · Sin dependencias · Origen seguridad |
**Qué hacer exactamente:** ejecutar y registrar hallazgos: `git log -p | grep -iE "password|secret|key"` sobre historial completo; verificar `.gitignore` cubre `.env`, `secrets.h`; confirmar que tras DEVOPS-11 no queda credencial real versionada. Si algo sensible ya se subió: rotar contraseñas Wi-Fi/broker.
**Criterios:**
- [ ] Minuta de auditoría (qué se buscó, qué se encontró, acción) en `docs/risk-register.md`

## Continuo (cada sprint)

### PM-05 — Estado de sprints actualizado
| M · Al cierre de cada sprint |
**Qué hacer exactamente:** editar §Estado actual de `docs/sprints.md`: sprint completado, % tareas Must cerradas (del tablero PM-02), desvíos. Es la señal que usan agentes y compañeros para no trabajar con info vieja.
**Criterios:**
- [ ] Actualizado máximo 2 días después del cierre del sprint

### PM-06 — Retrospectiva con minuta
| S · Al cierre de cada sprint |
**Formato mínimo:** 3 columnas (Bien / Mal / Cambiar) + decisión concreta por ítem "Mal". Guardar en `docs/retros/sprint-N.md`. Alimenta PM-03 y PM-05.

### PM-07 — Planning Poker de contingencia
| C · Solo si desvío >20% |
**Disparador medible:** al cierre del sprint, si Must cerradas <80% de planeadas → sesión de reestimación de los sprints restantes usando velocity real; resultado: recorte C/W o extensión justificada documentada en sprints.md.

## Sprint 4 — Cierre y entregables

### PM-09 — Matriz de trazabilidad final *(nuevo)*
| M · Sprint 4 · Origen calidad |
**Qué hacer exactamente:** generar `docs/trazabilidad-final.md`: tabla RF/HU → tarea backlog → archivo(s) → test/evidencia → estado (cumple/no cumple). Fuente de verdad para el informe y la sustentación; los docs por materia ya contienen el mapeo conceptual — aquí va la verificación puntual.
**Criterios:**
- [ ] 100% de RF/RNF/HU tienen fila con evidencia o declaración explícita de incumplimiento

### PM-10 — Presupuesto hardware plan vs real *(nuevo)*
| S · Sprint 4 |
**Qué hacer exactamente:** agregar tabla costo estimado/costo real por componente en `docs/hardware-inventory.md` + desviación total; párrafo de análisis breve (¿por qué la desviación?).
**Criterios:**
- [ ] Todo componente comprado tiene precio real registrado

### PM-11 — Paquete de entrega académica *(nuevo)*
| M · Sprint 4 · Depende de PM-09 |
**Qué hacer exactamente:** tag git `v1.0.0-demo` + release GitHub con APK debug adjunto; checklist de demo script por HU (orden: HU-01 acceso → HU-03 telemetría filtrada → HU-02 intrusión → HU-04 fail-safe); PDF consolidado por materia generado desde `docs/materias/`.
**Criterios:**
- [ ] Release descargable con artefactos
- [ ] Ensayo de demo completo <15 min grabado o ensayado
