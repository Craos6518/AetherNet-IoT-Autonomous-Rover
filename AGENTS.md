# AGENTS.md — AetherNet IoT & Autonomous Rover

Este archivo es el punto de entrada para cualquier agente de código (Claude Code, OpenCode, Cursor, etc.) que trabaje en este repositorio. No reemplaza la documentación del proyecto — la indexa.

## 1. Qué es este proyecto

Plataforma distribuida de domótica modular, telemetría estadística y robótica móvil. Proyecto Integrador del 5º semestre de Ingeniería de Sistemas / Desarrollo de Software (UTP), 100% FOSS: sin dependencia de nubes propietarias (AWS, GCP, Azure, Tuya Cloud).

**Antes de tocar código, lee en este orden:**
1. `docs/prd.md` — visión, alcance (in/out), KPIs, restricciones técnicas.
2. `docs/requirements.md` — requisitos funcionales/no funcionales y las Historias de Usuario BDD (HU-01, HU-02, HU-03).
3. `docs/hardware-inventory.md` — matriz completa de hardware por subsistema (LED RGB local como único indicador visual; bombillo Tuya cancelado ADR-001).
4. `docs/sprints.md` — planeación Scrum y estado actual del proyecto; consúltalo antes de proponer trabajo, para no adelantar tareas de un sprint cuya base aún no existe.
5. `docs/roadmap.md` — conocimientos necesarios por materia para desplegar cada componente; útil si el agente necesita explicar o justificar una elección técnica a un colaborador nuevo.
6. `docs/backlog.md` — backlog operativo por materia (MoSCoW + sprint asignado). Es la fuente de verdad de "qué falta hacer" — un agente debe consultarlo antes de proponer nuevo trabajo, y actualizar el estado de un ítem cuando lo complete.

Si una tarea contradice algo en estos seis archivos, el agente debe señalarlo antes de implementar, no asumir cuál documento tiene prioridad.

## 2. Mapa de stack por componente

| Componente | Stack | Ubicación esperada |
|---|---|---|
| App Android "AetherControl" | Kotlin, Jetpack Compose, MVVM, Coroutines, StateFlow, Retrofit, MQTT, Bluetooth API | `app/` |
| Backend | FastAPI, PostgreSQL, Mosquitto MQTT, Docker Compose | `backend/` |
| Automatización LowCode | Node-RED, Telegram Bot API (tuya-local cancelado 2026-09-01) | `automation/` |
| Firmware Gateway | C++ (ESP32), UART, nRF24L01, arduino-cli | `firmware/gateway-esp32/` |
| Firmware Acceso/Potencia | C++ (Arduino MEGA), teclado 4x4, servo MG90S, LED RGB local | `firmware/mega-access/` |
| Firmware Rover | C++ (Arduino UNO), L298N, HC-SR04, TCRT5000 | `firmware/rover-uno/` |
| Análisis estadístico | Python (Pandas/SciPy) o R — EMA, prueba t-Student | `stats/` |
| CI/CD | GitHub Actions + `arduino-cli` | `.github/workflows/` |

> Ajusta las rutas de arriba si la estructura real del repo difiere; este mapa es orientativo hasta que exista un `README.md` de estructura de carpetas.

## 3. Convenciones de trabajo

- Cada cambio de código debe referenciar la Historia de Usuario (HU-XX) o el Requisito Funcional (RF-X.X) que implementa.
- Los criterios de aceptación BDD (`Dado / Cuando / Entonces`) en `requirements.md` son la fuente de verdad para tests, no una guía aproximada.
- Firmware: todo push debe pasar el pipeline de `arduino-cli` en GitHub Actions antes de mergear.
- Backend: tests con PyTest; frontend Android: tests con JUnit/Jest según corresponda.
- Ningún componente puede introducir una dependencia de nube de pago o SDK propietario (ver RNF-3.1, cumplimiento FOSS).

## 4. Cuándo pedir confirmación al humano

- Antes de cambiar el protocolo de comunicación entre subsistemas (RF, UART, MQTT).
- Antes de modificar umbrales de seguridad (láser, PIN, filtro EMA α).
- Antes de agregar cualquier dependencia que no sea FOSS.

## 5. Skills / agentes especializados

Si el equipo agrega skills de revisión de código, testing o codificación (Claude Code Skills, plugins de OpenCode, etc.), documentarlas aquí con su propósito y cuándo se activan, para que no queden implícitas en la config local de cada colaborador.

_(Sección pendiente de completar cuando se defina qué skills se usarán.)_
