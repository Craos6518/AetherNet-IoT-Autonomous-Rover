# Estrategia de Ramas — AetherNet IoT & Autonomous Rover

> Fuente de verdad: `docs/backlog.md` (IDs MoSCoW), `docs/sprints.md` (4 sprints), `docs/requirements.md` (RF/HU) y `docs/hardware-inventory.md`.

## 1. Modelo elegido: GitFlow adaptado a sprints académicos

```
main ─────────────────────────────────────────────────────► release/producción (estable, evaluable)
 │
 └─ develop ─────────────────────────────────────────────► integración continua de sprints
      │
      ├─ sprint/1-infra-firmware-base ──────────────────► Sprint 1 (Semanas 1-2)
      ├─ sprint/2-domotica-acceso ──────────────────────► Sprint 2 (Semanas 3-4)
      ├─ sprint/3-rover-telemetria ─────────────────────► Sprint 3 (Semanas 5-6)
      └─ sprint/4-lowcode-estadistica-cierre ───────────► Sprint 4 (Semanas 7-8)
           │
           └─ feature/* ────────────────────────────────► trabajo atómico por ID de backlog
```

- **`main`**: solo recibe merges desde `develop` o `hotfix/*` vía PR con CI verde (`arduino-cli` + `docker-build` + `ruff/pytest`). Protegida.
- **`develop`**: rama de integración. Todo `feature/*` hace PR hacia su `sprint/*`, y cada `sprint/*` hace PR hacia `develop` al cerrar sprint.
- **`sprint/*`**: 4 ramas de integración por sprint. Son la unidad de entrega evaluable. No se trabaja directo en ellas — solo vía PR desde `feature/*`.
- **`feature/*`**: trabajo atómico. Nomenclatura mapea 1:1 a IDs de `backlog.md`.
- **`hotfix/critical`**: fail-safe, parches de seguridad láser/rover. Branch desde `main` o `develop`, PR directo a ambas.
- **`release/v1.0-sprint4`**: preparación de entrega final (docs, PDF, tag).

## 2. Mapa completo de ramas creadas (35)

### Nivel 0 — Troncales (2)
| Rama | Base | Propósito |
|---|---|---|
| `main` | — | Producción estable |
| `develop` | `main` | Integración de los 4 sprints |

### Nivel 1 — Sprints (4)
| Rama | Sprint | Habilita (sprints.md) | Issues clave |
|---|---|---|---|
| `sprint/1-infra-firmware-base` | 1 | RNF-1.1/1.2, RF-2.1/3.1 | DEVOPS-01..05, MOV-01, LOW-01, EST-01, PM-03/04 |
| `sprint/2-domotica-acceso` | 2 | RF-2.2, HU-01 | MOV-02..04, DEVOPS-06..08, LOW-02, FW mega-access |
| `sprint/3-rover-telemetria` | 3 | RF-3.2, RF-1.2 | MOV-05..07, FW rover-uno/gateway, RF-3.3 fail-safe |
| `sprint/4-lowcode-estadistica-cierre` | 4 | RF-4.1/4.2, RNF-2.1/2.2 | LOW-03..05, EST-02..07, MOV-08..10, cierre E2E |

### Nivel 2 — Features por sprint (27)

#### Sprint 1 — Infraestructura & Firmware Base (6)
| Rama | Backlog ID | RF/HU | Contenido |
|---|---|---|---|
| `feature/devops-docker-mqtt` | DEVOPS-01,02,03 | RNF-1.1, RF-2.1 | `docker-compose.yml`, Mosquitto ACL, `init.sql`/`models.py:34` |
| `feature/devops-ci-arduino` | DEVOPS-04 | RNF-1.2 | `.github/workflows/ci.yml:90`, arduino-cli FQBN |
| `feature/firmware-gateway-rf` | DEVOPS-05 | RF-2.1, RF-3.1 | `firmware/gateway-esp32/*`, nRF24L01 SPI, `docs/testing-rf-sprint1.md` |
| `feature/app-setup-mvvm` | MOV-01 | RF-1.1 | `app/` Kotlin+Compose+MVVM base |
| `feature/stats-ema-prototipo` | EST-01 | RNF-2.1 | `stats/ema_filter.py`, `tests/test_ema_filter.py` |
| `feature/automation-tuya-validation` | LOW-01 ⚠️ | RF-4.2 | Validación `tuya-local` (riesgo crítico) |

#### Sprint 2 — Domótica Fija & Control de Acceso (7)
| Rama | Backlog ID | RF/HU | Contenido |
|---|---|---|---|
| `feature/firmware-mega-cerrojo` | — | RF-2.2, HU-01 | `firmware/mega-access/*` teclado 4x4 + MG90S + LED RGB verde |
| `feature/firmware-mega-laser` | — | RF-2.3, HU-02 | KY-008 láser, LED RGB rojo |
| `feature/app-pantallas-domotica` | MOV-02 | RF-1.1 | Compose: bombillo Tuya + LED local |
| `feature/app-mqtt-telemetria` | MOV-03 | RF-1.1 | Cliente MQTT/WebSocket |
| `feature/app-pin-cerrojo` | MOV-04 | HU-01 | PIN desde app |
| `feature/backend-endpoints` | DEVOPS-06,07,08 | RF-2.1 | `backend/app/routers/events.py:8`, `schemas.py`, tests |
| `feature/automation-mqtt-sub` | LOW-02 | RF-4.1 | Node-RED `automation/flows/intrusion_alert.json` |

#### Sprint 3 — Rover Tanque & Telemetría (6)
| Rama | Backlog ID | RF/HU | Contenido |
|---|---|---|---|
| `feature/firmware-rover-chasis` | — | RF-3.2 | `firmware/rover-uno/*` L298N + HC-SR04 + TCRT5000 |
| `feature/firmware-rover-failsafe` | — | RF-3.3, HU-04 | fail-stop 300-500ms sin RF |
| `feature/firmware-gateway-esp32` | — | RF-2.1 | Gateway UART↔RF↔MQTT |
| `feature/app-joystick-virtual` | MOV-05 | RF-1.2 | Joystick Compose X,Y |
| `feature/app-joystick-latency` | MOV-06 | RF-1.2 | Throttling <10ms RF / <50ms Wi-Fi |
| `feature/app-bluetooth-fallback` | MOV-07 | RF-1.3 | HC-06 SPP |

#### Sprint 4 — LowCode, Estadística y Cierre (8)
| Rama | Backlog ID | RF/HU | Contenido |
|---|---|---|---|
| `feature/automation-telegram-bot` | LOW-03 | RF-4.1, HU-02 | BotFather + Node-RED Telegram |
| `feature/automation-tuya-local` | LOW-04 | RF-4.2, HU-02 | `tuya-local` bombillo |
| `feature/automation-intrusion-alert` | LOW-05 | HU-02 | Flujo completo láser→Telegram+bombillo rojo |
| `feature/stats-ema-firmware` | EST-02,03 | RNF-2.1, HU-03 | EMA C++ α=0.2 HC-SR04 + KY-037 |
| `feature/stats-analitica-tstudent` | EST-04,05,06,07 | RNF-2.2 | psycopg2, t-Student, KPI >85% |
| `feature/app-dashboard-consolidado` | MOV-08,09 | RF-1.1 | Dashboard + reconexión MQTT |
| `feature/app-tests-viewmodel` | MOV-10 | RNF calidad | JUnit ViewModels |
| `feature/docs-cierre-e2e` | DEVOPS-09, PM-05/06 | — | Tests E2E, docs, retro |

### Nivel 3 — Soporte (2)
| Rama | Uso |
|---|---|
| `hotfix/critical` | Parches urgentes (láser, fail-safe rover) |
| `release/v1.0-sprint4` | Tag final de entrega UTP |

## 3. Flujo de trabajo

```bash
# 1. Empezar una tarea (ej. MOV-05 joystick)
git fetch origin
git checkout feature/app-joystick-virtual
git pull origin feature/app-joystick-virtual

# 2. Commitear referenciando HU/RF + backlog ID
git commit -m "MOV-05: implementa joystick virtual Compose (RF-1.2) #HU-01"

# 3. Push y PR hacia su sprint
git push -u origin feature/app-joystick-virtual
# PR: feature/app-joystick-virtual → sprint/3-rover-telemetria

# 4. Al cerrar sprint, PR del sprint hacia develop
# PR: sprint/3-rover-telemetria → develop

# 5. Al cerrar proyecto, PR develop → main + tag
git tag v1.0-sprint4 && git push origin v1.0-sprint4
```

**Reglas:**
- Cada commit/PR debe citar `ID backlog` + `HU-XX`/`RF-X.X` (ver `AGENTS.md` §3).
- Ningún push directo a `main` o `develop` — siempre vía PR con CI verde.
- `LOW-01` y `EST-01` deben validarse en Sprint 1 aunque su cierre formal sea Sprint 4 (ver riesgos en `backlog.md:99`).
- Firmware: todo push pasa `arduino-cli` (RNF-1.2). Backend: `pytest` + `ruff` + `mypy`.

## 4. Dependencias entre ramas (orden de merge)

```
DEVOPS-01..05 (sprint/1) ─┬─► sprint/2 ─┬─► sprint/4 ─► develop ─► main
                          │             │
EST-01 (prototipo) ───────┘             ├─► LOW-03..05 (Telegram+Tuya)
                                        │
MOV-05/06 (joystick) ───────────────────┘
```

Si Sprint 1 se atrasa, todo se atrasa (ver `backlog.md:47` riesgo DevOps).

## 5. Comandos útiles

```bash
git branch -a                          # ver las 35 ramas
git checkout sprint/2-domotica-acceso  # cambiar de sprint
git log --oneline --graph --all -20    # grafo
git branch -d feature/app-setup-mvvm   # borrar feature ya mergeada (local)
git push origin --delete feature/app-setup-mvvm  # borrar remota
```

## 6. Estado actual (2026-08-26)

- Sprint activo: **Sprint 1 — En curso** (`sprint/1-infra-firmware-base`)
- Ramas Sprint 1 ya tienen base validada (DEVOPS-01..05 cerrados en `main:566932c`)
- Cambios pendientes en working dir (`ci.yml` docker-build mejorado + `sprints.md:61`): hacer commit en `develop` o `sprint/1-infra-firmware-base` antes de arrancar Sprint 2.
- Próximo paso: `git checkout sprint/2-domotica-acceso` y comenzar `feature/firmware-mega-cerrojo` + `feature/app-pantallas-domotica`.
