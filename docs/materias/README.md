# Documentación Académica por Materia

Mapeo entre el contenido oficial de cada asignatura UTP (PDFs en `docs/UTP/`) y su aplicación real en el proyecto AetherNet IoT & Autonomous Rover: **qué se ve en el programa → qué se aplica → en qué área/archivo está aplicado**.

| Materia | Código | Área del proyecto | Documento académico | Roadmap de temas | Backlog operativo |
|---|---|---|---|---|---|
| Estadística | TS4D3 | Área 5 — Filtrado y Analítica (`stats/`, firmware Rover) | [estadistica.md](estadistica.md) | [roadmap-estadistica.md](roadmap-estadistica.md) | [backlog-estadistica.md](backlog-estadistica.md) |
| DevOps | Electiva | Área 2 — Infraestructura, CI/CD y Firmware base (`.github/`, `docker-compose.yml`, `firmware/`, `backend/`) | [devops.md](devops.md) | [roadmap-devops.md](roadmap-devops.md) | [backlog-devops.md](backlog-devops.md) |
| Administración y Planeación de Proyectos | TS683 | Área 4 — Gestión (`docs/sprints.md`, `backlog.md`, `risk-register.md`) | [administracion-proyectos.md](administracion-proyectos.md) | [roadmap-administracion.md](roadmap-administracion.md) | [backlog-administracion.md](backlog-administracion.md) |
| Programación Móvil | TS6C3 | Área 1 — App AetherControl (`app/`) | [programacion-movil.md](programacion-movil.md) | [roadmap-movil.md](roadmap-movil.md) | [backlog-movil.md](backlog-movil.md) |
| Automatizaciones LowCode *(sin asignatura)* | — | Área 3 — Node-RED, Telegram, Tuya (`automation/`) | — | incluido en el documento único | [backlog-lowcode.md](backlog-lowcode.md) |

**Cómo usar estos documentos:**
- **Documento académico:** mapeo "qué se ve en el programa del PDF → qué se aplica → dónde está aplicado" (para sustentación/informe).
- **Roadmap por materia:** TODOS los temas necesarios para completar esa área del proyecto, con profundidad requerida y requisito que habilita.
- **Backlog por materia:** tareas operativas ultra-específicas (qué hacer exactamente, alcance IN/OUT, criterios de aceptación verificables). Los IDs heredados de `docs/backlog.md` se mantienen; los nuevos continúan la serie. En conflicto, gana el backlog específico por materia.

Notas:
- La quinta área operativa del backlog (Área 3 — Automatizaciones LowCode: Node-RED, Telegram, `tuya-local`) no tiene asignatura con PDF propio; sus evidencias se citan transversalmente en los cuatro documentos.
- Programación Móvil: el PDF académico usa .NET/Xamarin; el proyecto aplica los mismos patrones (MVVM, UI declarativa, REST/servicios, permisos) en Kotlin/Jetpack Compose — la equivalencia está documentada unidad por unidad.
- Cada documento incluye sección de brechas honestas (temas del programa sin aplicación o pendientes), trazables a ítems de `docs/backlog.md`.
