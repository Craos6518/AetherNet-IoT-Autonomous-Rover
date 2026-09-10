# Revisión Documental Sprint 2 — Bitácora Completa

> **Rama:** `docs/revision-sprint2-completa` (desde `sprint/2-domotica-acceso@ec5d70b`)
> **Fecha:** 2026-09-10
> **Autor:** Andrés Felipe Martínez Henao — revisión integral doc↔código
> **Base:** `sprint/2-domotica-acceso` tip `ec5d70b` (merge `c7ce065` laser v2 + `144fda2/9e25a0c/ec5d70b` datasets/admin) vs `origin/develop` (merge-base `c7ce065^`)
> **Objetivo:** sincronizar TODA la documentación con los cambios reales desde Sprint 2, sin adelantar Sprint 3-4.

---

## 1. Resumen ejecutivo

Sprint 2 está **funcionalmente Done** (cerrojo + laser + app pantallas/MQTT/PIN + backend 8 endpoints + datasets 36.5k), pero la documentación tenía **9 desalineaciones** detectadas en la auditoría. Esta rama corrige esas desalineaciones y deja trazabilidad explícita RF→archivo para que Sprint 3 no herede deuda documental.

**Resultado:** 13 archivos actualizados + 1 nuevo (`docs/revision-sprint2.md` = este archivo). Ningún cambio de código — solo docs. Todos los checks de esta rama son `docs/*` y `notebooks/`/`stats/Dataset/` ya mergeados.

---

## 2. Auditoría doc↔código (hallazgos)

### 2.1 Fuentes verificadas

| Fuente | Commit / archivo | Estado |
|---|---|---|
| `firmware/mega-access/src/config.h` | `ROW_PINS 30/32/34/36` `COL_PINS 22/24/26/28` `SERVO_PIN 9` `LED 44/45/46` `LASER 8/7 INPUT` | ✅ OK |
| `firmware/mega-access/src/laser.{h,cpp}` | `laserInit/handleLaser/triggerIntrusionAlert` `CHECK 50ms` `COOLDOWN 3s` `SECURITY:` | ✅ OK |
| `firmware/test-laser-uno/` | banco aislado LDR discreta 10k | ✅ OK |
| `firmware/gateway-esp32/gateway-esp32.ino:20-68` | `secrets.h` fallback + topics `aethernet/#` | ✅ OK |
| `backend/app/routers/events.py:25` + `main.py:16` + `schemas.py` + `models.py:34` | 8 endpoints + `GET /health` degraded | ✅ OK |
| `backend/mosquitto/config/{mosquitto,acl}.conf` | `1883/9001` + `aethernet/#` + `$SYS/#` | ✅ OK |
| `backend/tests/test_events.py:14` + `test_health.py:7` | 21 verdes | ✅ OK |
| `app/src` `MqttManager` + `LedState` + `PinViewModel` | Paho 1.2.5 `aethernet/#` `assembleDebug` verde | ✅ OK |
| `automation/flows/intrusion_alert.json` | `mqtt-intrusion` → `telegram-alert` → `http-telegram` | ⚠️ Referencia (no deploy — deuda) |
| `stats/water_turbidity_analysis.py:1` + `Dataset/` + `data/*.png` + `notebooks/EMA_Estadistica.ipynb` §7c | 36.5k filas (31.5k CC BY-SA 4.0 + 5k CC0) Welch/ANOVA | ✅ OK |
| `docs/Administracion de proyectos/Proyecto_AetherNet_*.docx` | 99KB 14 secciones | ✅ OK |

### 2.2 Desalineaciones detectadas (antes de esta rama)

| # | Doc | Desalineación | Severidad |
|---|---|---|---|
| 1 | `docs/architecture.md` §2/§3/§5/§7 | MEGA decía "láser reservado", Node-RED como vigente, topics "pendiente de definición" aunque ya definidos en `acl.conf` | **Alta** |
| 2 | `docs/sprints.md` §Estado actual | Última actualización 2026-09-07, sin laser v2 `c7ce065` ni datasets `ec5d70b` | **Alta** |
| 3 | `docs/backlog.md` Área 2/5 | DEVOPS-01..05/08 sin marca Done, sin fila `FW-MEGA-CERROJO/LASER`, EST-01 sin Done, EST-05/06 sin datasets | **Alta** |
| 4 | `docs/gantt.md` | Sprint 2 aún `active` (cerrojo/laser/MOV-02..04), LOW-02 como `active`, LOW-04 sin tachado, sin datasets ni revisión | **Media** |
| 5 | `docs/tablero-scrum.md` | Sprint 2 In Progress con 7 tarjetas aún "activas", Done vacío | **Media** |
| 6 | `docs/prd.md` §4 In-Scope | "Automatización LowCode mediante Node-RED" sin nota deuda | **Media** |
| 7 | `docs/requirements.md` §2.4 + HU-02 | RF-4.1/ Then decía "Node-RED envía" sin deuda | **Media** |
| 8 | `docs/roadmap.md` §3/§5 | LowCode solo Node-RED, Estadística sin datasets 36.5k | **Media** |
| 9 | `docs/README.md` | Mapa rápido 2026-09-07, sin datasets/admin | **Baja** |
| 10 | `docs/materias/backlog-*.md` + `roadmap-*.md` | Estado "Aug 2026" — `main.py NO existe`, `MQTT TODOs` | **Media** |
| 11 | `docs/branching-strategy.md` | Count 35 ramas, LOW-02 sin deuda, LOW-04 sin cancelado | **Baja** |
| 12 | `docs/test-plan.md` §2 HU-02 | "Flujo Node-RED" sin deuda | **Baja** |

---

## 3. Cambios realizados (esta rama)

| Archivo | Cambio |
|---|---|
| `docs/sprints.md` | `Sprint activo` → fase cierre documental + nueva sección `Rama de revisión` + `Última actualización 2026-09-10` con 4 bullets (10-09 revisión, 09 datasets/admin, 07 laser v2 `c7ce065`, 07 DEVOPS-06/07) + `Próximo` → Sprint 2 cierre (todo Done, merge → develop pendiente) |
| `docs/backlog.md` | ÁREA 2: DEVOPS-01..05 marcadas Done con archivos/líneas, DEVOPS-08 Done, nuevas filas `FW-MEGA-CERROJO` + `FW-MEGA-LASER` Done `c7ce065`; ÁREA 5: EST-01 Done, EST-05/06 🔄 adelantado parcial con `water_turbidity_analysis.py` 36.5k; ÁREA 4: PM-02/03/04 Done, PM-05 🔄; Resumen riesgos actualizado LOW-02 deuda + EST-01 adelantado |
| `docs/architecture.md` | §1-§2: capa 3 → Telegram directo + Node-RED deuda; MEGA → laser activo `7/8 INPUT`; Backend → 8 endpoints + acl; Node-RED → Telegram Bot directo; stats → 36.5k; §3: Gateway↔Backend↔App sin Node-RED, Gateway→Telegram HTTP; diagrama actualizado; §5 paso 6 → Telegram directo; §6: nueva decisión "Node-RED deuda"; §4: flujo con Welch/ANOVA + datasets; §7: "Pendiente" → "Topics definidos" con lista canónica `aethernet/#` |
| `docs/prd.md` | §4 In-Scope: Node-RED → deuda 2026-09-09 + Telegram directo |
| `docs/requirements.md` | §2.4: Node-RED → Telegram directo (deuda 2026-09-09, `intrusion_alert.json` referencia); HU-02 Then → Telegram directo + LED rojo 3s |
| `docs/gantt.md` | Header → actualizado 2026-09-10; Sprint1 LOW-01 → Done cancelado; Sprint2 → todo `done` + `EST datasets` + `Revision docs` active; Sprint4 LOW-04 → done cancelado; hitos → laser/ datasets/ revisión; riesgo LOW-01 → cancelado + LOW-02 deuda |
| `docs/tablero-scrum.md` | Kanban: Sprint2 In Progress → vacía, Done Sprint2 → 7 tarjetas Done (FW-MEGA, MOV-02..04, DEVOPS-06/07, EST); nueva sección "Sprint 2 Done" + "Sprint 2 revisión" |
| `docs/README.md` | Mapa rápido → 2026-09-10 + 9 filas actualizadas + sección notebooks+datasets ampliada |
| `docs/branching-strategy.md` | Modelo → `docs/revision-sprint2-completa` + Sprint2 → RF-2.3 + LOW-02 deuda + LOW-03/04/05 actualizados + count 35→36 |
| `docs/roadmap.md` | §3 LowCode → Telegram directo + Node-RED deuda + Home Assistant nota + §5 Estadística → datasets 36.5k + validación externa |
| `docs/test-plan.md` | HU-02 LOW-05 → Telegram directo (deuda) + §6 → datasets Welch/ANOVA |
| `docs/materias/backlog-lowcode.md` | Estado → 2026-09-10 deuda Node-RED + flujo vigente laser |
| `docs/materias/backlog-movil.md` | Estado → 2026-09-10 MOV-01..04 Done |
| `docs/materias/backlog-devops.md` | Estado → 2026-09-10 DEVOPS-01..08 Done + laser validado |
| `docs/materias/backlog-estadistica.md` | Estado → 2026-09-10 36.5k validación externa |
| `docs/materias/roadmap-*.md` (4) | `Estado al Aug 2026` → `2026-09-10` + contenido actualizado por materia |
| `docs/revision-sprint2.md` | **Nuevo** — este archivo (bitácora + trazabilidad) |

**Total líneas cambiadas (estimado):** ~400 inserciones / ~150 eliminaciones en 13 docs existentes.

---

## 4. Trazabilidad RF/RNF/HU → Archivo → Estado

| RF/HU | Archivo(s) | Estado Sprint 2 |
|---|---|---|
| **RNF-1.1** Docker | `docker-compose.yml:1` + `backend/Dockerfile` | ✅ Done |
| **RNF-1.2** CI arduino-cli | `.github/workflows/ci.yml:90` + `firmware/*/*.ino` | ✅ Done |
| **RF-3.1** nRF24L01 | `firmware/gateway-esp32/` + `firmware/rover-uno/` + `docs/testing-rf-sprint1.md:32` | ✅ Done HW 2026-09-01 |
| **RNF-3.1** FOSS 100% | `backend/requirements.txt` + `app/build.gradle.kts` + `firmware/*.ino` ArduinoJson 6.21.3 | ✅ OK |
| **RF-2.2 HU-01** cerrojo | `firmware/mega-access/src/config.h:17` `door.cpp` `keypad_control.cpp` `led.cpp:64` `uart_protocol.cpp` `mega-access.ino` | ✅ Done `feature/firmware-mega-cerrojo` |
| **RF-2.3 HU-02** laser | `firmware/mega-access/src/laser.{h,cpp}` `config.h:8/7` `test-laser-uno` | ✅ Done `c7ce065` |
| **HU-01/HU-02 LED RGB** | `hardware-inventory.md:9` 44/45/46 + `firmware/mega-access/src/led.cpp` `LED_COMMON_ANODE` + `door.cpp:52` 5s / `laser.cpp` 3s | ✅ Done |
| **RF-1.1** Dashboard/MQTT | `app/src` `MqttManager` Paho 1.2.5 `acl.conf:1` `ServiceLocator.kt:114` `DashboardViewModel` `LedStatusCard` | ✅ Done MOV-02/03 |
| **HU-01** PIN app→cerrojo | `app/src` `PinViewModel`/`PinScreen` + `gateway-esp32.ino:332` `handleAccessCommand` + `backend/app/routers/events.py:25` | ✅ Done MOV-04 35 tests |
| **RF-2.1** Gateway routing | `firmware/gateway-esp32/gateway-esp32.ino:20-51` `secrets.h` + `backend/app/main.py:16` | ✅ Done |
| **RNF-2.2** histórico | `backend/app/models.py:34` + `init.sql` + `backend/app/routers/events.py:25` | ✅ Done |
| **DEVOPS-06/07** endpoints+tests | `backend/app/config.py:58` `backend/tests/test_events.py:14` `test_health.py:7` 21 verdes + `openapi.json` 8 ops | ✅ Done 2026-09-07 |
| **DEVOPS-08** .env.example | `backend/.env.example` + `firmware/gateway-esp32/secrets.h.example` + `.gitignore:219` | ✅ Done |
| **RF-4.1 HU-02** Telegram | `automation/flows/intrusion_alert.json` (referencia, no deploy) → Gateway `POST /api/security-events` + `aethernet/seguridad/intrusion:72` → `api.telegram.org` | 🔄 Parcial (deuda LOW-02 — directo sin Node-RED Sprint 4) |
| **RF-4.2** Tuya | `docs/adr/adr-001-cancelacion-tuya.md` + `hardware-inventory.md` | ❌ Cancelado ADR-001 |
| **RNF-2.1 HU-03** EMA | `stats/ema_filter.py:15` α=0.2 + `firmware/test-ema-uno` + `notebooks/EMA_Estadistica.ipynb:2` + `stats/water_turbidity_analysis.py:1` 36.5k | 🔄 Adelantado parcial (port C++ `rover-uno.ino:259` queda Sprint 4) |
| **RNF-2.2** descriptivo/t-Student | `stats/Dataset/` 36.5k + `stats/water_turbidity_analysis.py` Welch/ANOVA + `notebooks/EMA_Estadistica.ipynb` §7c + `stats/data/water_turbidity_report.json` | 🔄 Validación externa Done, histórico real PG queda Sprint 4 |
| **PM-01..04** gestión | `docs/backlog.md` + `docs/sprints.md` + `docs/risk-register.md:32` + `docs/gantt.md:7` + `projects/14` | ✅ Done |
| **RF-3.2/3.3** Rover | `firmware/rover-uno/` `rover-uno.ino:220` fail-safe 500ms + `architecture.md` §6 | ⏳ Sprint 3 |

---

## 5. Gaps que quedan para Sprint 3-4 (no son deuda de esta rama)

- **Sprint 3:** `FW-ROVER` chasis L298N/HC-SR04/TCRT5000, fail-safe 300-500ms calibración HW, `MOV-05/06` joystick X,Y <10ms RF / <50ms Wi-Fi, `MOV-07` BT SPP `HC-06`.
- **Sprint 4:** `LOW-03/05` Telegram bot directo (no Node-RED), `EST-02/03` EMA firmware KY-037/HC-SR04, `EST-04` extracción PG real, `EST-07` reporte KPI >85%, `MOV-08..10` dashboard consolidado + reconexión MQTT + JUnit ViewModels, `DEVOPS-09` `docker-compose up` script.
- **Deudas persistentes:** `LOW-02` Node-RED (referencia `automation/flows/intrusion_alert.json`), `LOW-04/06/07` Won't.

---

## 6. Verificación de esta rama

```bash
# Docs actualizados
git diff --stat origin/sprint/2-domotica-acceso..HEAD -- docs/
# → 13 docs + 1 nuevo (este archivo) + materías

# No cambios de código (solo docs)
git diff --stat origin/sprint/2-domotica-acceso..HEAD -- backend/ app/ firmware/ automation/
# → 0 (excepto docs/fritzing PNGs ya en base ec5d70b)

# Links internos verificados (grep rápido)
grep -r "docs/revision-sprint2" docs/ --include="*.md" | wc -l   # ≥3 (sprints, backlog area, roadmap)
grep -r "DEUDA.*2026-09-09\|Node-RED.*deuda" docs/ --include="*.md" | wc -l  # ≥8
grep -r "c7ce065\|laser.*v2\|LDR.*10k" docs/ --include="*.md" | wc -l        # ≥5
```

**Próximo paso propuesto:** `git push origin docs/revision-sprint2-completa` → PR hacia `sprint/2-domotica-acceso` → squash/merge → luego `sprint/2-domotica-acceso` → `develop` para cerrar Sprint 2.

---

## 7. Historial de commits en esta rama

| Commit | Mensaje |
|---|---|
| `ec5d70b` | base `sprint/2-domotica-acceso` (admin doc + datasets 36.5k) |
| `WIP` | docs(revision-sprint2): sincroniza 13 docs + crea bitácora trazabilidad |

Ver `git log --oneline docs/revision-sprint2-completa --not origin/sprint/2-domotica-acceso` para lista completa.

---

## 8. Referencias cruzadas

- `docs/prd.md:32` (LowCode deuda) ↔ `docs/requirements.md:31` (RF-4.1 directo) ↔ `docs/architecture.md:13` (capa 3 Telegram) ↔ `docs/sprints.md:42` (Sprint 4 LowCode) ↔ `docs/backlog.md:56` (LOW-02 Won't) ↔ `docs/roadmap.md:48` (LowCode roadmap)
- `docs/hardware-inventory.md:9` (LED 44/45/46 + laser 7/8) ↔ `firmware/mega-access/src/config.h:17` ↔ `docs/fritzing/compendio-planos.md` P3/P5
- `docs/Estadistica/Datasets/README.md` ↔ `stats/Dataset/README.md` ↔ `stats/water_turbidity_analysis.py:1` ↔ `notebooks/EMA_Estadistica.ipynb` §7c ↔ `docs/roadmap.md:86` §5

*Si una contradicción futura aparece, actualizarla aquí y en `docs/sprints.md:52` PM-05.*
