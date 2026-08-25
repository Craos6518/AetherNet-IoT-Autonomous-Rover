# Roadmap por Materia — Administración y Planeación de Proyectos (TS683)

**Alcance de este roadmap:** todos los temas de gestión necesarios para planear, medir, controlar y CERRAR el proyecto con entregables académicos defendibles. Área transversal: no produce código, pero sin ella las otras áreas trabajan a ciegas.

**Estado al Aug 2026:** backlog + sprints + riesgos documentados ✅ · **falta:** tablero Scrum visual, Gantt, línea base de costos, evidencias de ceremonias.

Backlog operativo detallado: [`backlog-administracion.md`](backlog-administracion.md)

---

## Bloque 1 — Planificación Scrum aplicada

| Tema | Profundidad | Para qué | Específico del proyecto |
|---|---|---|---|
| Product Backlog: priorización MoSCoW + trazabilidad | Ya dominado → mantener | PM-01 | `docs/backlog.md` es la fuente; el trabajo es sincronizarlo con los backlogs por materia nuevos |
| Sprint Planning y Definition of Done por tipo de tarea | Formalizar | Ceremonias | DoD distinto para firmware (compila en CI + probado en banco), backend (tests+CI verde), app (CI+manual), docs (revisada) |
| Tablero visual (GitHub Projects): Backlog/En curso/Bloqueado/Hecho | Montar y operar | PM-02 | Tarjetas = IDs del backlog; automatizar con PRs que cierren tarjetas |
| WBS (Work Breakdown Structure) | Aplicar una vez | Entregable PDF | Descomponer cada sprint en paquetes verificables — los backlogs por materia ya son 90% del insumo |
| Gantt con dependencias | Diagramar | PM-04 | Insumo listo: columna "Depende de" de `docs/sprints.md`; herramienta FOSS (GanttProject/draw.io/Mermaid) |

## Bloque 2 — Gestión de riesgos

| Tema | Profundidad | Para qué | Específico |
|---|---|---|---|
| Matriz probabilidad×impacto + planes de mitigación/contingencia | Mantener viva | PM-03 | `docs/risk-register.md` existe; actualizar al cierre de cada sprint con estado real |
| Riesgos de integración hardware/software | Vigilancia activa | LOW-01, DEVOPS-14 | El conflicto de pines MEGA y la compatibilidad tuya-local son riesgos VIVOS hoy |
| Gestión de dependencias críticas entre áreas | Monitoreo semanal | Cascadas documentadas | MOV-06←RF←DEVOPS-05; EST-04←DEVOPS-12; App←broker ACL |

## Bloque 3 — Gestión de configuración

| Tema | Profundidad | Para qué |
|---|---|---|
| Convención de commits/PRs citando ID de tarea | Auditar cumplimiento | Trazabilidad exigida por AGENTS.md |
| Versionado de releases (tags v0.x por sprint) | Introducir | Demo final reproducible |
| Secretos: qué vive en .env vs secrets.h vs GitHub Secrets | Auditoría única | Verificar cero credenciales versionadas (hallazgo gateway.ino) |

## Bloque 4 — Medición y análisis (proceso, no producto)

| Tema | Profundidad | Para qué |
|---|---|---|
| Velocity: tareas cerradas/planeadas por sprint | Medir desde Sprint 1 | Reestimar sprints 3-4 con datos, no intuición |
| Burndown simple por sprint | Graficar en retro | Detección temprana de atraso >20% → dispara PM-07 (Planning Poker) |
| KPIs de producto como métricas de gestión | Tabular avance | Los 5 KPIs del PRD son también criterios de éxito del proyecto integrador |

## Bloque 5 — Gestión de calidad

| Tema | Profundidad | Para qué |
|---|---|---|
| Plan de pruebas como documento vivo | Mantener | `docs/test-plan.md` + criterios BDD de HU como oracle |
| Puertas de calidad automáticas (CI) | Gobernanza | Regla: merge sin CI verde prohibido — es decisión de gestión, no técnica |
| Revisión de requisitos ↔ implementación (matriz trazabilidad) | Generar al cierre | Evidencia académica clave: RF/HU → archivo → test → resultado |

## Bloque 6 — Costos

| Tema | Profundidad | Para qué |
|---|---|---|
| Presupuesto de hardware (plan vs real) | Formalizar | `docs/hardware-inventory.md` lista equipos; falta columna costo real y desviación |
| Costo de oportunidad FOSS vs cloud | Documentar en informe final | Justifica restricción RNF-3.1 ante evaluadores |

## Bloque 7 — Monitoreo, control y cierre

| Tema | Profundidad | Para qué |
|---|---|---|
| Sprint Review + Retro con minuta breve (qué se atrasó y por qué) | Operar cada 2 semanas | PM-05/PM-06; alimenta matriz de riesgos |
| Informe final por materia (este repo ya tiene base) | Consolidar Sprint 4 | Cada doc de `docs/materias/` se cierra con resultados reales |
| Demo day script: flujo completo HU-01→HU-04 | Ensayar | Sustentación ante docentes |

---

## Orden crítico

1. **PM-02 (tablero)** primero: sin visualización, velocity/burndown no existen.
2. Auditoría de secretos YA (riesgo reputacional/seguridad barato de cerrar).
3. Gantt y presupuesto: una sola sesión de trabajo bien invertida.
