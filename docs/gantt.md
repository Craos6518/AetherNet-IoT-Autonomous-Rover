# Diagrama Gantt — AetherNet IoT (PM-04)

Resuelve deuda Sprint 1→2 PM-04. Fuente: `docs/sprints.md:9-48` + `docs/backlog.md:9-79` + `docs/branching-strategy.md:122-131` + `docs/deuda-sprint1-sprint2.md`.

> Sprint 1 fechado 2026-08-26 (merge `9eac686` a `develop`), Sprint 2 ✅ CERRADO 2026-09-10, **Sprint 3 ✅ CERRADO 2026-09-11**. Deuda 1→2 saldada 2026-08-29 en `feature/firmware-mega-cerrojo`, sincerada 2026-08-31 (MOV-01 plantilla-only, rotación cerrada, Project 14, LOW-01 pivot skill). **Actualizado 2026-09-11** `Sprint 3 Rover`: `MOV-05/06/07` Done + `FW-ROVER` chasis/evasion/anti-caída Done, `FW-MEGA cerrojo+laser` Done, `MOV-02/03/04` + `DEVOPS-06/07` Done, `LOW-02` deuda 2026-09-09, `LOW-04` cancelado, `EST-01` + datasets 36.5k adelantados. Sprint 4 activo. Fuera de alcance (W) omitido.

```mermaid
gantt
    title AetherNet IoT — Sprints 1-4 + Deuda 1→2 (PM-04)
    dateFormat  YYYY-MM-DD
    axisFormat  %d %b
    excludes    weekends

    section Sprint 1 — Infra (RNF-1.1/1.2, RF-2.1)
    DEVOPS-01..03 Docker/PG/Mosquitto   :done,  s1a, 2026-08-12, 5d
    DEVOPS-04 CI arduino-cli            :done,  s1b, 2026-08-14, 4d
    DEVOPS-05 RF nRF24L01                :done,  s1c, 2026-08-16, 4d
    MOV-01 Setup MVVM (plantilla-only)   :done,  s1d, 2026-08-17, 7d
    LOW-01 tuya-local validación         :crit, active, s1e, 2026-08-18, 8d
    LOW-01 pivot skill Alexa/GHome      :crit, s1e2, 2026-08-31, 7d
    EST-01 EMA prototipo                 :done,  s1f, 2026-08-13, 6d
    PM-03/04 sprints/gantt inicial      :done,  s1g, 2026-08-12, 3d

    section Deuda 1→2 (esta rama)
    PM-08 auditoría secretos            :done,  d1, 2026-08-29, 1d
    DEVOPS-11 rotar secrets             :done,  d2, 2026-08-29, 1d
    DEVOPS-10 pipeline verde            :done,  d3, 2026-08-29, 1d
    PM-03 R-12/R-13                     :done,  d4, 2026-08-29, 1d
    MOV-01 cierre formal                :done,  d5, 2026-08-29, 1d
    MOV-12 build Android                :done,  d6, 2026-08-29, 1d
    PM-02 tablero Scrum (Project 14)    :done,  d7, 2026-08-29, 1d
    PM-04 gantt (este archivo)          :done,  d8, 2026-08-29, 1d
    Sinceramiento 2026-08-31             :done,  d9, 2026-08-31, 1d

    section Sprint 2 — Domótica (RF-2.2 HU-01)
    FW-MEGA cerrojo 4x4+MG90S            :done, s2a, 2026-08-26, 10d
    FW-MEGA láser KY-008 v2             :done, s2b, 2026-09-01, 7d
    MOV-02 pantallas domótica            :done, s2c, 2026-08-29, 10d
    MOV-03 MQTT app                      :done, s2d, 2026-08-29, 10d
    MOV-04 PIN cerrojo                   :done, s2e, 2026-09-01, 7d
    DEVOPS-06/07 endpoints+tests         :done, s2f, 2026-09-01, 7d
    LOW-02 Node-RED sub DEUDA 09-09     :crit, done, s2g, 2026-09-09, 1d
    EST datasets 36.5k externo           :done, s2h, 2026-09-07, 3d
    Revision docs Sprint 2               :active, s2i, 2026-09-10, 2d

    section Sprint 3 — Rover (RF-3.2, RF-1.2) ✅ CERRADO 2026-09-11
    FW-ROVER chasis L298N/HC-SR04        :done,  s3a, 2026-09-08, 10d
    FW-ROVER failsafe 300-500ms          :done,  s3b, after s3a, 5d
    MOV-05 joystick Compose               :done,  s3c, 2026-09-08, 7d
    MOV-06 latencia <10ms                :done,  s3d, after s3c, 7d
    MOV-07 BT SPP fallback               :done,  s3e, 2026-09-15, 7d

    section Sprint 4 — LowCode/Stats/Cierre
    LOW-03 Telegram bot directo          :       s4a, 2026-09-22, 5d
    LOW-04 tuya CANCELADO ADR-001        :crit, done, s4b, 2026-09-01, 1d
    LOW-05 intrusión Telegram+rojo       :       s4c, after s4a, 5d
    EST-02/03 EMA firmware               :       s4d, 2026-09-22, 10d
    EST-04..07 t-Student KPI>85%         :       s4e, after s4d, 7d
    MOV-08..10 dashboard+tests           :       s4f, 2026-09-22, 10d
```

## Dependencias críticas (flechas lógicas)

```
DEVOPS-01..05 (Sprint 1) ─┬─► Sprint 2 (FW-MEGA/MOV-02..04/LOW-02) ─┬─► Sprint 4 (LOW-03..05, EST)
                          │                                        │
EST-01 ───────────────────┘                                        ├─► Sprint 3 (MOV-05/06 joystick) ─┘
PM-08 ─► DEVOPS-11 ─► DEVOPS-10 ─► MOV-01/12 (deuda, ya saldada 2026-08-29)
```

- **Ruta crítica:** `DEVOPS-11 → DEVOPS-10 → MOV-01` (desbloqueó CI y KSP 1.9.22-1.0.17, `.gitignore`).
- **Riesgo LOW-01** (`R-01`) **CANCELADO 2026-09-01** ADR-001 (no pivot skill — HU-02 solo LED+Telegram). **LOW-02** **DEUDA 2026-09-09** (no Node-RED esta iteración).
- `MOV-01` sincerado plantilla-only 2026-08-31 — lógica real queda para `feature/app-setup-mvvm` Sprint 2.
- `MOV-06` latencia joystick depende de `DEVOPS-05` RF ya probado.

## Hitos

- 2026-08-26 Sprint 1 merge `9eac686` → `develop` + `sprint/2-domotica-acceso` activo.
- **2026-08-29 Deuda 1→2 saldada** (8/8 ACT en `docs/deuda-sprint1-sprint2.md`).
- **2026-08-31 Sinceramiento:** MOV-01 plantilla-only (`docs/cierre-mov01.md:4`), rotación prod + AP lab `FELIPE./2516f751` (`R-12` Resuelto), Project `https://github.com/users/Craos6518/projects/14`, LOW-01 pivot skill Alexa/GHome (`R-01` alta bloqueada), `a051dd4` EMA bench en `develop`.
- 2026-09-07 FW-MEGA laser v2 `c7ce065` + DEVOPS-06/07 21 tests verdes.
- 2026-09-09 datasets 36.5k + admin doc `ec5d70b`.
- 2026-09-10 revisión `docs/revision-sprint2-completa` (esta rama) — sincroniza 9 docs.
- **2026-09-11 Sprint 3 ✅ CERRADO** — `MOV-05/06/07` + `FW-ROVER` (L298N/HC-SR04/TCRT5000, EMA α=0.2, failsafe 500ms, RF nRF24L01) — `rover-uno.ino:42/86/247/375` + `gateway-esp32.ino:241`.
- 2026-09-22 Sprint 4 cierre E2E + sustentación (activo).

## Export

Mermaid renderizable en GitHub/docs o `presentaciones/gantt.pptx` (usar `docs/gantt.md` como fuente). No requiere dependencias propietarias (RNF-3.1 FOSS).
