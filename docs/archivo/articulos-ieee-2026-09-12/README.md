# Artículos IEEE — AetherNet IoT & Autonomous Rover

> 7 artículos en formato IEEE Conference (`IEEEtran`, doble columna, 6 páginas máx) — 1 general + 6 por materia (LowCode provisional sin lineamientos docentes).
> Cada `.tex` compila directo con `pdflatex`/`latexmk`. Cada `.md` es la versión legible en GitHub.

## Estructura

| # | Archivo `.tex` / `.md` | Materia | Código | Enfoque IEEE | Lineamientos fuente |
|---|---|---|---|---|---|
| 0 | `00-general-integrador` | **General Integrador** | — | Visión 3 capas, PRD/RF/RNF/HU-01..04, arquitectura, sprints 1-4, KPIs | `docs/prd.md`, `requirements.md`, `architecture.md`, `sprints.md`, `roadmap.md`, `backlog.md` |
| 1 | `01-estadistica-TS4D3` | Estadística | **TS4D3** | U1-U8, EMA α=0.2, Welch t-Student + ANOVA, datasets 36.5k | `docs/Estadistica/estadistica.md`, `roadmap-estadistica.md`, `backlog-estadistica.md`, `stats/ema_filter.py`, `water_turbidity_analysis.py`, `EMA_Estadistica.ipynb` |
| 2 | `02-devops` | DevOps | Electiva | CALMS, Docker Compose 3 svc, Mosquitto ACL, CI/CD 6 jobs arduino-cli | `docs/DevOps/devops.md`, `roadmap-devops.md`, `backlog-devops.md`, `docker-compose.yml`, `ci.yml` |
| 3 | `03-administracion-TS683` | Administración y Planeación | **TS683** | Scrum MoSCoW, WBS 40, Gantt Mermaid, risk R-01..R-13, costos | `docs/Administracion-Proyectos/administracion-proyectos.md`, `roadmap-administracion.md`, `backlog-administracion.md`, `gantt.md`, `risk-register.md` |
| 4 | `04-programacion-movil-TS6C3` | Programación Móvil | **TS6C3** | .NET/Xamarin→Kotlin/Compose, MVVM StateFlow, Paho MQTT, joystick 20 Hz | `docs/Programacion-Movil/programacion-movil.md`, `roadmap-movil.md`, `backlog-movil.md`, `app/` |
| 5 | `05-firmware-hardware` | Firmware + Hardware | — | MEGA/Uno/ESP32, pines 44-46/22-36/9/8/7, RF nRF24L01, HC-SR04+EMA, TCRT, L298N/TT 1:48 | `docs/Firmware/firmware.md`, `roadmap-firmware.md`, `docs/Hardware/hardware.md`, `hardware-inventory.md`, `fritzing/` |
| 6 | `06-automatizacion-lowcode` | Automatización LowCode | — | **Provisional sin lineamientos docentes:** Telegram Bot HTTP directo, Node-RED deuda 2026-09-09, Tuya ADR-001 | `docs/Automatizacion-LowCode/roadmap-lowcode.md`, `backlog-lowcode.md`, `automation/flows/intrusion_alert.json`, `adr-001` |

## Compilación

```bash
# Un artículo
cd docs/articulos-ieee
pdflatex 00-general-integrador.tex && pdflatex 00-general-integrador.tex
# O todos
./build.sh
# Requiere: texlive-latex-recommended, texlive-latex-extra, texlive-fonts-recommended
```

`build.sh` genera los 7 PDFs en `docs/articulos-ieee/build/`.

## Convenciones IEEE aplicadas

- Clase `IEEEtran` conference, `a4paper`, `compsoc` opcional, doble columna, 10pt.
- Abstract 150-250 palabras + 4-6 Keywords.
- Secciones numeradas: I Introducción, II Marco / Lineamientos, III Metodología, IV Resultados, V Discusión/Limitaciones, VI Conclusiones, Referencias (numeradas [1]..[n]), opcional Apéndice.
- Tablas y figuras numeradas con caption IEEE, citadas en texto.
- Referencias cruzadas a trazabilidad RF/HU/backlog/sprint con `docs/` exactos.
- Cumplimiento FOSS 100% (RNF-3.1) declarado en cada artículo.

## Correspondencia 6 versiones pedidas

El usuario pidió 6 versiones (1 por materia + general). Este repo tiene 7 materias canónicas (`docs/README.md`). Se entregan:
- 5 materias con lineamientos docentes formales (Estadística, DevOps, Administración, Móvil, Firmware/Hardware conjunto)
- 1 materia LowCode **provisional** sin lineamientos (marcada explícitamente)
- 1 artículo general integrador
= 7 PDFs. Si el tribunal exige exactamente 6, unir `05-firmware-hardware.tex` como un solo bloque (ya está unido) → quedan 6.

## Trazabilidad por artículo

Cada artículo cita en § Referencias los `docs/` exactos usados, para que el evaluador pueda auditar la correspondencia lineamiento→implementación.
