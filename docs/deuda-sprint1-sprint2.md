# Deuda Técnica Sprint 1 → Sprint 2 — Checkpoint

> **Rama activa:** `feature/firmware-mega-cerrojo` (Sprint 2 Domótica, `docs/sprints.md:52`).
> **Uso:** cada entrada del agente lee este archivo + `docs/sprints.md` + `docs/backlog.md` + `docs/auditoria-secretos-sprint1.md` para saber qué actividad toca. Al terminar una actividad, el humano marca `- [x]` en la casilla correspondiente. No avanzar a la siguiente sin esa marca.
> **App Android:** solo abierta en Android Studio, sin desarrollo — `MOV-01` es setup base, `MOV-12` queda fusionado con `MOV-01` (no es "fix build roto" sino "primer build verde"). **Sincerado 2026-08-31:** dueño confirma 0 líneas Kotlin propias — `MainActivity.kt:22`/`DashboardViewModel.kt:32` son plantilla wizard, `MOV-01` pasa a `plantilla-only` (ver `docs/cierre-mov01.md:4`).

| # | Ítem backlog | Actividad | Estado | Dueño | Evidencia al cerrar |
|---|---|---|---|---|---|
| ACT-01 | PM-08 | Auditoría historial commits (secretos) | - [x] | Gestión/DevOps | `docs/auditoria-secretos-sprint1.md` + `gitleaks/truffleHog` 0 leaks nuevos |
| ACT-02 | DEVOPS-11 | Rotar/asegurar credenciales (`secrets.h` + `.env.example`) | - [x] | DevOps | `firmware/gateway-esp32/secrets.h.example`, `backend/.env.example`, `gateway-esp32.ino` sin hardcoded |
| ACT-03 | DEVOPS-10 | Pipeline CI en verde | - [x] | DevOps | `gh run list` / `ci.yml` verde en `develop` |
| ACT-04 | PM-03 | Matriz de riesgos actualizada (R-12/R-13) | - [x] | Gestión | `docs/risk-register.md` con R-12/R-13 |
| ACT-05 | MOV-01 | Cierre formal setup Android MVVM base | - [x] ✅ Done `f03190b` 2026-09-01 | Móviles | `feature/app-setup-mvvm` `f03190b` — `AetherControlApp`+`ServiceLocator` DI manual, `Retrofit`+DTOs, `PreferencesManager` `updateBaseUrl`, `DashboardViewModel` `StateFlow`+`NavGraph`, `assembleDebug`/`testDebugUnitTest` verdes, `curl 192.168.1.14:8000/health` ok SM-X620 — cierra plantilla-only, ver `docs/cierre-mov01.md:4` |
| ACT-06 | MOV-12 | Build Android pendiente (fusionado con ACT-05) | - [x] ✅ Done `f03190b` | Móviles | `ci.yml` `android-build` condicional + `assembleDebug` verde `f03190b` — infra + código |
| ACT-07 | PM-02 | Tablero Scrum | - [x] | Gestión | `https://github.com/users/Craos6518/projects/14` (PM-02 completado 2026-08-31) |
| ACT-08 | PM-04 | Diagrama Gantt | - [x] | Gestión | `docs/gantt.md` (Mermaid) exportado — actualizar LOW-01 a bloqueado 2026-08-31 |

## Dependencias

```
ACT-01 (PM-08) ──► ACT-02 (DEVOPS-11) ──┐
                                      ├──► ACT-03 (DEVOPS-10) ──► ACT-05/06 (MOV-01/12)
ACT-04 (PM-03) ────────────────────────┘     ACT-07/08 (PM-02/04) en paralelo
```

## Reglas para el agente

1. En cada entrada, leer **este archivo primero** y ejecutar solo la primera actividad con `- [ ]` cuyas dependencias ya estén `[x]`.
2. No editar actividades futuras. Solo la actividad activa.
3. Al terminar, pedir al humano que marque `[x]` y haga commit con mensaje `PM-08: ...` / `DEVOPS-11: ...` según corresponda.

## Log de avances

- 2026-08-29 — checkpoint creado en rama `feature/firmware-mega-cerrojo`. Pendiente ACT-01.
- 2026-08-29 — ACT-01 PM-08 completada: `docs/auditoria-secretos-sprint1.md` generado, H-01 (`gateway-esp32.ino:24-25` FELIPE./2516f751 en `ed557b7/ddebd48`) y H-02 (`backend/.env.example` faltante) documentados. Siguiente: ACT-02 DEVOPS-11.
- 2026-08-29 — ACT-02 DEVOPS-11 completada: `gateway-esp32.ino:20-51` sin hardcoded FELIPE/2516f751, `__has_include("secrets.h")` + defaults `AetherNet-LAN/changeme/192.168.1.100`; `firmware/gateway-esp32/secrets.h.example` trackeado, `secrets.h` gitignored y compilable (`arduino-cli compile esp32:esp32:esp32 80% OK`); `backend/.env.example` creado (DEVOPS-08); `.gitignore:219` con `firmware/**/secrets.h`. Pendiente rotación física de password router si FELIPE es red productiva.
- 2026-08-29 — ACT-03 DEVOPS-10 completada: `ci.yml:14` ARDUINO_CLI 1.0.4→1.5.1, `ci.yml:99-107` fallback `secrets.h` en CI, `ci.yml:241` android-build condicional `hashFiles(...)`, `.gitignore:68-69` removido `*.gradle.kts` (causa MOV-12); verificado `ruff OK`, `pytest 6 passed`, `mypy Success` (backend/pyproject.toml:13), `arduino-cli compile mega 7%/gateway 79%/rover 21%`.
- 2026-08-29 — ACT-04 PM-03 completada: `docs/risk-register.md:32` R-12 (WiFi leak) + R-13 (IP hardcodeada) añadidos, resumen actualizado a 5 riesgos Alta.
- 2026-08-29 — ACT-05/06 MOV-01/MOV-12 completadas: `app/build.gradle.kts:4` KSP 1.9.22-1.0.17, `app/gradlew` 9.5.0 + `gradle-wrapper.jar` + `.gitignore:90-93` whitelist, `app/gradlew tasks` OK (ksp warning corregido), `docs/cierre-mov01.md` formal; MOV-12 fusionado (CI `ci.yml:241` condicional, `assembleDebug` requiere SDK en CI).
- 2026-08-29 — ACT-07 PM-02 completada: `docs/tablero-scrum.md` 6 columnas Kanban + 8 tarjetas deuda/Sprint2, guía GitHub Projects.
- 2026-08-29 — ACT-08 PM-04 completada: `docs/gantt.md` Mermaid con Sprints 1-4 + deuda 1→2 (8 ACT done) y ruta crítica.
- 2026-08-31 — Sinceramiento MOV-01: dueño confirma 0 líneas Kotlin propias — `docs/cierre-mov01.md:4` pasa a plantilla-only; PM-02 link real `https://github.com/users/Craos6518/projects/14`; rotación FELIPE./2516f751 cerrada (red prod rotada + AP lab FELIPE./2516f751 recreado); LOW-01 bloqueado — 2da opción skill Alexa/Google Home en evaluación (R-01).

## Notas App Android (aclaración 2026-08-29 — sincerada 2026-08-31)

> "No he comenzado con el desarrollo de la app, solo la abrí con Android Studio" — `MOV-01`/`MOV-12` no son fix de build roto sino setup inicial. `app/build.gradle.kts:1` y `app/src/main/java/com/aethernet/aethercontrol/MainActivity.kt:22` están en estado plantilla. ACT-05/06 validarán primer `./gradlew tasks` / `assembleDebug` local sin exigir CI Android verde completo hasta que exista código Compose real.
> **2026-08-31:** confirmación formal — `MainActivity.kt:52` `DashboardScreen` y `DashboardViewModel.kt:41` son scaffold wizard con `TODO MQTT` sin implementación. Infra (`gradlew 9.5.0`, KSP, `ci.yml:241`) verde, pero `MOV-01` no es Done hasta escribir lógica MVVM real en Sprint 2.
