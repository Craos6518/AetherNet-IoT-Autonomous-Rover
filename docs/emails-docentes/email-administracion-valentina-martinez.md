Asunto: Avance proyecto integrador AetherNet — TS683 Administración y Planeación (contexto post-sismo)

Estimada profesora Valentina:

Le escribo con profundo respeto por lo que está viviendo nuestra comunidad universitaria tras el **sismo del 10 de agosto (7:34 AM, mag. 7.4)**. La afectación ha sido muy desigual: compañeros con daños en sus hogares, otros desplazados a sus regiones de origen, muchos como voluntarios en zonas críticas. En mi caso, mi familia y yo no tuvimos pérdidas materiales graves, estoy apoyando como voluntario en lo que puedo, y cuento con condiciones para seguir trabajando en la **planificación y gestión del proyecto integrador** desde casa.

Solo tuvimos **una semana de clases** antes del sismo. Dado que su asignatura **es transversal a todo el proyecto**, mi intención es **mantener viva la gestión** (backlog, riesgos, sprints, métricas) para que, cuando retomemos, el equipo tenga una hoja de ruta clara y podamos enfocarnos en la ejecución coordinada.

---

### ¿Qué es AetherNet? (El proyecto que estoy gestionando)

**AetherNet IoT & Autonomous Rover** es mi **Proyecto Integrador de 5º semestre** (Ingeniería de Sistemas / Desarrollo de Software). Es un **sistema real, multidisciplinar, hardware + software** que integra 5 subsistemas comunicados:

| Subsistema | Tecnología Principal | Qué hace |
|------------|---------------------|----------|
| **App Móvil** | Kotlin + Jetpack Compose (Android nativo) | Interfaz humana: dashboard, joystick, PIN, alertas |
| **Backend** | Python (FastAPI) + PostgreSQL + Mosquitto MQTT (Docker) | API, Base de datos, Broker de mensajes tiempo real |
| **Automatización** | Node-RED (LowCode visual) + Telegram Bot + `tuya-local` | Reglas: "si intrusión → Telegram + luz roja" |
| **Gateway ESP32** | C++ (Arduino CLI) | Puente Wi-Fi/MQTT ↔ Radio 2.4GHz (nRF24L01) ↔ Puerto serie (UART) |
| **Controlador MEGA** | C++ (Arduino IDE) | Cerrojo (teclado 4x4 + servo), Láser KY-008, LED RGB |
| **Rover UNO** | C++ (Arduino IDE) | Robot oruga: motores L298N, Ultrasonido HC-SR04, IR TCRT5000, Radio nRF24L01 |
| **Estadística** | Python (Pandas, SciPy) | Filtro EMA, t-Student, Weibull — aplicados a sensores reales |

**Restricción clave del proyecto:** **100% FOSS (código abierto)** — sin AWS, Azure, Tuya Cloud, ni licencias pagas. Todo corre en red local (LAN).

---

### El sismo como **caso de estudio real de Gestión de Riesgos** (Unidad 2)

Lo ocurrido **es en sí mismo un riesgo materializado** que estoy documentando formalmente en la matriz del proyecto (`PM-03` en backlog):

| Riesgo Identificado (Pre-sismo) | Probabilidad | Impacto | Qué pasó (10 ago) | Lección / Acción Correctiva |
|--------------------------------|--------------|---------|-------------------|----------------------------|
| **Falla infraestructura UTP / laboratorios** | Baja | Alto | **Materializado** — acceso limitado a labs | Validar riesgos hardware **temprano** (Sprint 1): LOW-01 bombillo Tuya, DEVOPS-05 enlace RF |
| **Dependencia de hardware físico único** | Media | Alto | Parcial (algunos tienen kits en casa, otros no) | Diseñar **prototipos software-first**: simuladores (Wokwi), datos sintéticos, stubs |
| **Disponibilidad desigual del equipo** | Media | Medio | **Materializado** — compañeros voluntarios, desplazados | Planificar **tareas asíncronas + parcelables**, no todo sincrónico; dueños flexibles |

> Esto **es exactamente lo que enseña la Unidad 2**: identificar, analizar, priorizar, mitigar y dar seguimiento a riesgos. El sismo no fue "teoría" — fue un evento real que activó nuestro plan de contingencia.

---

### Alineación con TS683 — Artefactos de Gestión Reales (no teóricos)

| Unidad Syllabus | Artefacto en AetherNet | Qué demuestra | Estado |
|-----------------|------------------------|---------------|--------|
| **U1: Scrum, Planificación, BDD, PSP/TSP, Marco Lógico** | `docs/sprints.md` (4 sprints, 8 sem, dependencias)<br>`docs/backlog.md` (~40 tareas MoSCoW)<br>`docs/requirements.md` (4 HU BDD: Dado/Cuando/Entonces)<br>`docs/roadmap.md` (conocimientos por materia) | Planificación real, WBS implícita, Historias INVEST+BDD, Trazabilidad HU↔RF↔Sprint | ✅ Base completa |
| **U2: Riesgos, Configuración, Medición/Análisis** | Matriz Riesgos `PM-03` (con sismo real)<br>Control Config: Git + GitHub Actions + `arduino-cli` (compila 3 firmwares)<br>KPIs medibles `PRD §5` (latencia, ruido, falsos positivos, FOSS%) | Gestión riesgos viva, CI/CD como control configuración, Métricas cuantificables | 🟡 En curso |
| **U3: Calidad, Costos, Monitoreo/Control** | CI/CD obligatorio (RNF-1.2) — falla build si firmware no compila<br>Tests: PyTest (backend) + JUnit (app ViewModels)<br>Retrospectivas `PM-06` por sprint<br>Burndown via GitHub Projects | Quality Gate automático, Métricas de proceso, Mejora continua | 🟡 Planeado |

---

### Estructura de 3 Cortes (Syllabus) ↔ 4 Sprints (Proyecto Real)

| Corte Syllabus | Entregable de Gestión | Sprint Proyecto | Evidencia Concreta |
|----------------|----------------------|-----------------|-------------------|
| **Corte 1 (30%)**<br>Planificación + Entrega 1 | **Backlog MoSCoW priorizado** + **Matriz Riesgos** + **Gantt** (PM-04) + **Sprint 1 planificado** con DoD | **Sprint 1**: Infraestructura (Docker + CI/CD + RF base) | `PM-01 a 04`, `DEVOPS-01 a 05`, `docs/sprints.md` Sprint 1 |
| **Corte 2 (30%)**<br>Seguimiento + Entrega 2 | **Sprint 2-3 ejecutados** + **Retrospectivas** (actas) + **Burndown** + **Riesgos actualizados** | **Sprint 2**: Cerrojo + App base<br>**Sprint 3**: Rover + Telemetría | `MOV-02 a 06`, `LOW-02`, `RF-3.1/3.2`, `docs/sprints.md` Sprint 2-3 |
| **Corte 3 (40%)**<br>Entrega Final | **Proyecto integrado** + **Demo E2E** + **Lecciones aprendidas** + **Métricas finales vs KPIs** | **Sprint 4**: EMA firmware + t-Student + Intrusión completa (Telegram+Tuya) + Docs | `EST-02/05/07`, `LOW-03/04/05`, `docs/backlog.md` completado |

---

### Mi enfoque de gestión durante la contingencia (práctico, empático, asíncrono)

**Tengo todo el hardware en casa** (ESP32, MEGA, UNO, Nano, sensores, módulos, chasis, laptop). Esto **reduce drásticamente el riesgo "Dependencia de hardware físico único"** para mi parte: puedo ejecutar tareas `hardware-dep` (flasheo, depuración UART/SPI/Radio, sensores reales, comunicación MEGA↔ESP32↔UNO) sin necesitar el laboratorio universitario.

| Principio | Qué hago concretamente |
|-----------|------------------------|
| **Fuente de verdad única y viva** | `docs/sprints.md` y `docs/backlog.md` se actualizan en GitHub — cualquiera del equipo ve estado real sin reuniones |
| **Tareas parcelables (hardware en casa vs lab)** | Backlog: ítems `hardware-dep` **sí los puedo hacer yo en casa** (tengo kit completo); los que requieren lab compartido (impresión 3C, fuente de banco, osciloscopio) se dejan para el retorno |
| **Comunicación transparente, no presionante** | Este correo a 4 docentes + Issues GitHub por tarea → quien pueda aporta, quien no, no se bloquea |
| **Empatía operativa** | Dueños de tareas "Must" tienen **fechas flexibles** hasta normalización; no asignar a quien está en emergencia |
| **Gestión de configuración real** | `arduino-cli` en GitHub Actions = **quality gate automático**: si firmware no compila, no hay merge — evita deuda técnica silenciosa |
| **Mitigación riesgo "hardware único" activada** | Yo avanzo integración física (UART, Radio, Sensores) en casa; resto del equipo prioriza software-only; sincronizamos en lab cuando haya acceso |

---

### Evidencia de gestión disponible para su revisión (todo en repositorio)

| Documento | Qué contiene | Ubicación |
|-----------|--------------|-----------|
| **Product Backlog MoSCoW** | 5 áreas, ~40 tareas, prioridad, sprint, dependencia, origen (RF/HU), riesgos | `docs/backlog.md` |
| **Sprint Backlog + Gantt** | 4 sprints, dependencias inter-sprint, estado actual, línea base | `docs/sprints.md` |
| **Historias BDD** | 4 HU formato *Dado/Cuando/Entonces* (criterios aceptación verificables) | `docs/requirements.md` §4 |
| **Matriz de Riesgos** | Identificación, análisis cuali/cuantitativo, mitigación, dueño, estado | `docs/backlog.md` §99-104 + `PM-03` |
| **Requisitos Funcionales/No Funcionales** | 14 RF + 8 RNF trazados a HU y sprints | `docs/requirements.md` |
| **Visión + KPIs + Restricciones** | Métricas cuantificables (latencia, ruido, falsos positivos, FOSS%) | `docs/prd.md` |
| **Roadmap Conocimientos** | Qué saber por materia, orden de aprendizaje, dependencias cruzadas | `docs/roadmap.md` |
| **Convenciones de Trabajo** | Referencia obligatoria HU/RF en commits, CI obligatorio, FOSS estricto | `AGENTS.md` |

---

### Competencias del Syllabus evidenciadas en la práctica

| Competencia (Syllabus) | Evidencia Concreta en AetherNet |
|------------------------|--------------------------------|
| **Cognitivas**: Metodologías TI, PMBOK, Marco Lógico | Scrum aplicado 4 sprints, WBS via backlog, Historias INVEST+BDD, Matriz Lógica implícita en HU |
| **Procedimentales**: Planificación, Organización, Estimación, Dirección equipos/proyectos | Planning Poker (PM-07), Gantt con dependencias, Coordinación 5 áreas técnicas heterogéneas (app, backend, firmware x3, stats, lowcode) |
| **Actitudinales**: Liderazgo, Responsabilidad, Trabajo bajo stress, Adaptación cambios | **Gestión real post-sismo**: re-planificación, comunicación proactiva docentes, priorización empática, transparencia |

---

Profesora, entiendo que la planificación académica institucional está en redefinición. Este correo no busca presionar calendarios, sino **demostrar que la gestión del proyecto sigue viva, se adapta a la realidad, y usa sus herramientas (riesgos, sprints, métricas, retrospectivas) para navegar la contingencia**.

Quedo a su entera disposición para ajustar lo que sea necesario. Mi prioridad, como la de todos, es el bienestar de la comunidad.

Con todo mi respeto y solidaridad,

**[Su Nombre]**
Estudiante TS683 Administración y Planeación de Proyectos de Software - Grupo 401
Proyecto Integrador: AetherNet IoT & Autonomous Rover