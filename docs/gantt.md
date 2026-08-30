# Diagrama Gantt — AetherNet IoT (PM-04)

Resuelve deuda Sprint 1→2 PM-04. Fuente: `docs/sprints.md:9-48` + `docs/backlog.md:9-79` + `docs/branching-strategy.md:122-131` + `docs/deuda-sprint1-sprint2.md`.

> Sprint 1 fechado 2026-08-26 (merge `9eac686` a `develop`), Sprint 2 activo. Deuda 1→2 saldada 2026-08-29 en `feature/firmware-mega-cerrojo`. Fuera de alcance (W) omitido.

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
    MOV-01 Setup MVVM                    :done,  s1d, 2026-08-17, 7d
    LOW-01 tuya-local validación         :active,s1e, 2026-08-18, 8d
    EST-01 EMA prototipo                 :done,  s1f, 2026-08-13, 6d
    PM-03/04 sprints/gantt inicial      :done,  s1g, 2026-08-12, 3d

    section Deuda 1→2 (esta rama)
    PM-08 auditoría secretos            :done,  d1, 2026-08-29, 1d
    DEVOPS-11 rotar secrets             :done,  d2, 2026-08-29, 1d
    DEVOPS-10 pipeline verde            :done,  d3, 2026-08-29, 1d
    PM-03 R-12/R-13                     :done,  d4, 2026-08-29, 1d
    MOV-01 cierre formal                :done,  d5, 2026-08-29, 1d
    MOV-12 build Android                :done,  d6, 2026-08-29, 1d
    PM-02 tablero Scrum                 :done,  d7, 2026-08-29, 1d
    PM-04 gantt (este archivo)          :done,  d8, 2026-08-29, 1d

    section Sprint 2 — Domótica (RF-2.2 HU-01)
    FW-MEGA cerrojo 4x4+MG90S            :active, s2a, 2026-08-26, 10d
    FW-MEGA láser KY-008                :       s2b, after s2a, 7d
    MOV-02 pantallas domótica            :       s2c, 2026-08-29, 10d
    MOV-03 MQTT app                      :       s2d, 2026-08-29, 10d
    MOV-04 PIN cerrojo                   :       s2e, after s2d, 7d
    DEVOPS-06/07 endpoints+tests         :       s2f, 2026-08-29, 7d
    LOW-02 Node-RED sub                 :       s2g, 2026-08-29, 7d

    section Sprint 3 — Rover (RF-3.2, RF-1.2)
    FW-ROVER chasis L298N/HC-SR04        :       s3a, 2026-09-08, 10d
    FW-ROVER failsafe 300-500ms          :       s3b, after s3a, 5d
    MOV-05 joystick Compose               :       s3c, 2026-09-08, 7d
    MOV-06 latencia <10ms                :       s3d, after s3c, 7d
    MOV-07 BT SPP fallback               :       s3e, 2026-09-15, 7d

    section Sprint 4 — LowCode/Stats/Cierre
    LOW-03 Telegram bot                  :       s4a, 2026-09-22, 5d
    LOW-04 tuya-local                    :       s4b, after s4a, 5d
    LOW-05 intrusión Telegram+rojo       :       s4c, after s4b, 5d
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
- **Riesgo LOW-01** (`R-01`) validado en Sprint 1, no esperar a Sprint 4 (`backlog.md:63`).
- `MOV-06` latencia joystick depende de `DEVOPS-05` RF ya probado.

## Hitos

- 2026-08-26 Sprint 1 merge `9eac686` → `develop` + `sprint/2-domotica-acceso` activo.
- **2026-08-29 Deuda 1→2 saldada** (8/8 ACT en `docs/deuda-sprint1-sprint2.md`).
- 2026-09-08 Sprint 3 inicia (requiere Sprint 2 verde).
- 2026-09-22 Sprint 4 cierre E2E + sustentación.

## Export

Mermaid renderizable en GitHub/docs o `presentaciones/gantt.pptx` (usar `docs/gantt.md` como fuente). No requiere dependencias propietarias (RNF-3.1 FOSS).
