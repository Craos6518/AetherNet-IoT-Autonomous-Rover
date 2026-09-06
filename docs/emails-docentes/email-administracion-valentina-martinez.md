# Email — Administración TS683 (Valentina Martínez) — Versión final

**Asunto:** Propuesta y avances del componente de gestión (AetherNet) — TS683 Gr. 401

**Repositorio:** https://github.com/Craos6518/AetherNet-IoT-Autonomous-Rover

---

Estimada profesora Valentina:

Atendiendo a su orientación previa para el proyecto integrador, estructuré la propuesta formal del componente de gestión de **AetherNet IoT & Autonomous Rover** — proyecto transversal a las 5 áreas técnicas — y avancé en los artefactos de planificación, medición y control previstos para los tres cortes de la asignatura.

**Alineación con el syllabus TS683:**

* **U1 (Metodologías y Planificación — Scrum, PSP/TSP, BDD):** Product Backlog MoSCoW por 5 áreas, ventana 8 semanas / 4 Sprints con dependencias y grafo habilitador, 4 Historias de Usuario BDD (Dado/Cuando/Entonces) como fuente de verdad para tests.
* **U2 (Riesgos, Configuración, Medición y Análisis):** Matriz formal de riesgos con mitigación y dueño, gestión de configuración vía Git + GitHub Actions + `arduino-cli` (quality gate), KPIs de producto definidos antes de construir (latencia, ruido, falsos positivos, FOSS%).
* **U3 (Calidad, Costos, Monitoreo y Control):** Plan SQA y criterios BDD, pipeline CI con 6 jobs como puerta de calidad, inventario de hardware como base de costos y restricción FOSS 100% (sin licencias pagas), mecanismo de seguimiento (PM-05), retros (PM-06) y reestimación por Planning Poker (PM-07).

**Estructura de cortes ↔ sprints del proyecto:**

| Corte TS683 | Entregable de gestión | Sprint AetherNet | Evidencia |
|---|---|---|---|
| Corte 1 (30%) — Planificación | Backlog MoSCoW + Matriz de Riesgos + Sprints con DoD | Sprint 1: Infraestructura (Docker + CI/CD + RF base) | PM-01..04, DEVOPS-01..05, `docs/sprints.md` §1 |
| Corte 2 (30%) — Seguimiento | Sprints 2-3 ejecutados + Retros + Burndown | Sprint 2: Cerrojo + App base<br>Sprint 3: Rover + Telemetría | MOV-02..06, LOW-02, RF-3.1/3.2 |
| Corte 3 (40%) — Entrega final | Integración E2E + Demo + Lecciones aprendidas + Métricas vs KPIs | Sprint 4: EMA firmware + t-Student + Flujo intrusión (Telegram + LED RGB) | EST-02/05/07, LOW-03/05 |

**Entregables específicos propuestos:**

| Aplicación | Artefacto / Método | Resultado esperado |
|---|---|---|
| Planificación Scrum | Backlog MoSCoW + Sprints + Historias BDD (INVEST) | Trazabilidad HU ↔ RF ↔ Sprint ↔ archivo, verificable en commits |
| Gestión de Riesgos | Matriz PM-03 (R-01 Tuya cancelado ADR-001 incluido) | Riesgos priorizados con mitigación y dueño |
| Medición y Calidad | KPIs PRD §5 + CI `arduino-cli` + tests PyTest/JUnit | Métrica `meets_kpi` automatizada; merge bloqueado si no compila |

**Estado actual:** Backlog y sprints base completos y versionados. Matriz de riesgos y PRD/requisitos estables. PM-05 (actualización de estado por sprint) en curso continuo. PM-02 (tablero Scrum) y PM-04 (Gantt diagramado) pendientes de formalización — registrados como brechas trazables, no como bloqueos.

**Documentación de referencia en el repositorio:**

* Backlog operativo (40 tareas, MoSCoW, dependencias): `docs/backlog.md`
* Planificación por sprints (4 sprints, habilitadores, estado actual): `docs/sprints.md`
* Mapa académico U1-U3 → aplicación: `docs/materias/administracion-proyectos.md`
* Historias BDD y RF/RNF: `docs/requirements.md:51` (HU-01..04) y `docs/requirements.md:13` (RF/RNF)
* Visión, alcance y KPIs medibles: `docs/prd.md:5` y `docs/prd.md:46`
* Matriz de riesgos: `docs/risk-register.md` (R-01 ADR-001) — ítem PM-03
* Plan de pruebas SQA: `docs/test-plan.md` — ítems PM-05/06/07
* Inventario hardware (base de costos): `docs/hardware-inventory.md`
* Convenciones y quality gates: `AGENTS.md:3`
* Pipeline CI (6 jobs): `.github/workflows/ci.yml`

Quedo atento a sus comentarios o ajustes para adaptar el cronograma de evaluación a lo que defina la facultad.

Un saludo cordial,

**Andres Felipe Martinez Henao**
Estudiante TS683 Administración y Planeación — Grupo 401
Proyecto Integrador: AetherNet IoT & Autonomous Rover
