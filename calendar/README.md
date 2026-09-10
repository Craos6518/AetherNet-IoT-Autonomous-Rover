# Calendario AetherNet — 8 semanas (10:00-23:00) + Sprints + Clases UTP

> **Rama:** `docs/revision-sprint2-completa@90c5118` — ventana `2026-08-12` → `2026-10-06` (8 semanas) + buffer sustentación `2026-10-07` → `10-10`.
> **Fuente fechas:** `docs/Administracion de proyectos/Proyecto_AetherNet_Andres_Felipe_Martinez_Henao.docx` Tabla 4 (Sem 1-2, 3-4, 5-6, 7-8 + Sem 9 buffer) + `docs/gantt.md:7` Mermaid (`2026-08-12` 5d … `2026-09-22` 10d) + `git log --all --date=short` commits DEVOPS-01..07/MOV-02..04/laser v2 `c7ce065`/datasets `144fda2/ec5d70b`.

## Archivos generados (Google Calendar compatible)

| Archivo | Formato | Contenido | Eventos | Uso |
|---|---|---|---|---|
| `AetherNet_8semanas_Trabajo_Sprints.ics` | **iCalendar ICS** (recomendado) | 4 sprints + buffer + **104 bloques diarios trabajo 10:00-23:00 split por clases** | 109 | Importar a Google Calendar → `Importar → Seleccionar archivo` |
| `AetherNet_8semanas_Trabajo_Sprints.vcs` | **vCalendar VCS** (legacy) | Copia idéntica del ICS en formato VCS 1.0 | 109 | Outlook antiguo / import masivo |
| `AetherNet_Clases_UTP_2026-2.ics` | ICS **solo clases** (RRULE semanal) | 8 clases recurrentes hasta `2026-10-06` | 8 | Referencia — ya existen en tu Google Calendar, no duplicar si ya están |
| `AetherNet_Completo_8semanas.ics` | ICS **completo** | Trabajo+Sprints + Clases en un solo archivo | 117 | Import único si quieres todo junto |
| `AetherNet_Google_Calendar.csv` | **CSV Google** | Mismo contenido trabajo+sprints en formato CSV (`Subject,Start Date,Start Time...`) | 109 filas | `Google Calendar → Configuración → Importar → CSV` |

Todos en `TZID:America/Bogota` (UTC-5, sin DST). `DTSTAMP` `20260910T214904Z` + `UID@aethernet.utp`.

## Ventana de trabajo diaria — 10:00-23:00, split alrededor de clases

La doc indica "8 semanas 10:00 AM – 11:00 PM" como ventana disponible del responsable único (trabajo individual). Los bloques diarios se **parten** para no solapar con las clases ya creadas en tu Google Calendar:

| Día | Bloques trabajo (OPAQUE) | Clases que se respetan (TRANSP en otro calendario) |
|---|---|---|
| **Lunes** | `10:00-18:30` + `20:10-23:00` | `TS683 Admin` `18:30-20:10` `1D-509` Valentina |
| **Martes** | `10:00-18:30` + `20:10-23:00` | `TS6C3 Móviles` `18:30-20:10` `13B-301` Edisson |
| **Miércoles** | `10:00-18:30` + `22:10-23:00` | `TS683` `18:30-20:10` + `TS4D3 Estadística` `20:30-22:10` |
| **Jueves** | `10:00-18:30` + `20:10-23:00` | `TS6C3 Móviles` `18:30-20:10` `1D-207` Edisson |
| **Viernes** | `10:00-18:30` + `20:10-23:00` | `TS4D3 Estadística` `18:30-20:10` `4A-248` Maria Paula |
| **Sábado** | `11:20-14:00` + `17:20-23:00` | `TS6D3 DevOps` `08:00-11:20` Julian + `TS6F3 LowCode` `14:00-17:20` Sebastian (`13A-311`) |
| **Domingo** | `10:00-23:00` | — libre |

> **Si ya tienes las 8 clases en Google Calendar**, importa solo `AetherNet_8semanas_Trabajo_Sprints.ics` (o el CSV). Los bloques ya dejan hueco para esas clases — no habrá solape visual aunque estén en calendarios separados.

## Sprints — 5 materias backlog en cada evento

Cada evento Sprint (10:00 del primer día → 23:00 del último día, `TRANSP:TRANSPARENT` para no bloquear vista diaria) lista en `DESCRIPTION` los backlogs de las **5 áreas** (Móviles, DevOps, LowCode, Gestión, Estadística) con traza a commits/fechas:

| Sprint | Rango (10:00-23:00) | Backlogs clave (ver `DESCRIPTION` completo) | Commits / evidencia |
|---|---|---|---|
| **S1 Infra** | `2026-08-12` → `2026-08-26` | DEVOPS-01 Docker/PG/Mosquitto `2026-08-12` 5d, DEVOPS-02 ACL `aethernet/#`, DEVOPS-03 `init.sql`, DEVOPS-04 `ci.yml` 1.5.1, DEVOPS-05 nRF24L01 `5235a5d`, MOV-01 MVVM `f03190b`, EST-01 `ema_filter.py` α=0.2, PM-02 Projects 14 | `5781791` `5235a5d` `f03190b` `a051dd4` |
| **S2 Domótica** | `2026-08-26` → `2026-09-09` | FW-MEGA cerrojo 4×4+MG90S 44-46, FW-laser KY008+LDR `c7ce065`, MOV-02 `LedStatusCard` `eb01fdd`, MOV-03 `MqttManager` `662ae9f`, MOV-04 `PinViewModel`, DEVOPS-06/07 8 endpoints 21 tests `73cee08/99c8caa`, EST 36.5k `144fda2/ec5d70b`, LOW-02 deuda | `c7ce065` `eb01fdd` `662ae9f` `73cee08` `ec5d70b` |
| **S3 Rover** | `2026-09-09` → `2026-09-23` | FW-ROVER L298N/HC-SR04/TCRT5000, fail-safe 500ms, MOV-05 joystick, MOV-06 <10ms, MOV-07 BT SPP | planificado — sin commits aún |
| **S4 Cierre** | `2026-09-23` → `2026-10-07` | LOW-03 Telegram, LOW-05 intrusión, EST-02/03 EMA firmware, EST-04 PG, EST-05 t-Student, EST-06/07 KPI>85%, MOV-08..10, DEVOPS-09 `docker compose up` | planificado |
| **Buffer** | `2026-10-07` → `2026-10-10` | E2E, presentación, sustentación | — |

Los **bloques diarios** heredan el Sprint del día (`S1/S2/S3/S4/BUF` en `CATEGORIES` y en `SUMMARY: AetherNet S2 — Trabajo`) y listan en `DESCRIPTION` 5 backlogs del sprint para contexto rápido.

## Cómo importar a Google Calendar

### Opción A — ICS (recomendado)
1. Google Calendar → ⚙️ Configuración → `Importar y exportar` → `Seleccionar archivo de tu ordenador` → elige `AetherNet_8semanas_Trabajo_Sprints.ics`
2. Elige calendario destino (recomendado: crear calendario nuevo "AetherNet 8 semanas" para poder ocultar/mostrar)
3. Importar → 109 eventos. Verifica `America/Bogota` en zona horaria.
4. Si quieres clases también: repite con `AetherNet_Clases_UTP_2026-2.ics` (RRULE semanal hasta `2026-10-06`) — o omite si ya las tienes.

### Opción B — CSV
1. `AetherNet_Google_Calendar.csv` ya en formato Google (`MM/DD/YYYY` + `10:00 AM`/`11:00 PM`)
2. Google Calendar → Importar → CSV → mismo calendario destino.

### Opción C — VCS
Mismo que ICS pero para clientes que solo aceptan `.vcs` (Outlook legacy).

## Validación

```bash
# Contar eventos
grep -c "BEGIN:VEVENT" calendar/*.ics
# → AetherNet_8semanas_Trabajo_Sprints.ics 109 (5 sprints + 104 diarios)
# → AetherNet_Clases_UTP_2026-2.ics 8
# → AetherNet_Completo_8semanas.ics 117

# Verificar timezone
grep "TZID:America/Bogota" calendar/AetherNet_8semanas_Trabajo_Sprints.ics | head -1

# Probar import en Thunderbird/Outlook antes de Google si quieres
```

## Notas de fechas vs docx

- **Docx Tabla 4** define Sprints por "Sem." `1–2`, `3–4`, `5–6`, `7–8` + `8 (sem 9)` buffer sin fechas absolutas. **Gantt** fija `2026-08-12` como inicio (merge `9eac686` Sprint 1 fechado 2026-08-26). Se tomó `2026-08-12` como ancla y se proyectaron 8 semanas = `2026-10-06`.
- **Commits reales** usados como evidencia en `DESCRIPTION`: `5781791` (DEVOPS-04), `5235a5d` (DEVOPS-05), `f03190b` (MOV-01), `c7ce065` (laser v2), `eb01fdd/662ae9f/73cee08` (MOV-02/03/DEVOPS-06) — coinciden con `git log --date=short --all` del repo.
- Si tu UTP arranca en otra semana, ajusta `START_WORK` en `/tmp/gen_calendar.py` y re-ejecuta.

## Generador

`python3 /tmp/gen_calendar.py` — idempotente, regenera los 5 archivos. Fuente única de verdad para fechas: `docs/Administracion de proyectos/Proyecto_AetherNet_Andres_Felipe_Martinez_Henao.docx` + `docs/gantt.md` + `git log`.

---
*Generado 2026-09-10 en `docs/revision-sprint2-completa@90c5118` — rama de revisión documental Sprint 2.*
