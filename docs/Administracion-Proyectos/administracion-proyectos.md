# Documentación Académica — Administración y Planeación de Proyectos de Software (TS683)

**Proyecto:** AetherNet IoT & Autonomous Rover
**Asignatura UTP:** TS683 — 3 créditos, 6º semestre, obligatoria (`docs/UTP/2350_TS683 Administración y Planeación de Proyectos de Software.docx (1).pdf`)
**Área del proyecto:** Área 4 — Administración y Planeación (Gestión) · ver `docs/backlog.md` §Área 4 · ítems PM-01 a PM-07

> Mapeo del contenido académico del PDF contra lo implementado. Esta área es transversal: no produce código, pero gobierna cómo se planifica, mide y controla todo lo demás.

---

## 1. Contenido académico según el PDF

| Unidad | Contenido |
|---|---|
| **U1** | Generalidades sobre proyectos y gestión de proyectos. Metodologías **Scrum, PSP y TSP**. Procesos de planificación |
| **U2** | Gestión de **riesgos**, gestión de la **configuración**, procesos de **medición y análisis** de software |
| **U3** | Gestión de **calidad** de software. Gestión de **costos**. **Monitoreo y control** del proyecto |

Evaluación del PDF: proyecto por cortes (30% + 30% + 40%) — el Proyecto Integrador AetherNet es el caso de estudio de la asignatura.

---

## 2. Mapa: tema académico → aplicación en el proyecto → área

### U1 — Metodologías y Planificación ✅ Aplicado

| Lo que se ve en el contenido | Lo que se aplica en el proyecto | Dónde está aplicado |
|---|---|---|
| **Scrum**: backlog, sprints, HU | Product Backlog completo priorizado con MoSCoW y dividido en 5 áreas de trabajo; ventana de ejecución 8 semanas / 4 sprints con dependencias explícitas | `docs/backlog.md` (estructura completa), `docs/sprints.md` (4 sprints) |
| Historias de Usuario con criterios de aceptación | 4 HU redactadas con formato BDD (*Dado/Cuando/Entonces*) — declaradas fuente de verdad para tests, no guía aproximada | `docs/requirements.md` §4 (HU-01 a HU-04); convención en `AGENTS.md` §3 |
| Procesos de planificación (descomposición) | Cada tarea del backlog referencia su RF/HU de origen, sprint objetivo e ítems de los que depende ("Depende de") — WBS implícito con grafo de dependencias | `docs/backlog.md` columnas ID/Tarea/Sprint/Depende de/Origen |
| PSP/TSP (proceso personal/equipo) | **Adaptado, no literal:** el "proceso" se formaliza en reglas para agentes de código y colaboradores: qué leer antes de trabajar, cuándo pedir confirmación humana, qué convenciones seguir | `AGENTS.md` (punto de entrada obligatorio) |
| Estimación | Planning Poker definido como mecanismo de reestimación si un sprint se atrasa >20% | `docs/backlog.md` PM-07 |

### U2 — Gestión de Riesgos ✅ Aplicado

| Lo que se ve en el contenido | Lo que se aplica en el proyecto | Dónde está aplicado |
|---|---|---|
| Identificación y matriz de riesgos | Matriz formal de riesgos del proyecto; cada riesgo con plan de mitigación | `docs/risk-register.md`; ítem PM-03 del backlog |
| Riesgos técnicos de integración | ~~Riesgo tuya-local~~ — **CANCELADO 2026-09-01** (R-01, RF-4.2 Won't). Hu-02 ahora solo Telegram + LED RGB | `docs/risk-register.md:16` R-01 |
| Riesgos de dependencia entre áreas | Cada área del backlog documenta su riesgo específico: cascada Móviles←DevOps (MOV-06 depende del enlace RF), base fundacional DevOps que bloquea todo, backlog Estadística concentrado en Sprint 4 | `docs/backlog.md` §riesgos por área |
| Riesgo de seguridad funcional | Fail-safe del Rover ante pérdida de enlace RF (HU-04) nació como respuesta a riesgo documentado | RF-3.3 ↔ `docs/architecture.md` §6 ↔ `docs/risk-register.md` |

### U2 — Gestión de Configuración ✅ Aplicado

| Lo que se ve en el contenido | Lo que se aplica en el proyecto | Dónde está aplicado |
|---|---|---|
| Control de versiones y línea base | Git como SCM; branches main/develop; CI como puerta de integración | `.github/workflows/ci.yml` |
| Trazabilidad de cambios | Convención: commits/PRs citan el ID del backlog (`MOV-05: ...`) ligándolos al requisito origen | `docs/backlog.md` §final |
| Gestión de secretos/configuración | Credenciales fuera del código: `.env.example` como plantilla sin valores reales; secrets parametrizados en Compose | `env.example`, `docker-compose.yml`, DEVOPS-08 |
| Documentación bajo control | Documentación viva versionada en el repo con advertencias de frescura (un agente que lea un sprint desactualizado trabaja mal) | `docs/sprints.md` nota inicial, PM-05 |

### U2 — Medición y Análisis ✅ Aplicado

| Lo que se ve en el contenido | Lo que se aplica en el proyecto | Dónde está aplicado |
|---|---|---|
| Métricas del producto | KPIs cuantitativos definidos ANTES de construir: latencia MQTT <50 ms, latencia RF <10 ms, reducción ruido >85%, falsos positivos láser 0%, cumplimiento FOSS 100% | `docs/prd.md` §5 |
| Indicadores verificables programáticamente | El KPI estadístico tiene función que lo calcula y retorna `meets_kpi` booleano — medición automatizada, no manual | `stats/ema_filter.py:99-123` |
| Medición del proceso | Actualización del estado real de sprints al cierre de cada uno (PM-05) + retro corta documentando desvíos (PM-06) | `docs/sprints.md` §Estado actual; `docs/backlog.md` PM-05/PM-06 |

### U3 — Gestión de Calidad ✅ Aplicado

| Lo que se ve en el contenido | Lo que se aplica en el proyecto | Dónde está aplicado |
|---|---|---|
| Plan de calidad / SQA | Plan de pruebas formal del proyecto | `docs/test-plan.md` |
| Criterios de aceptación objetivos | Los criterios BDD de las HU son la fuente de verdad de qué constituye "hecho" | `docs/requirements.md` §4 |
| Puertas de calidad automatizadas | CI ejecuta linting (ruff), chequeo de tipos (mypy), pytest backend/stats, compilación de 3 firmwares, build Docker con health check, escaneo Trivy — merge bloqueado si fallan | `.github/workflows/ci.yml` (6 jobs) |
| Requisitos no funcionales medibles | RNF-1.1/1.2 (infra), RNF-2.1/2.2 (rendimiento/estadística), RNF-3.1 (FOSS) auditables | `docs/requirements.md` §3 |

### U3 — Gestión de Costos ✅ Aplicado (adaptado)

| Lo que se ve en el contenido | Lo que se aplica en el proyecto | Dónde está aplicado |
|---|---|---|
| Presupuesto y control de costos | Inventario de hardware como base de costos materiales (compras, disponibilidad, riesgos de adquisición) | `docs/hardware-inventory.md` |
| Costos de licenciamiento | Restricción FOSS 100% (RNF-3.1) elimina costos de licencias por diseño — decisión administrativa con impacto técnico directo | `docs/prd.md` §5 (KPI FOSS), `AGENTS.md` §3 |
| Brecha honesta | **No existe** presupuesto formal con seguimiento de gasto real vs. estimado — registrado como mejora | — |

### U3 — Monitoreo y Control del Proyecto ✅ Aplicado

| Lo que se ve en el contenido | Lo que se aplica en el proyecto | Dónde está aplicado |
|---|---|---|
| Seguimiento del avance | Tablero Scrum reflejando el backlog (GitHub Projects u otro) | PM-02 (pendiente de crear) |
| Control de desviaciones | Regla: todo "Must" sin cerrar al final de su sprint escala automáticamente a la matriz de riesgos y al Gantt | `docs/backlog.md` §"Cómo se relaciona" |
| Visualización de cronograma | Diagrama de Gantt con dependencias entre sprints derivado de `sprints.md` | PM-04 (pendiente) |
| Cierre iterativo | Retro al cierre de cada sprint + replanificación con Planning Poker si desvío >20% | PM-06, PM-07 |

---

## 3. Entregables académicos de esta materia (estado)

| Entregable típico (PDF) | Evidencia en el proyecto | Estado |
|---|---|---|
| Product Backlog | `docs/backlog.md` (MoSCoW × 5 áreas × 4 sprints) | ✅ Completo |
| Sprint Planning | `docs/sprints.md` con habilitadores y dependencias | ✅ Completo |
| Matriz de riesgos | `docs/risk-register.md` | ✅ Existe (mantener al día = PM-03) |
| Gantt | Derivable de sprints.md | ⚠️ Pendiente diagramar (PM-04) |
| Tablero Scrum | GitHub Projects | ❌ Pendiente (PM-02) |
| Retros / control | Mecanismo definido (PM-05/06/07) | ⚠️ En curso continuo |

## 4. Trazabilidad

- **Ítems propios:** PM-01 a PM-07 (`docs/backlog.md` Área 4)
- **Impacto cruzado:** si PM-05 no se mantiene, cualquier agente/colaborador trabaja con información obsoleta — riesgo documentado en `docs/backlog.md` §Área 4
- **Nota metodológica:** el PDF menciona PSP/TSP y marco lógico (LFA/PMBOK); el proyecto adopta Scrum+BDD como metodología principal, usando los conceptos del resto como vocabulario complementario.
