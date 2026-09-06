# Roadmap por Materia — DevOps (incluye Firmware Base)

**Alcance de este roadmap:** todos los temas necesarios para COMPLETAR el área DevOps del proyecto: infraestructura contenerizada, CI/CD, broker MQTT, base de datos, API backend y los 3 firmwares. Convención heredada de `docs/backlog.md`: el firmware base vive en esta área.

**Estado al Aug 2026:** `docker-compose.yml` ✅ · `ci.yml` con 6 jobs ✅ · 3 firmwares compilables ✅ · **`backend/app/main.py` NO existe** (el job `docker-build` del CI falla al hacer curl a `/health`) · credenciales Wi-Fi/MQTT hardcodeadas en el gateway ⚠️ · job Android desactivado pese a que Gradle ya existe ⚠️

Backlog operativo detallado: [`backlog-devops.md`](backlog-devops.md)

---

## Bloque 1 — Contenedores y orquestación (Sprint 1, cerrar YA)

| Tema | Profundidad requerida | Para qué requisito | Notas específicas del proyecto |
|---|---|---|---|
| Ciclo de vida Docker: build/run/volúmenes/redes | Operativo | RNF-1.1 | Ya aplicado; solo mantener |
| Healthchecks + `depends_on.condition` | Operativo | RNF-1.1 | Ya implementado para Postgres; replicar criterio si se agregan servicios |
| Variables de entorno + `.env` sin secretos | Operativo | RNF-1.1, DEVOPS-08 | `env.example` existe; verificar que NADA real esté versionado |

## Bloque 2 — Broker Mosquitto MQTT (Sprint 1-2)

| Tema | Profundidad requerida | Para qué requisito | Notas específicas |
|---|---|---|---|
| Configuración `mosquitto.conf`: listener 1883 + WebSocket 9001 | Saber editar y reiniciar | RF-2.1 | El puerto 9001 ya está mapeado en Compose; falta confirmar que la config lo habilite (`protocol websockets`) — la App Android usará WS o TCP según librería |
| Autenticación (`password_file`) y ACLs (`acl_file`) | Saber crear ambos archivos y hashear contraseñas con `mosquitto_passwd` | DEVOPS-02, RF-4.2 | Mínimo: gateway puede publicar en `aethernet/#`; Node-RED puede leer `aethernet/seguridad/#`; nadie anónimo publica |
| Topics y QoS: diseño de árbol de topics | Diseñar y documentar tabla topic→productor→consumidor→QoS | RF-2.1, RF-4.x | Los topics ya existen definidos en `gateway.ino` (rover/command, access/event, seguridad/intrusion): documentarlos es la tarea, no inventarlos nuevos |
| Retained messages y Last Will (LWT) | Conceptual+aplicado en gateway | RF-3.3 | LWT del gateway para que la App sepa si el gateway murió |

## Bloque 3 — CI/CD GitHub Actions avanzado (Sprint 1-2)

| Tema | Profundidad requerida | Para qué requisito | Notas específicas |
|---|---|---|---|
| Sintaxis YAML de workflows: jobs, matrix, needs, if | Operativo | RNF-1.2 | Ya aplicado; mantener |
| Caché de pip y de cores arduino-cli | Aplicar | RNF-1.2 | Cada run reinstala cores ESP32/AVR (~minutos perdidos); cachear `$HOME/.arduino15` |
| Secrets de GitHub vs `.env` local | Diferenciar y aplicar | Seguridad | Trivy ya corre; no subir credenciales reales jamás |
| Habilitación condicional de jobs (`if:`) | Aplicar | MOV-10 | Job `android-build` está `if: false` pero Gradle ya existe → habilitarlo |
| Badges y estados requeridos para merge | Aplicar | RNF-1.2 | Branch protection: CI verde obligatorio para mergear a main |

## Bloque 4 — Backend FastAPI (Sprint 2) ← *brecha crítica actual*

| Tema | Profundidad requerida | Para qué requisito | Notas específicas |
|---|---|---|---|
| Estructura de app FastAPI: `main.py`, routers, lifespan | Implementar completo | DEVOPS-06 | **No existe main.py**: crear app factory, incluir router health y eventos |
| SQLAlchemy async (asyncpg) + sesión por request | Implementar | RNF-2.2 | `database.py` y `models.py` ya existen; falta exponerlos |
| Pydantic v2 schemas (request/response) | Implementar | DEVOPS-06 | Separar modelos ORM de esquemas API |
| PyTest con TestClient + BD de prueba | Implementar | DEVOPS-07 | Fixtures con SQLite en memoria o Postgres efímero; correrá en job `backend-test` que YA existe en el CI |
| Cliente MQTT desde Python (paho-mqtt o aiomqtt) | Implementar puente | RF-2.1 | El backend debe suscribirse a `aethernet/access/event`, `aethernet/seguridad/intrusion`, `aethernet/rover/telemetry` y persistir en las tablas que ya existen (`AccessEvent`, `SensorEvent`, `SecurityEvent`) |
| Migraciones (Alembic) o init.sql como única fuente | Decidir UNA estrategia | RNF-2.2 | Hoy existe `backend/init.sql` montado en el entrypoint de Postgres; decidir si Alembic reemplaza o complementa — no dejar ambas fuentes divergentes |

## Bloque 5 — Firmware embebido y su integración (Sprint 1-3)

| Tema | Profundidad requerida | Para qué requisito | Notas específicas |
|---|---|---|---|
| `arduino-cli`: compile/upload/lib/core | Operativo | RNF-1.2 | Ya en CI; falta cache y comando de upload documentado para flashing local |
| Protocolo UART ESP32↔MEGA: framing + checksum | Diseñar y documentar | RF-2.1 | Ambos sketches usan Serial2 @115200 pero **el formato de trama no está documentado**: definir (ej. JSON por línea vs binario con header/checksum), misma decisión en ambos lados |
| Structs empaquetados (`#pragma pack`) compartidos entre sketches | Aplicar | RF-3.1 | Patrón ya usado Rover↔Gateway; replicar consistencia Gateway↔MEGA |
| Gestión de credenciales en firmware (sin hardcodear) | Refactorizar | Seguridad, FOSS | `gateway.ino` tiene `WIFI_PASSWORD "changeme"` y broker IP fija → mover a archivo `config.h` ignorado por git + plantilla `config.h.example` |
| Resolución de conflictos de pines | Verificar | RF-2.2 | `access_control.ino` verificado — LED RGB 44-46 libre, sin colisión (conflicto histórico resuelto) |
| EEPROM para PIN y config persistente | Implementar | HU-01 | `VALID_PIN "1234"` hardcodeado con TODO explícito → mover a EEPROM con comando de cambio de PIN |
| Depuración serie y medición de latencia | Aplicar | KPI PRD (<10 ms RF, <50 ms MQTT) | Instrumentar timestamps en paquetes para alimentar EST-05 |

## Bloque 6 — Redes LAN (transversal, Sprint 1)

| Tema | Profundidad requerida | Para qué requisito |
|---|---|---|
| Subredes, DHCP reservations (IP fija al ESP32/servidor) | Aplicar en el router | Restricción §6 PRD: todo en la misma subred (bombillo cancelado) |
| Diagnóstico: ping, nmap, tcpdump básico | Saber usar | Debugging de conectividad MQTT/RF |
| Puertos: 1883 (MQTT), 9001 (WS), 5432 (PG), 8000 (API) | Saber quién expone qué y a quién | Seguridad LAN |

## Bloque 7 — Monitoreo y logs (Sprint 4, mínimo viable)

| Tema | Profundidad requerida | Para qué requisito |
|---|---|---|
| Logs estructurados del backend (uvicorn + print/logging JSON) | Implementar básico | T6 del PDF parcialmente cubierto hoy vía volúmenes Mosquitto |
| Healthchecks de negocio (no solo `/health` de proceso): broker vivo, DB accesible | Extender endpoint | Control operacional demostrable en la sustentación |
| (Opcional post-proyecto) Prometheus/Grafana | Solo mencionar en doc final | Declarado fuera de alcance actual |

---

## Orden crítico de ejecución

1. **DEVOPS bloque 4 primero dentro de lo nuevo**: sin `main.py` el CI está roto silenciosamente (job docker-build fallará) y ninguna otra área puede probar integración.
2. Credenciales fuera de git (bloque 5) antes de que el repo crezca más.
3. Cache CI (bloque 3) tan pronto el pipeline empiece a tardarse molesto.
